package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
					User user = this.jda.getUserById(uid);
					User issuer = this.jda.getUserById(issuerId);

					assert user != null;

					Guild guild = this.jda.getGuildById(guildId);
					assert guild != null;

					TextChannel remindChannel = DatabaseUtils.getSettings(guild).getRemindChannel();
					assert remindChannel != null;

					StringBuilder botMessage = new StringBuilder();
					botMessage.append(user.getAsMention());
					botMessage.append(", ich soll dich");
					if(issuerId != uid)
					{
						botMessage.append(" von ");

						assert issuer != null;

						botMessage.append(Objects.requireNonNull(guild.getMember(issuer)).getEffectiveName());
					}

					if(message != null)
					{
						botMessage.append(" an ");
						botMessage.append('"');
						botMessage.append(message);
						botMessage.append('"');
					}

					botMessage.append(" erinnern!");
					if(clock - time > 60000L)
					{
						botMessage.append(" Ich bin leider verspätet! Die Erinnerung sollte eigentlich ");
						botMessage.append(this.outputFormatter.format(LocalDateTime
								                                              .ofInstant(Instant.ofEpochMilli(time), ZoneId
										                                              .systemDefault())));
						botMessage.append(" erinnern.");
					}

					remindChannel.sendMessage(botMessage.toString()).queue();
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
