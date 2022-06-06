package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.regex.Pattern;

public class CommandRemind extends Command
{
	private final PeriodFormatter periodformatter;
	private final DateTimeFormatter dateTimeFormatter;
	private final DateTimeFormatter timeFormatter;
	private final DateTimeFormatter outputFormatter;

	private final int MAX_REMINDERS_SELF = 10;
	private final int MAX_REMINDERS_OTHER = 5;
	private final int COIN_FACTOR_SELF = 100;
	private final int COIN_FACTOR_OTHER = 1000;

	public CommandRemind()
	{
		this.periodformatter = (new PeriodFormatterBuilder()).appendDays().appendSuffix("d").appendHours().appendSuffix("h").appendMinutes().appendSuffix("min").toFormatter();
		this.dateTimeFormatter = (new DateTimeFormatterBuilder()).appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('.').appendValue(ChronoField.MONTH_OF_YEAR).appendOptional((new DateTimeFormatterBuilder()).appendLiteral('.').appendValue(ChronoField.YEAR).toFormatter()).appendLiteral('/').appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR).toFormatter();
		this.timeFormatter = (new DateTimeFormatterBuilder()).appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR).toFormatter();
		this.outputFormatter = DateTimeFormatter.ofPattern("'am' dd.MM.uuuu 'um' HH:mm");
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (args.length < 1)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.narg1")).queue();
			return false;
		}
		else
		{
			Pattern pattern = Pattern.compile("<@(|!)([0-9]*)>");
			String timeString;
			User user;
			byte messageDex;
			if (message.getMentionedUsers().isEmpty() && !pattern.matcher(args[0]).matches())
			{
				timeString = args[0];
				user = message.getAuthor();
				messageDex = 1;
			}
			else
			{
				if (args.length < 2)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.narg2")).queue();
					return false;
				}

				timeString = args[1];
				user = message.getMentionedUsers().get(0);
				messageDex = 2;
			}

			int cost = 0;
			int count = 0;
			boolean self = user.getIdLong() == message.getAuthor().getIdLong();
			if(!self)
			{
				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement countReminders = connection.prepareStatement("SELECT COUNT(*) FROM reminders WHERE guild=? AND uid<>? AND issuer=? AND done=0"))
				{
					countReminders.setLong(1, guild.getIdLong());
					countReminders.setLong(2, message.getAuthor().getIdLong());
					countReminders.setLong(3, message.getAuthor().getIdLong());
					try(ResultSet set = countReminders.executeQuery())
					{
						if(set.next()) count = set.getInt(1);
					}
				}
				catch(SQLException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement countReminders = connection.prepareStatement("SELECT COUNT(*) FROM reminders WHERE guild=? AND uid=? AND issuer=? AND done=0"))
				{
					countReminders.setLong(1, guild.getIdLong());
					countReminders.setLong(2, message.getAuthor().getIdLong());
					countReminders.setLong(3, message.getAuthor().getIdLong());
					try(ResultSet set = countReminders.executeQuery())
					{
						if(set.next()) count = set.getInt(1);
					}
				}
				catch(SQLException e)
				{
					e.printStackTrace();
				}
			}
			int c = count - (self ? MAX_REMINDERS_SELF : MAX_REMINDERS_OTHER);
			if(c > 0)
			{
				cost = (self ? COIN_FACTOR_SELF : COIN_FACTOR_OTHER) * c * c;
				int coinsOwned = DatabaseUtils.getCoins(message.getAuthor().getIdLong(), guild.getIdLong());
				if(cost > coinsOwned)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.cost")).queue();
					return false;
				}
				DatabaseUtils.changeCoins(message.getAuthor().getIdLong(), guild.getIdLong(), -cost);
			}

			LocalDateTime time = null;

			try
			{
				Period period = this.periodformatter.parsePeriod(timeString);
				long millis = period.toStandardDuration().getMillis();
				time = LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() + millis), ZoneId.systemDefault());
			}
			catch (IllegalArgumentException ignored)
			{
			}

			try
			{
				time = this.dateTimeFormatter.parse(timeString, (ta) ->
				{
					LocalDateTime time1 = LocalDateTime.now().withMonth(ta.get(ChronoField.MONTH_OF_YEAR)).withDayOfMonth(ta.get(ChronoField.DAY_OF_MONTH)).withHour(ta.get(ChronoField.HOUR_OF_DAY)).withMinute(ta.get(ChronoField.MINUTE_OF_HOUR)).withSecond(0).withNano(0);
					if (ta.isSupported(ChronoField.YEAR))
					{
						return time1.withYear(ta.get(ChronoField.YEAR));
					}
					else
					{
						return time1.isBefore(LocalDateTime.now()) ? time1.plusYears(1L) : time1;
					}
				});
			}
			catch (DateTimeParseException ignored)
			{
			}

			try
			{
				time = this.timeFormatter.parse(timeString, (ta) ->
				{
					LocalDateTime time1 = LocalDateTime.now().withHour(ta.get(ChronoField.HOUR_OF_DAY)).withMinute(ta.get(ChronoField.MINUTE_OF_HOUR)).withSecond(0).withNano(0);
					return time1.isBefore(LocalDateTime.now()) ? time1.plusDays(1L) : time1;
				});
			}
			catch (DateTimeParseException ignored)
			{
			}

			if (time == null)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.fmt")).queue();
				return false;
			}
			else if (time.isBefore(LocalDateTime.now()))
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.past")).queue();
				return false;
			}
			else
			{
				String reMessage = null;
				if (messageDex < args.length)
				{
					String[] reasonArray = new String[args.length - messageDex];
					System.arraycopy(args, messageDex, reasonArray, 0, reasonArray.length);
					reMessage = String.join(" ", reasonArray);
				}

				if(reMessage != null && (reMessage.contains("@everyone") || reMessage.contains("@here")))
				{
					if(!settings.isOperator(Objects.requireNonNull(message.getMember())))
					{
						channel.sendMessage(ChocoBot.translateError(settings, "command.remind.error.perm")).queue();
						return false;
					}
				}

				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement insertReminder = connection.prepareStatement("INSERT INTO reminders (uid, guild, message, time, issuer, channel) VALUES(?, ?, ?, ?, ?, ?)"))
				{
					insertReminder.setLong(1, user.getIdLong());
					insertReminder.setLong(2, guild.getIdLong());
					insertReminder.setString(3, reMessage);
					insertReminder.setLong(4, time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
					insertReminder.setLong(5, message.getAuthor().getIdLong());
					insertReminder.setLong(6, channel.getIdLong());
					insertReminder.execute();
					String var10001;
					if (message.getAuthor().getIdLong() == user.getIdLong())
					{
						var10001 = message.getAuthor().getAsMention();
						channel.sendMessage(settings.translate(cost>0?"command.remind.self.cost":"command.remind.self", var10001, this.outputFormatter.format(time), cost)).queue();
					}
					else
					{
						var10001 = message.getAuthor().getAsMention();
						channel.sendMessage(settings.translate(cost>0?"command.remind.other.cost":"command.remind.other", var10001, user.getName(), this.outputFormatter.format(time), cost)).queue();
					}
				}
				catch (SQLException var12)
				{
					var12.printStackTrace();
				}

				return true;
			}
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "remind";
	}
}
