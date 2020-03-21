package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IssueEventRunnable implements Runnable
{
	private PreparedStatement listStatement;
	private PreparedStatement updateTimeStatement;

	public IssueEventRunnable()
	{
		try
		{
			this.listStatement = ChocoBot.database.prepareStatement("SELECT * FROM bugreports");
			this.updateTimeStatement = ChocoBot.database
					.prepareStatement("UPDATE bugreports SET last_event_time=? WHERE id=?");
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		try
		{
			ResultSet set = listStatement.executeQuery();
			while(set.next())
			{
				try
				{
					int id = set.getInt("id");
					long reporter = set.getLong("reporter");
					long lastEventTimeSec = set.getLong("last_event_time");
					LocalDateTime lastEventTime = LocalDateTime.ofEpochSecond(
							lastEventTimeSec, 0, ZoneOffset.UTC);

					PrivateChannel channel = Objects.requireNonNull(
							ChocoBot.jda.getUserById(reporter))
					                                .openPrivateChannel().complete();

					LocalDateTime maxLastEventTime = lastEventTime;

					List<?> events = ChocoBot.githubApp.getIssueEvents(id);
					for(Object o : events)
					{
						Map<?, ?> event = (Map<?, ?>) o;
						TemporalAccessor temp = DateTimeFormatter.ISO_DATE_TIME
								.parse((String) event.get("created_at"));
						LocalDateTime time = LocalDateTime.from(temp);
						if(time.isAfter(lastEventTime))
						{
							event = ChocoBot.githubApp.getIssueEvent(event);

							Map<?, ?> actor = (Map<?, ?>) event.get("actor");
							Map<?, ?> issue = (Map<?, ?>) event.get("issue");
							String eventType = (String) event.get("event");

							EmbedBuilder eb = new EmbedBuilder();
							eb.setAuthor(
									(String) actor.get("login"),
									(String) actor.get("html_url"),
									(String) actor.get("avatar_url"));
							eb.setTimestamp(time);
							eb.setTitle(String.format("Neuigkeiten zu \"%s\"", issue.get("title")),
							            (String) issue.get("html_url"));

							if(eventType.equals("closed"))
							{
								String commitUrl = (String) event.get("commit_url");
								if(commitUrl != null)
								{
									Map<?, ?> commit = ChocoBot.githubApp.get(commitUrl, Map.class);
									String htmlUrl = (String) commit.get("html_url");
									String message = (String)
											((Map<?, ?>) commit.get("commit"))
													.get("message");
									eb.setDescription(String.format(
											"Der Issue wurde durch die Änderung \"%s\" (%s) geschlossen.",
											message, htmlUrl));
								}
								else
								{
									eb.setDescription("Der Issue wurde geschlossen.");
								}
							}

							eb.setFooter("Reagiere mit \u274c um diese Benachrichtigungen abzubestellen");

							channel.sendMessage(eb.build()).queue();

							if(time.isAfter(maxLastEventTime))
								maxLastEventTime = time;
						}
					}

					List<?> comments = ChocoBot.githubApp.getIssueComments(id);
					for(Object o : comments)
					{
						Map<?, ?> comment = (Map<?, ?>) o;
						TemporalAccessor temp = DateTimeFormatter.ISO_DATE_TIME
								.parse((String) comment.get("updated_at"));
						LocalDateTime time = LocalDateTime.from(temp);
						if(time.isAfter(lastEventTime))
						{
							Map<?, ?> user = (Map<?, ?>) comment.get("user");
							Map<?, ?> issue = ChocoBot.githubApp.get(
									(String) comment.get("issue_url"), Map.class);

							EmbedBuilder eb = new EmbedBuilder();
							eb.setAuthor(
									(String) user.get("login"),
									(String) user.get("html_url"),
									(String) user.get("avatar_url"));
							eb.setTimestamp(time);
							eb.setTitle(String.format("Neuigkeiten zu \"%s\"", issue.get("title")),
							            (String) issue.get("html_url"));

							eb.setDescription(String.format("Der Issue wurde kommentiert:\n%s\n\n\n%s",
							                                comment.get("html_url"), comment.get("body")));

							eb.setFooter("Reagiere mit \u274c um diese Benachrichtigungen abzubestellen");

							channel.sendMessage(eb.build()).queue();

							if(time.isAfter(maxLastEventTime))
								maxLastEventTime = time;
						}
					}

					long maxSec = maxLastEventTime.toEpochSecond(ZoneOffset.UTC);
					updateTimeStatement.setLong(1, maxSec);
					updateTimeStatement.setInt(2, id);
					updateTimeStatement.execute();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
}
