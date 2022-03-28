package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class CommandBugReport extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
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

				int id = (Integer) response.get("number");
				String htmlURL = (String) response.get("html_url");

				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement insertBugReport = connection.prepareStatement("INSERT INTO bugreports (id, reporter, last_event_time) VALUES (?, ?, ?)"))
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
				eb.setTitle(settings.translate("command.bugreport.title"));
				eb.setColor(ChocoBot.COLOR_COOKIE);
				eb.setDescription(settings.translate("command.bugreport.message.issue", htmlURL));

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
				eb.setTitle(settings.translate("command.bugreport.title"));
				eb.setColor(ChocoBot.COLOR_COOKIE);
				eb.setDescription(settings.translate("command.bugreport.message"));

				channel.sendMessage(eb.build()).queue();
			}
			return true;
		}
		catch(RuntimeException | FileNotFoundException e)
		{
			e.printStackTrace();

			channel.sendMessage(ChocoBot.translateError(settings, "command.bugreport.error.general")).queue();

			return false;
		}
	}

	@Override
	public @NotNull String getKeyword()
	{
		return "bugreport";
	}


	@Override
	public boolean multipleArguments()
	{
		return false;
	}
}
