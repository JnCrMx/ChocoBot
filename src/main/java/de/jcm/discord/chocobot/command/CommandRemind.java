package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

	public CommandRemind()
	{
		this.periodformatter = (new PeriodFormatterBuilder()).appendDays().appendSuffix("d").appendHours().appendSuffix("h").appendMinutes().appendSuffix("min").toFormatter();
		this.dateTimeFormatter = (new DateTimeFormatterBuilder()).appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('.').appendValue(ChronoField.MONTH_OF_YEAR).appendOptional((new DateTimeFormatterBuilder()).appendLiteral('.').appendValue(ChronoField.YEAR).toFormatter()).appendLiteral('/').appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR).toFormatter();
		this.timeFormatter = (new DateTimeFormatterBuilder()).appendValue(ChronoField.HOUR_OF_DAY).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR).toFormatter();
		this.outputFormatter = DateTimeFormatter.ofPattern("'am' dd.MM.uuuu 'um' HH:mm");
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if (args.length < 1)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du musst mindestens einen Zeitpunkt für die Erinnerung angeben!")).queue();
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
					channel.sendMessage(ChocoBot.errorMessage("Du musst mir schon sagen, wann die Erinnerung stattfinden sollt!")).queue();
					return false;
				}

				timeString = args[1];
				user = message.getMentionedUsers().get(0);
				messageDex = 2;
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
				channel.sendMessage(ChocoBot.errorMessage("Ich kann deine Zeitangabe leider nicht verstehen!")).queue();
				return false;
			}
			else if (time.isBefore(LocalDateTime.now()))
			{
				channel.sendMessage(ChocoBot.errorMessage("Dafür bräuchtest du eine Zeitmaschine!")).queue();
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

				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement insertReminder = ChocoBot.getDatabase().prepareStatement("INSERT INTO reminders (uid, message, time, issuer) VALUES(?, ?, ?, ?)"))
				{
					insertReminder.setLong(1, user.getIdLong());
					insertReminder.setString(2, reMessage);
					insertReminder.setLong(3, time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
					insertReminder.setLong(4, message.getAuthor().getIdLong());
					insertReminder.execute();
					String var10001;
					if (message.getAuthor().getIdLong() == user.getIdLong())
					{
						var10001 = message.getAuthor().getAsMention();
						channel.sendMessage(var10001 + ", du wirst " + this.outputFormatter.format(time) + " erinnert!").queue();
					}
					else
					{
						var10001 = message.getAuthor().getAsMention();
						channel.sendMessage(var10001 + ", " + Objects.requireNonNull(channel.getGuild().getMember(user)).getEffectiveName() + " wird " + this.outputFormatter.format(time) + " erinnert!").queue();
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

	@Nullable
	public String getHelpText()
	{
		return "Erinnere mich.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c <Zeit> [<Erinnerungs-Nachricht>] : Erinnere mich zum angegebenen Zeitpunkt.\n" +
				"%c <Dauer> [<Erinnerungs-Nachricht>] : Erinner mich nach angegebener Dauer.\n" +
				"%c <Zeit> <Nutzer> [<Erinnerungs-Nachricht>] : Erinnere jemanden zum angegebenen Zeitpunkt.\n" +
				"%c <Dauer> <Nutzer> [<Erinnerungs-Nachricht>] : Erinner jemanden nach angegebener Dauer.\n" +
				"\n" +
				"Beispiele für Zeit:\n" +
				"``25.03.2020/17:42``\n" +
				"``26.03/15:23``\n" +
				"``18:32``\n" +
				"\n" +
				"Beispiele für Dauer:\n" +
				"``29min``\n" +
				"``1h15min``\n" +
				"``1d5h12min``";
	}
}
