package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.plugin.Plugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;

public abstract class Command
{
	private static final Logger logger_ = LoggerFactory.getLogger(Command.class);
	final Logger logger = LoggerFactory.getLogger(this.getClass());

	static final HashMap<String, Command> commands = new HashMap<>();
	static final HashMap<String, Pair<Command, Plugin>> pluginCommands = new HashMap<>();

	protected Command()
	{
	}

	public abstract boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args);

	@NotNull
	protected abstract String getKeyword();

	@Nullable
	protected String getHelpText(GuildSettings settings)
	{
		return settings.translate("command."+getKeyword()+".help");
	}

	public boolean multipleArguments()
	{
		return true;
	}

	public boolean usableEverywhere()
	{
		return false;
	}

	public static void registerCommand(Command command)
	{
		String keyword = command.getKeyword().toLowerCase();
		if (commands.containsKey(keyword))
		{
			throw new IllegalArgumentException("Command with same keyword already exists");
		}
		else
		{
			commands.put(keyword, command);
			logger_.info("Registered command "+keyword+": "+command);
		}
	}

	public static void registerPluginCommand(Command command, Plugin plugin)
	{
		String keyword = command.getKeyword().toLowerCase();
		if(pluginCommands.containsKey(keyword))
		{
			throw new IllegalArgumentException("Command with same keyword already exists");
		}
		else
		{
			pluginCommands.put(keyword, new ImmutablePair<>(command, plugin));
			logger_.info("Registered plugin command "+keyword+" for plugin "+plugin.getName()+": "+command);
		}
	}

	public void showUsage(TextChannel channel, GuildSettings settings)
	{
		String usage = getUsage(settings);
		if(usage==null)
		{
			return;
		}

		String prefix = settings.getPrefix();

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(prefix+getKeyword());

		usage = usage.replace("$p", prefix);
		usage = usage.replace("$c", prefix+getKeyword());
		usage = usage.replace("$C", getKeyword());

		String helpText = getHelpText(settings);
		if(helpText!=null)
		{
			usage = usage.replace("$h", helpText);
		}

		eb.setDescription(usage);

		eb.setColor(ChocoBot.COLOR_COOKIE);
		channel.sendMessage(eb.build()).queue();
	}

	@Nullable
	protected String getUsage(GuildSettings settings)
	{
		return settings.translate("command."+getKeyword()+".usage");
	}

	@Nullable
	public static Command getCommand(String keyword)
	{
		return commands.getOrDefault(keyword.toLowerCase(), null);
	}

	@Nullable
	public static Command getPluginCommand(String keyword, long guild)
	{
		Pair<Command, Plugin> pair = pluginCommands.getOrDefault(keyword.toLowerCase(Locale.ROOT), null);
		if(pair == null)
			return null;

		if(!pair.getRight().isEnabled(guild))
			return null;
		return pair.getLeft();
	}
}
