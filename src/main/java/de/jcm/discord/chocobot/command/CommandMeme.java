package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

public class CommandMeme extends Command
{
	private final WebTarget baseTarget;
	private final Random random = new Random();

	public CommandMeme()
	{
		this.baseTarget = ChocoBot.client.target("https://oauth.reddit.com");
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if (args.length != 1)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du musst mir schon sagen, wo ich das Meme hernehmen soll!")).queue();
			return false;
		}
		else
		{
			String subreddit = args[0];
			if (!subreddit.startsWith("r/"))
			{
				subreddit = "r/" + subreddit;
			}

			WebTarget target = this.baseTarget.path(subreddit).path("new").property("limit", 100);
			Response response = target.request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + ChocoBot.redditToken).get();
			if (response.getStatus() != 200)
			{
				channel.sendMessage(ChocoBot.errorMessage("Ich kann auf diesen Subreddit nicht zugreifen!")).queue();
				return false;
			}
			else
			{
				HashMap<?, ?> map = (HashMap<?, ?>) response.readEntity(HashMap.class);
				ArrayList<?> list = (ArrayList<?>) ((HashMap<?, ?>) map.get("data")).get("children");
				Collections.shuffle(list, this.random);

				for(Object entry : list)
				{
					HashMap<?,?> data = (HashMap<?,?>) ((HashMap<?,?>) entry).get("data");

					boolean over18 = (Boolean) data.get("over_18");
					String url = (String) data.get("url");
					if (this.matchUrl(url))
					{
						if (!over18 || channel.isNSFW())
						{
							channel.sendMessage(url).queue();
							return true;
						}

						this.logger.debug(String.format("Rejected url \"%s\" because it is marked as over 18 and channel \"%s\" (%s) is not a NSFW channel.", url, channel.getName(), channel.getId()));
					}
					else
					{
						this.logger.debug("Rejected url \"" + url + "\" because it does not seem to be a meme. Is that correct?");
					}
				}
				channel.sendMessage(ChocoBot.errorMessage("Ich konnte keine Memes finden!")).queue();
				return false;
			}
		}
	}

	private boolean matchUrl(String url)
	{
		if (url.endsWith(".png"))
		{
			return true;
		}
		else if (!url.endsWith(".jpg") && !url.endsWith(".jpeg"))
		{
			if (url.matches("https://(www\\.|)youtube\\.com/watch\\?v=.*"))
			{
				return true;
			}
			else if (url.matches("https://youtu\\.be/.*"))
			{
				return true;
			}
			else if (url.matches("https://(www\\.|)twitter.com/.*/status/.*"))
			{
				return true;
			}
			else
			{
				return url.matches("https://(www\\.|)gfycat\\.com/.*");
			}
		}
		else
		{
			return true;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "meme";
	}

	@Nullable
	public String getHelpText()
	{
		return "Zeige ein zufälliges Meme aus einem Subreddit an.";
	}
}
