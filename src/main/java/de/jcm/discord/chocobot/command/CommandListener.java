package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CommandListener extends ListenerAdapter
{
	private final Logger logger = LoggerFactory.getLogger(CommandListener.class);

	private final HashMap<Long, Long> lastCommands = new HashMap<>();

	public CommandListener()
	{
	}

	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		if (!event.getAuthor().isBot())
		{
			String message = event.getMessage().getContentRaw();
			if (message.startsWith(ChocoBot.prefix))
			{
				String keyword = message.substring(ChocoBot.prefix.length()).trim();
				if (keyword.contains(" "))
				{
					keyword = keyword.split(" ")[0];
				}

				Command command = Command.getCommand(keyword);
				if (command != null)
				{
					if((System.currentTimeMillis()-
							lastCommands.getOrDefault(event.getAuthor().getIdLong(), 0L))
							<5000)
					{
						this.logger.info("Command \"{}\" from user {} ({}) was ignored due to delay not met.",
								message, event.getAuthor().getAsTag(), event.getAuthor().getId());
						event.getChannel().sendMessage(
								ChocoBot.errorMessage(event.getMember().getEffectiveName()+
								", bitte warte etwas, bevor du weitere Befehle sendest!"))
								.queue(m->m.delete().queueAfter(10, TimeUnit.SECONDS));
					}
					else
					{
						String[] args;
						String argstring;
						if (command.multipleArguments())
						{
							if (message.contains(" "))
							{
								argstring = message.substring(message.indexOf(32) + 1);
								args = argstring.split(" ");
							}
							else
							{
								args = new String[0];
							}
						}
						else
						{
							if (message.contains(" "))
							{
								argstring = message.substring(message.indexOf(' ') + 1);
								args = new String[]{argstring};
							}
							else
							{
								args = new String[0];
							}
						}

						lastCommands.put(event.getAuthor().getIdLong(), System.currentTimeMillis());

						boolean result = command.execute(event.getMessage(), event.getTextChannel(), args);
						if (result)
						{
							this.logger.info("Command \"{}\" from user {} ({}) succeeded.", message, event.getAuthor().getAsTag(), event.getAuthor().getId());
						}
						else
						{
							this.logger.info("Command \"{}\" from user {} ({}) failed.", message, event.getAuthor().getAsTag(), event.getAuthor().getId());
						}
					}
				}
			}

		}
	}
}
