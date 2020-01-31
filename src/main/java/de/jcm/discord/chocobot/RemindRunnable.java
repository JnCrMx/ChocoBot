package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class RemindRunnable implements Runnable
{
	private final JDA jda;
	private PreparedStatement listStatement;
	private PreparedStatement doneStatement;
	private final DateTimeFormatter outputFormatter;

	public RemindRunnable(JDA jda)
	{
		this.jda = jda;

		try
		{
			this.listStatement = ChocoBot.database.prepareStatement("SELECT * FROM reminders WHERE time <= ? AND done = 0");
			this.doneStatement = ChocoBot.database.prepareStatement("UPDATE reminders SET done = 1 WHERE id = ?");
		}
		catch (SQLException var3)
		{
			var3.printStackTrace();
		}

		this.outputFormatter = DateTimeFormatter.ofPattern("'am' dd.MM.uuuu 'um' HH:mm");
	}

	public void run()
	{
		try
		{
			long clock = Clock.systemUTC().millis();
			TextChannel remindChannel = this.jda.getTextChannelById(ChocoBot.remindChannel);

			assert remindChannel != null;

			Guild guild = remindChannel.getGuild();
			this.listStatement.setLong(1, clock);
			ResultSet resultSet = this.listStatement.executeQuery();

			while (resultSet.next())
			{
				int id = resultSet.getInt("id");
				long uid = resultSet.getLong("uid");
				String message = resultSet.getString("message");
				long issuerId = resultSet.getLong("issuer");
				long time = resultSet.getLong("time");
				User user = this.jda.getUserById(uid);
				User issuer = this.jda.getUserById(issuerId);
				StringBuilder botMessage = new StringBuilder();

				assert user != null;

				botMessage.append(user.getAsMention());
				botMessage.append(", ich soll dich");
				if (issuerId != uid)
				{
					botMessage.append(" von ");

					assert issuer != null;

					botMessage.append(Objects.requireNonNull(guild.getMember(issuer)).getEffectiveName());
				}

				if (message != null)
				{
					botMessage.append(" an ");
					botMessage.append('"');
					botMessage.append(message);
					botMessage.append('"');
				}

				botMessage.append(" erinnern!");
				if (clock - time > 60000L)
				{
					botMessage.append(" Ich bin leider verspätet! Die Erinnerung sollte eigentlich ");
					botMessage.append(this.outputFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())));
					botMessage.append(" erinnern.");
				}

				remindChannel.sendMessage(botMessage.toString()).queue();
				this.doneStatement.setInt(1, id);
				this.doneStatement.execute();
			}
		}
		catch (Throwable var17)
		{
			var17.printStackTrace();
		}

	}
}
