package de.jcm.discord.chocobot.command.coin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class CommandDaily extends Command
{
	private static final double DAILY_FACTOR = 30 / Math.log1p(6);
	private static final double DAILY_START = 10;

	public static int getCoinsForStreak(int streak)
	{
		return (int) (Math.log1p(streak) * DAILY_FACTOR + DAILY_START);
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		long uid = message.getAuthor().getIdLong();

		try
		{
			Instant lastDaily;
			int dailyStreak;

			LocalDateTime now = LocalDateTime.now();
			boolean christmas = now.getMonth() == Month.DECEMBER &&
					now.getDayOfMonth() >= 24 && now.getDayOfMonth() <= 26;
			boolean christmasGifts = false;

			try(Connection connection = ChocoBot.getDatabase();
				PreparedStatement getCoins = connection.prepareStatement("SELECT last_daily, daily_streak FROM coins WHERE uid=? AND guild=?");
				PreparedStatement checkChristmasGifts = connection.prepareStatement("SELECT id FROM christmas_presents WHERE uid=? AND guild=? AND opened=0"))
			{
				getCoins.setLong(1, uid);
				getCoins.setLong(2, guild.getIdLong());
				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
					{
						lastDaily = Instant.ofEpochMilli(resultSet.getLong("last_daily"));
						dailyStreak = resultSet.getInt("daily_streak");
					}
					else
					{
						DatabaseUtils.createEmptyUser(connection, uid, guild.getIdLong());
						lastDaily = Instant.ofEpochMilli(0L);
						dailyStreak = 0;
					}
				}

				if(christmas)
				{
					checkChristmasGifts.setLong(1, uid);
					checkChristmasGifts.setLong(2, guild.getIdLong());
					ResultSet resultSet = checkChristmasGifts.executeQuery();
					christmasGifts = resultSet.next();
				}
			}

			LocalDateTime dateTime = LocalDateTime.ofInstant(lastDaily, ZoneId.systemDefault());
			if (dateTime.getLong(ChronoField.EPOCH_DAY) < now.getLong(ChronoField.EPOCH_DAY))
			{
				EmbedBuilder builder = new EmbedBuilder();

				boolean christmasSave = false;
				if (dateTime.getLong(ChronoField.EPOCH_DAY) + 1L < LocalDateTime.now().getLong(ChronoField.EPOCH_DAY)
						&& lastDaily.getEpochSecond() != 0)
				{
					if(christmas) // streaks don't decay on Christmas ;)
					{
						christmasSave = true;
					}
					else
					{
						dailyStreak = 0;
						builder.setFooter("Du hast deine Streak verloren! \ud83d\ude2d");
					}
				}

				int coinsToAdd = getCoinsForStreak(dailyStreak) * (christmas ? 2 : 1);
				dailyStreak++;

				int coins;
				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement updateCoins = connection.prepareStatement("UPDATE coins SET last_daily=?, daily_streak=?, coins=coins+? WHERE uid=? AND guild=?"))
				{
					updateCoins.setLong(1, System.currentTimeMillis());
					updateCoins.setInt(2, dailyStreak);
					updateCoins.setInt(3, coinsToAdd);
					updateCoins.setLong(4, uid);
					updateCoins.setLong(5, guild.getIdLong());
					updateCoins.execute();

					coins = DatabaseUtils.getCoins(connection, uid, guild.getIdLong());

					DatabaseUtils.updateStat(connection, uid, guild.getIdLong(), "daily.max_streak", dailyStreak);
					DatabaseUtils.updateStat(connection, uid, guild.getIdLong(), "max_coins", coins);
				}
				builder.setTitle(":moneybag: Coins :moneybag:");
				builder.setColor(ChocoBot.COLOR_COINS);
				builder.setDescription("Du hast einen täglichen Bonus von " + coinsToAdd + " Coins erhalten!");
				builder.addField("Deine Coins", Integer.toString(coins), false);
				builder.addField("Deine Streak", Integer.toString(dailyStreak), false);

				if(christmas)
				{
					builder.appendDescription("\nDa Weihnachten ist, erhältst du doppelt so viele Coins! \uD83C\uDF84");
				}
				if(christmasSave)
				{
					builder.appendDescription("\nEin Weihnachtsengel hat deine Streak gerettet! \uD83D\uDC7C");
				}
				if(christmasGifts)
				{
					builder.setFooter("Du hast noch ungeöffnete Weihnachtsgeschenke! \uD83C\uDF81");
				}

				channel.sendMessage(builder.build()).queue();
				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Du hast deinen täglichen Bonus heute bereits eingefordert!")).queue();
				return false;
			}
		}
		catch (SQLException var13)
		{
			var13.printStackTrace();
			return false;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "daily";
	}

	public String getHelpText()
	{
		return "Erhalte deinen täglichen Coin-Bonus.";
	}

	@Override
	protected String getUsage()
	{
		return "%c : %h";
	}
}
