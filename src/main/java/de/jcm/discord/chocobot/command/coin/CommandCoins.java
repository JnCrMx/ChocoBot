package de.jcm.discord.chocobot.command.coin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
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
import java.time.ZoneId;
import java.time.temporal.ChronoField;

public class CommandCoins extends Command
{
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		long uid = message.getAuthor().getIdLong();
		boolean foreign = false;

		if(!message.getMentionedUsers().isEmpty())
		{
			uid = message.getMentionedUsers().get(0).getIdLong();
			if(uid != message.getAuthor().getIdLong())
			{
				if(!settings.isOperator(message.getMember()))
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.coins.error.perm")).queue();
					return false;
				}
				foreign = true;
			}
		}

		try
		{
			int coins;
			Instant lastDaily;
			try(Connection connection = ChocoBot.getDatabase();
				PreparedStatement getCoins = connection.prepareStatement("SELECT coins, last_daily FROM coins WHERE uid=? AND guild=?"))
			{
				getCoins.setLong(1, uid);
				getCoins.setLong(2, guild.getIdLong());

				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
					{
						coins = resultSet.getInt("coins");
						lastDaily = Instant.ofEpochMilli(resultSet.getLong("last_daily"));
					}
					else
					{
						DatabaseUtils.createEmptyUser(connection, uid, guild.getIdLong());
						coins = 0;
						lastDaily = Instant.ofEpochMilli(0L);
					}
				}
			}

			LocalDateTime dateTime = LocalDateTime.ofInstant(lastDaily, ZoneId.systemDefault());
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(settings.translate("command.coins.title"));
			builder.setColor(ChocoBot.COLOR_COINS);
			if(foreign)
			{
				builder.addField(
						ChocoBot.provideUser(uid, UserData::getTag, "Unknown user") + "s Coins",
						Integer.toString(coins),
						false);
			}
			else
			{
				builder.addField(settings.translate("command.coins.your"), Integer.toString(coins), false);
				if(dateTime.getLong(ChronoField.EPOCH_DAY) <
						LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
				{
					builder.setFooter(settings.translate("command.coins.daily"));
				}
			}

			channel.sendMessage(builder.build()).queue();
			return true;
		}
		catch(SQLException var11)
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
}
