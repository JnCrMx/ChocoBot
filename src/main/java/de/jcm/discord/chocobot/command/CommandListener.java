package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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

	static Command findCommand(String keyword, Guild guild, boolean alias)
	{
		Command command;
		if(alias) // prevent recursion
		{
			command = CommandAlias.forGuild(guild, keyword);
			if(command != null)
				return command;
		}

		command = Command.getCommand(keyword);
		if(command != null)
			return command;

		command = CommandCustom.forGuild(guild.getIdLong(), keyword);
		if(command != null)
			return command;

		command = Command.getPluginCommand(keyword, guild.getIdLong());
		if(command != null)
			return command;

		return null;
	}

	public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event)
	{
		if(event.getAuthor().getIdLong() == 829006607391129660L)
			return;

		if (!event.getAuthor().isBot())
		{
			String message = event.getMessage().getContentRaw();

			Guild guild = event.getGuild();
			GuildSettings guildSettings = DatabaseUtils.getSettings(guild);
			if(guildSettings == null)
				return;

			if(message.startsWith(guildSettings.getPrefix()))
			{
				String keyword = message.substring(guildSettings.getPrefix().length()).trim();
				if (keyword.contains(" "))
				{
					keyword = keyword.split(" ")[0];
				}

				Command command = findCommand(keyword, guild, true);

				if (command != null)
				{
					Member member = event.getMember();
					assert member != null;

					if(event.getChannel().getId().equals(guildSettings.getCommandChannel().getId()) ||
							command.usableEverywhere() ||
							guildSettings.isOperator(member))
					{
						if ((System.currentTimeMillis() -
								lastCommands.getOrDefault(event.getAuthor().getIdLong(), 0L))
								< 3000 &&
								!guildSettings.isOperator(member))
						{
							this.logger.info("Command \"{}\" from user {} ({}) was ignored due to delay not met.",
									message, event.getAuthor().getAsTag(), event.getAuthor().getId());
							event.getChannel().sendMessage(
									ChocoBot.translateError(guildSettings, "command.error.timeout", event.getMember().getEffectiveName()))
									.queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
							event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
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

							boolean result = command.execute(event.getMessage(), event.getChannel(), guild, guildSettings, args);
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
					else
					{
						this.logger.info(
								"Command \"{}\" from user {} ({}) was ignored because it was sent to wrong channel.",
								message, event.getAuthor().getAsTag(), event.getAuthor().getId());

						event.getChannel().sendMessage(
								guildSettings.translate("command.error.channel", event.getAuthor().getAsMention(),
								                        guildSettings.getCommandChannel().getAsMention()))
								.queue(s->s.delete().queueAfter(10, TimeUnit.SECONDS));
						event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
					}
				}
			}

		}
	}
}
