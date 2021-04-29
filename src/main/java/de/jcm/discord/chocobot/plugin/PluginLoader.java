package de.jcm.discord.chocobot.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jcm.discord.chocobot.ChocoBot;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginLoader
{
	private final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

	private final File directory;
	private final CopyOnWriteArrayList<Plugin> plugins = new CopyOnWriteArrayList<>();

	public PluginLoader(File directory)
	{
		this.directory = directory;
	}

	public void initPlugins()
	{
		ObjectMapper mapper = new ObjectMapper();

		for(File file : Objects.requireNonNull(directory.listFiles(f ->
			f.getName().endsWith(".jar") && f.isFile() && f.canRead())))
		{
			try
			{
				ZipFile zip = new ZipFile(file);
				ZipEntry entry = zip.getEntry("plugin.json");
				InputStream in = zip.getInputStream(entry);

				PluginInfo info = mapper.readValue(in, PluginInfo.class);

				in.close();

				if(info.isValid())
				{
					URLClassLoader loader = new URLClassLoader(info.name+"_loader",
					                                           new URL[] {file.toURI().toURL()},
					                                           this.getClass().getClassLoader());
					@SuppressWarnings("unchecked")
					Class<? extends Plugin> clazz = (Class<? extends Plugin>)
							Class.forName(info.mainClass, true, loader);
					Plugin instance = clazz.getConstructor().newInstance();
					plugins.add(instance);

					logger.info("Initialized plugin {} from class {} in {}",
					            instance.getName(), info.mainClass, file.getAbsolutePath());

					ZipEntry entry1 = zip.getEntry("languages.txt");
					if(entry1 != null)
					{
						int[] stats = {0, 0};
						try(InputStream liin = zip.getInputStream(entry1))
						{
							try(BufferedReader reader = new BufferedReader(new InputStreamReader(liin)))
							{
								reader.lines().peek(s->stats[0]++).forEach(l -> {
									Map<String, String> map = ChocoBot.languages.computeIfAbsent(l, q->new HashMap<>());
									try(InputStream lin = zip.getInputStream(zip.getEntry("lang/" + l + ".lang"));
									    BufferedReader lre = new BufferedReader(new InputStreamReader(lin)))
									{
										lre.lines()
										   .filter(Predicate.not(String::isBlank))
										   .map(line->new ImmutablePair<>(line.substring(0, line.indexOf('=')), line.substring(line.indexOf('=')+1)))
										   .peek(s->stats[1]++)
										   .forEach(e->map.put(e.getKey(), e.getValue()));
									}
									catch(IOException | NullPointerException e)
									{
										e.printStackTrace();
									}
								});
							}
							catch(IOException e)
							{
								e.printStackTrace();
							}
						}
						logger.info("Loaded {} translation(s) in {} different language(s) for plugin {}.", stats[1], stats[0], instance.getName());
					}
					else
					{
						logger.info("No language file found for plugin {}. Not loading any additional translations.", instance.getName());
					}
				}
				else
				{
					logger.error("Invalid plugin info in file {}: {}", file.getAbsolutePath(), info);
				}
				zip.close();
			}
			catch(IOException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void loadPlugins()
	{
		for(Plugin plugin : plugins)
		{
			plugin.onLoaded();
			logger.info("Loaded plugin {}", plugin.getName());
		}
	}

	public void unloadPlugins()
	{
		for(Plugin plugin : plugins)
		{
			plugin.onUnloaded();
			logger.info("Unloaded plugin {}", plugin.getName());
		}
	}

	public void destroyPlugins()
	{
		plugins.clear();
	}
}
