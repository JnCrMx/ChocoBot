package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandHelp extends Command
{
	public CommandHelp()
	{
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Befehle");
		builder.setColor(ChocoBot.COLOR_COOKIE);
		Collection<Command> commands = Command.commands.values();
		commands.stream().sorted((e1, e2) ->
				e1.getKeyword().compareToIgnoreCase(e2.getKeyword())).forEach((command) ->
		{
			if (command.getHelpText() != null)
			{
				builder.addField("?" + command.getKeyword(), command.getHelpText(), false);
			}

		});
		channel.sendMessage(builder.build()).queue();
		return true;
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
}
