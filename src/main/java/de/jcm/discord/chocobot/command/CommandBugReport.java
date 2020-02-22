package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class CommandBugReport extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		boolean showTag = false;
		String arg = args[0];

		if(arg.endsWith("+tag"))
		{
			showTag = true;
			arg = arg.replace("+tag", "").trim();
		}

		String title = arg;
		String body = null;
		if(arg.contains("\n"))
		{
			title = arg.substring(0, arg.indexOf('\n'));
			body = arg.substring(arg.indexOf('\n')+1);
		}

		try
		{
			HashMap<?, ?> response =
					ChocoBot.githubApp.createIssue(title, body, showTag ? message.getAuthor() : null);

			String htmlURL = (String) response.get("html_url");

			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Fehlermeldung", htmlURL);
			eb.setColor(ChocoBot.COLOR_COOKIE);
			eb.setDescription("Deine Fehlermeldung wurde erfolgreich gesendet!");

			channel.sendMessage(eb.build()).queue();

			return true;
		}
		catch(RuntimeException e)
		{
			channel.sendMessage(
						ChocoBot.errorMessage("Es trat ein Fehler beim Senden der Fehlermeldung auf!"))
			       .queue();

			return false;
		}
	}

	@Override
	public @NotNull String getKeyword()
	{
		return "bugreport";
	}

	@Override
	public @Nullable String getHelpText()
	{
		return "Sende eine Fehlermeldung.";
	}

	@Override
	public boolean multipleArguments()
	{
		return false;
	}
}
