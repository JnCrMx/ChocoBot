package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class CommandBugReport extends Command
{
	private PreparedStatement insertBugReport;

	public CommandBugReport()
	{
		try
		{
			insertBugReport = ChocoBot.database.prepareStatement("INSERT INTO bugreports (id, reporter, last_event_time) VALUES (?, ?, ?)");
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

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
			if(ChocoBot.githubBugReportEnabled)
			{
				HashMap<?, ?> response =
						ChocoBot.githubApp.createIssue(title, body, showTag ? message.getAuthor() : null);

				int id = (Integer) response.get("id");
				String htmlURL = (String) response.get("html_url");

				try
				{
					insertBugReport.setInt(1, id);
					insertBugReport.setLong(2, message.getAuthor().getIdLong());
					insertBugReport.setLong(3, (System.currentTimeMillis()/1000)+10);
					insertBugReport.execute();
				}
				catch(SQLException e)
				{
					e.printStackTrace();
					return false;
				}

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Issue");
				eb.setColor(ChocoBot.COLOR_COOKIE);
				eb.setDescription("Dein Issue wurde erfolgreich gesendet:\n"+htmlURL);

				channel.sendMessage(eb.build()).queue();
			}
			else
			{
				long time = System.currentTimeMillis();

				File bugreport = new File(ChocoBot.bugReportDirectory, time +".txt");
				PrintStream print = new PrintStream(bugreport);

				print.println("Time: "+time);
				if(showTag)
					print.println("User: "+message.getAuthor().getAsTag());
				print.println();
				print.println(title);
				print.println();
				print.println(body);

				print.close();

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle("Fehlermeldung");
				eb.setColor(ChocoBot.COLOR_COOKIE);
				eb.setDescription("Deine Fehlermeldung wurde erfolgreich gespeichert!");

				channel.sendMessage(eb.build()).queue();
			}
			return true;
		}
		catch(RuntimeException | FileNotFoundException e)
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

	@Override
	protected String getUsage()
	{
		return  "%c <Titel> : Sende eine Fehlermeldung (nur aus Titel bestehend).\n" +
				"%c <Titel>(Zeilenumbruch)<Text> : Sende eine Fehlermeldung mit Kommentar.\n" +
				"\n" +
				"Optionen:\n" +
				"+tag (am Ende) : füge den Discord-Tag des Absenders in die Fehlermeldung ein.";
	}
}
