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
			builder.setTitle("Befehle");
			builder.setColor(ChocoBot.COLOR_COOKIE);
			Collection<Command> commands = Command.commands.values();
			commands.stream().sorted((e1, e2) ->
					                         e1.getKeyword()
					                           .compareToIgnoreCase(e2.getKeyword()))
			        .forEach((command) ->
			                 {
				                 if(command
						                 .getHelpText() != null)
				                 {
					                 builder.addField(settings.getPrefix() + command
							                 .getKeyword(), command
							                                  .getHelpText(), false);
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
			if(command == null || command.getHelpText()==null || command.getUsage()==null)
			{
				ChocoBot.errorMessage("Ich konnte diesen Befehl nicht finden!");
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

	public String getHelpText()
	{
		return "Zeige Hilfe an.";
	}

	@Override
	protected String getUsage()
	{
		return  "%c : Zeige Liste aller Befehle\n" +
				"%c <Befehl> : Informiere über Verwendung eines Befehls";
	}
}
