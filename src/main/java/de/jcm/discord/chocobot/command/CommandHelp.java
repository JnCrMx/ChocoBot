package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandHelp extends Command
{
	public CommandHelp()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if(args.length==0)
		{
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(settings.translate("command.help.title"));
			builder.setColor(ChocoBot.COLOR_COOKIE);
			Collection<Command> commands = Command.commands.values();
			commands.stream().sorted((e1, e2) ->
					                         e1.getKeyword()
					                           .compareToIgnoreCase(e2.getKeyword()))
			        .forEach((command) ->
			                 {
				                 if(command.getHelpText(settings) != null)
				                 {
					                 builder.addField(settings.getPrefix() + command
							                 .getKeyword(), command.getHelpText(settings), false);
				                 }

			                 });
			channel.sendMessage(builder.build()).queue();
			return true;
		}
		else if(args.length == 1)
		{
			String keyword = args[0];
			if(keyword.startsWith(settings.getPrefix()))
			{
				keyword = keyword.substring(settings.getPrefix().length());
			}

			Command command = Command.getCommand(keyword);
			if(command == null || command.getHelpText(settings)==null || command.getUsage(settings)==null)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.help.error.noent")).queue();
				return false;
			}
			command.showUsage(channel, settings);

			return true;
		}
		else
		{
			showUsage(channel, settings);
			return false;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "help";
	}
}
