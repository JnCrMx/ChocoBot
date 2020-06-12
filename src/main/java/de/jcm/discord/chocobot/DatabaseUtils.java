package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.entities.Guild;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class DatabaseUtils
{
	private static Logger logger;

	// use caching to prevent database query for each guild message
	private static final long CACHE_DURATION = 60*60*1000;
	private static HashMap<Long, ImmutablePair<GuildSettings, Long>> settingsCache = new HashMap<>();

	private DatabaseUtils()
	{
	}

	public static void prepare()
	{
		logger = LoggerFactory.getLogger(DatabaseUtils.class);
	}

	public static void createEmptyUser(Connection connection, long uid, long guild)
	{
		try
		{
			try(PreparedStatement insertUser = connection.prepareStatement("INSERT INTO coins(uid, guild, coins, last_daily, daily_streak)VALUES (?, ?, 0, 0, 0)"))
			{
				insertUser.setLong(1, uid);
				insertUser.setLong(2, guild);
				insertUser.execute();
			}
			logger.debug("Created new user entry for " + uid + ".");
		}
		catch (SQLException var3)
		{
			logger.error("Database error", var3);
		}

	}

	public static int getCoins(long uid, long guild)
	{
		try
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement getCoins = connection.prepareStatement("SELECT coins FROM coins WHERE uid=? AND guild=?"))
			{
				getCoins.setLong(1, uid);
				getCoins.setLong(2, guild);
				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
					{
						return resultSet.getInt("coins");
					}
					else
					{
						createEmptyUser(connection, uid, guild);
						return 0;
					}
				}
			}
		}
		catch (SQLException var3)
		{
			logger.error("Database error", var3);
			return -1;
		}
	}

	public static void changeCoins(long uid, long guild, int amount)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement changeCoins = connection.prepareStatement("UPDATE coins SET coins=coins+? WHERE uid=? AND guild=?"))
		{
			changeCoins.setInt(1, amount);
			changeCoins.setLong(2, uid);
			changeCoins.setLong(3, guild);
			changeCoins.execute();
		}
		catch (SQLException var4)
		{
			logger.error("Database error", var4);
		}
	}

	public static GuildSettings getSettings(long guild)
	{
		if(settingsCache.containsKey(guild))
		{
			ImmutablePair<GuildSettings, Long> cached = settingsCache.get(guild);
			if(cached.getRight() > System.currentTimeMillis() + CACHE_DURATION)
			{
				settingsCache.remove(guild);
			}
			else
			{
				return cached.getLeft();
			}
		}

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement get = connection.prepareStatement("SELECT * FROM guilds WHERE id = ?");
		    PreparedStatement getOperators = connection.prepareStatement("SELECT * FROM guild_operators WHERE guild = ?");
		    PreparedStatement getMuted = connection.prepareStatement("SELECT * FROM guild_muted_channels WHERE guild = ?"))
		{
			get.setLong(1, guild);
			getOperators.setLong(1, guild);
			getMuted.setLong(1, guild);

			try(ResultSet resultSet = get.executeQuery())
			{
				if(resultSet.next())
				{
					GuildSettings settings = new GuildSettings(resultSet);
					settings.readOperators(getOperators.executeQuery());
					settings.readMutedChannels(getMuted.executeQuery());

					settingsCache.put(guild, new ImmutablePair<>(settings, System.currentTimeMillis()));

					return settings;
				}
			}
		}
		catch (SQLException var4)
		{
			logger.error("Database error", var4);
		}
		return null;
	}

	public static GuildSettings getSettings(Guild guild)
	{
		return getSettings(guild.getIdLong());
	}
}
