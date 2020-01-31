package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class CommandCoins extends Command
{
	private PreparedStatement getCoins;

	public CommandCoins()
	{
		try
		{
			this.getCoins = ChocoBot.database.prepareStatement("SELECT coins, last_daily FROM coins WHERE uid=?");
		}
		catch (SQLException var2)
		{
			var2.printStackTrace();
		}

	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		long uid = message.getAuthor().getIdLong();

		try
		{
			this.getCoins.setLong(1, uid);
			ResultSet resultSet = this.getCoins.executeQuery();
			int coins;
			Instant lastDaily;
			if (resultSet.next())
			{
				coins = resultSet.getInt("coins");
				lastDaily = Instant.ofEpochMilli(resultSet.getLong("last_daily"));
			}
			else
			{
				DatabaseUtils.createEmptyUser(uid);
				coins = 0;
				lastDaily = Instant.ofEpochMilli(0L);
			}

			LocalDateTime dateTime = LocalDateTime.ofInstant(lastDaily, ZoneId.systemDefault());
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(":moneybag: Coins :moneybag:");
			builder.setColor(ChocoBot.COLOR_COINS);
			builder.addField("Deine Coins", Integer.toString(coins), false);
			if (dateTime.getLong(ChronoField.EPOCH_DAY) < LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
			{
				builder.setFooter("\ud83d\udc8e Du kannst übrigens deinen täglichen Bonus einfordern! \ud83d\udc8e");
			}

			channel.sendMessage(builder.build()).queue();
			return true;
		}
		catch (SQLException var11)
		{
			var11.printStackTrace();
			return false;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "coins";
	}

	public String getHelpText()
	{
		return "Zeige an, wie viele Coins du hast.";
	}
}
