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
import java.util.Objects;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public class CommandCoins extends Command
{
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		long uid = message.getAuthor().getIdLong();
		boolean foreign = false;

		if(!message.getMentionedUsers().isEmpty())
		{
			uid = message.getMentionedUsers().get(0).getIdLong();
			if(uid != message.getAuthor().getIdLong())
			{
				if(Objects.requireNonNull(message.getMember()).getRoles().stream()
				          .noneMatch((r) -> ChocoBot.operatorRoles.contains(r.getId())))
				{
					sendTempMessage(channel, ChocoBot.errorMessage(
							"Du darfst dir nicht die Coins anderer Nutzer anzeigen lassen!"));
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
				PreparedStatement getCoins = connection.prepareStatement("SELECT coins, last_daily FROM coins WHERE uid=?"))
			{
				getCoins.setLong(1, uid);

				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
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
				}
			}

			LocalDateTime dateTime = LocalDateTime.ofInstant(lastDaily, ZoneId.systemDefault());
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(":moneybag: Coins :moneybag:");
			builder.setColor(ChocoBot.COLOR_COINS);
			if(foreign)
			{
				builder.addField(
						Objects.requireNonNull(ChocoBot.jda.getUserById(uid))
						       .getAsTag() + "s Coins",
						Integer.toString(coins),
						false);
			}
			else
			{
				builder.addField("Deine Coins", Integer.toString(coins), false);
				if(dateTime.getLong(ChronoField.EPOCH_DAY) <
						LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
				{
					builder.setFooter("\ud83d\udc8e Du kannst übrigens deinen täglichen Bonus einfordern! \ud83d\udc8e");
				}
			}

			sendTempMessage(channel, builder.build());
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

	public String getHelpText()
	{
		return "Zeige an, wie viele Coins du hast.";
	}

	@Override
	protected String getUsage()
	{
		return "%c : %h\n" +
				"%c <Nutzer> (nur Operatoren) : Zeige die Coins eines anderen Nutzers an.";
	}

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
