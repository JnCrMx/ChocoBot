package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.PrivateChannel;

import java.sql.Connection;
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
	@Override
	public void run()
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM bugreports");
		    ResultSet set = listStatement.executeQuery();
		    PreparedStatement updateTimeStatement = connection.prepareStatement("UPDATE bugreports SET last_event_time=? WHERE id=?"))
		{

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
							ChocoBot.jda.openPrivateChannelById(reporter).complete());

					LocalDateTime maxLastEventTime = lastEventTime;

					GuildSettings settings = null;
					List<?> events = ChocoBot.githubApp.getIssueEvents(id);
					for(Object o : events)
					{
						Map<?, ?> event = (Map<?, ?>) o;
						TemporalAccessor temp = DateTimeFormatter.ISO_DATE_TIME
								.parse((String) event.get("created_at"));
						LocalDateTime time = LocalDateTime.from(temp);
						if(time.isAfter(lastEventTime))
						{
							if(settings == null)
								settings = DatabaseUtils.getUserSettings(channel.getUser());

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
							eb.setTitle(settings.translate("issue_events.title", issue.get("title")),
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
									eb.setDescription(settings.translate(
											"issue_events.event.closed_commit", message, htmlUrl));
								}
								else
								{
									eb.setDescription(settings.translate("issue_events.event.closed"));
								}
							}

							eb.setFooter(settings.translate("issue_events.footer"));

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
							if(settings == null)
								settings = DatabaseUtils.getUserSettings(channel.getUser());

							Map<?, ?> user = (Map<?, ?>) comment.get("user");
							Map<?, ?> issue = ChocoBot.githubApp.get(
									(String) comment.get("issue_url"), Map.class);

							EmbedBuilder eb = new EmbedBuilder();
							eb.setAuthor(
									(String) user.get("login"),
									(String) user.get("html_url"),
									(String) user.get("avatar_url"));
							eb.setTimestamp(time);
							eb.setTitle(settings.translate("issue_events.title", issue.get("title")),
							            (String) issue.get("html_url"));

							eb.setDescription(settings.translate("issue_events.event.comment",
							                                comment.get("html_url"), comment.get("body")));

							eb.setFooter(settings.translate("issue_events.footer"));

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
