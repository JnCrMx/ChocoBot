package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

class RemindRunnable implements Runnable
{
	private final JDA jda;
	private final DateTimeFormatter outputFormatter;

	public RemindRunnable(JDA jda)
	{
		this.jda = jda;
		this.outputFormatter = DateTimeFormatter.ofPattern("'am' dd.MM.uuuu 'um' HH:mm");
	}

	public void run()
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement listStatement = connection.prepareStatement("SELECT * FROM reminders WHERE time <= ? AND done = 0");
			PreparedStatement doneStatement = connection.prepareStatement("UPDATE reminders SET done = 1 WHERE id = ?"))
		{
			long clock = Clock.systemUTC().millis();

			listStatement.setLong(1, clock);
			try(ResultSet resultSet = listStatement.executeQuery())
			{
				while(resultSet.next())
				{
					int id = resultSet.getInt("id");
					long uid = resultSet.getLong("uid");
					long guildId = resultSet.getLong("guild");
					String message = resultSet.getString("message");
					long issuerId = resultSet.getLong("issuer");
					long time = resultSet.getLong("time");
					long channelId = resultSet.getLong("channel");
					User user = this.jda.retrieveUserById(uid).complete();
					User issuer = this.jda.retrieveUserById(issuerId).complete();

					assert user != null;

					Guild guild = this.jda.getGuildById(guildId);
					if(guild == null)
						continue;
					GuildSettings settings = DatabaseUtils.getSettings(guild);

					TextChannel remindChannel = null;
					if(channelId != 0)
					{
						remindChannel = jda.getTextChannelById(channelId);
					}

					if(remindChannel == null)
					{
						remindChannel = DatabaseUtils.getSettings(guild).getRemindChannel();
						assert remindChannel != null;
					}

					String botMessage;
					if(issuerId != uid)
					{
						if(message != null)
							botMessage = settings.translate(
									"reminder.other.message", user.getAsMention(),
									Objects.requireNonNull(guild.retrieveMember(issuer).complete()).getEffectiveName(),
									message);
						else
							botMessage = settings.translate(
									"reminder.other.plain", user.getAsMention(),
									Objects.requireNonNull(guild.retrieveMember(issuer).complete()).getEffectiveName());
					}
					else
					{
						if(message != null)
							botMessage = settings.translate("reminder.self.message", user.getAsMention(), message);
						else
							botMessage = settings.translate("reminder.self.plain", user.getAsMention());
					}
					if(clock - time > 60000L)
					{
						String timeString = this.outputFormatter.format(
								LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
						botMessage += " " + settings.translate("reminder.delay", timeString);
					}

					remindChannel.sendMessage(botMessage).queue();
					doneStatement.setInt(1, id);
					doneStatement.execute();
				}
			}
		}
		catch (Throwable var17)
		{
			var17.printStackTrace();
		}

	}
}
