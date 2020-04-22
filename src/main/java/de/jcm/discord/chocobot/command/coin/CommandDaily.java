package de.jcm.discord.chocobot.command.coin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class CommandDaily extends Command
{
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		long uid = message.getAuthor().getIdLong();

		try
		{
			Instant lastDaily;
			int dailyStreak;
			try(Connection connection = ChocoBot.getDatabase();
				PreparedStatement getCoins = connection.prepareStatement("SELECT last_daily, daily_streak FROM coins WHERE uid=?"))
			{
				getCoins.setLong(1, uid);
				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
					{
						lastDaily = Instant.ofEpochMilli(resultSet.getLong("last_daily"));
						dailyStreak = resultSet.getInt("daily_streak");
					}
					else
					{
						DatabaseUtils.createEmptyUser(uid);
						lastDaily = Instant.ofEpochMilli(0L);
						dailyStreak = 0;
					}
				}
			}

			LocalDateTime dateTime = LocalDateTime.ofInstant(lastDaily, ZoneId.systemDefault());
			if (dateTime.getLong(ChronoField.EPOCH_DAY) < LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
			{
				EmbedBuilder builder = new EmbedBuilder();
				if (dateTime.getLong(ChronoField.EPOCH_DAY) + 1L < LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
				{
					dailyStreak = 0;
					builder.setFooter("Du hast deine Streak verloren! \ud83d\ude2d");
				}

				int coinsToAdd = (dailyStreak + 1) * 10;
				dailyStreak++;

				if(dailyStreak >= 7)
				{
					dailyStreak = 0;
				}

				try(Connection connection = ChocoBot.getDatabase();
				    PreparedStatement updateCoins = connection.prepareStatement("UPDATE coins SET last_daily=?, daily_streak=?, coins=coins+? WHERE uid=?"))
				{
					updateCoins.setLong(1, System.currentTimeMillis());
					updateCoins.setInt(2, dailyStreak);
					updateCoins.setInt(3, coinsToAdd);
					updateCoins.setLong(4, uid);
					updateCoins.execute();
				}

				int coins = DatabaseUtils.getCoins(uid);
				builder.setTitle(":moneybag: Coins :moneybag:");
				builder.setColor(ChocoBot.COLOR_COINS);
				builder.setDescription("Du hast einen täglichen Bonus von " + coinsToAdd + " Coins erhalten!");
				builder.addField("Deine Coins", Integer.toString(coins), false);
				builder.addField("Deine Streak", Integer.toString(dailyStreak), false);
				if (dailyStreak == 0)
				{
					builder.setFooter("Deine Streak wurde zurückgesetzt, da sie das Maximum von 7 Tagen überschritten hat.");
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
