package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseUtils
{
	private static Logger logger;

	// use caching to prevent database query for each guild message
	private static final long CACHE_DURATION = 60*60*1000;
	private static final HashMap<Long, ImmutablePair<GuildSettings, Long>> settingsCache = new HashMap<>();

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

	public static int getCoins(Connection connection, long uid, long guild)
	{
		try
		{
			try(PreparedStatement getCoins = connection.prepareStatement("SELECT coins FROM coins WHERE uid=? AND guild=?"))
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

		if(amount > 0)
		{
			int newCoins = getCoins(uid, guild);
			try(Connection connection = ChocoBot.getDatabase())
			{
				DatabaseUtils.updateStat(connection, uid, guild, "max_coins", newCoins);
			}
			catch(SQLException var4)
			{
				logger.error("Database error", var4);
			}
		}
	}

	public static void changeCoins(Connection connection, long uid, long guild, int amount)
	{
		try(PreparedStatement changeCoins = connection.prepareStatement("UPDATE coins SET coins=coins+? WHERE uid=? AND guild=?"))
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

		if(amount > 0)
		{
			int newCoins = getCoins(connection, uid, guild);
			try
			{
				DatabaseUtils.updateStat(connection, uid, guild, "max_coins", newCoins);
			}
			catch(SQLException var4)
			{
				logger.error("Database error", var4);
			}
		}
	}

	public static void setCoins(long uid, long guild, int coins)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement changeCoins = connection.prepareStatement("UPDATE coins SET coins=? WHERE uid=? AND guild=?"))
		{
			changeCoins.setInt(1, coins);
			changeCoins.setLong(2, uid);
			changeCoins.setLong(3, guild);
			changeCoins.execute();

			DatabaseUtils.updateStat(connection, uid, guild, "max_coins", coins);
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
		    PreparedStatement getMuted = connection.prepareStatement("SELECT * FROM guild_muted_channels WHERE guild = ?");
		    PreparedStatement getLanguageOverrides = connection.prepareStatement("SELECT * FROM guild_language_overrides WHERE guild = ?"))
		{
			get.setLong(1, guild);
			getOperators.setLong(1, guild);
			getMuted.setLong(1, guild);
			getLanguageOverrides.setLong(1, guild);

			try(ResultSet resultSet = get.executeQuery())
			{
				if(resultSet.next())
				{
					GuildSettings settings = new GuildSettings(resultSet);
					settings.readOperators(getOperators.executeQuery());
					settings.readMutedChannels(getMuted.executeQuery());
					settings.readLanguageOverrides(getLanguageOverrides.executeQuery());

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

	public static GuildSettings getUserSettings(User user)
	{
		List<Guild> guilds = ChocoBot.jda.getGuilds();
		for(Guild guild : guilds)
		{
			if(guild.retrieveMember(user).onErrorMap(e->null).complete() != null)
			{
				return getSettings(guild);
			}
		}
		return getSettings(guilds.get(0));
	}

	public static void deleteCached(long guild)
	{
		settingsCache.remove(guild);
	}

	public static int getStat(Connection connection, long uid, long guild, String stat) throws SQLException
	{
		try(PreparedStatement statement = connection.prepareStatement("SELECT value FROM user_stats WHERE uid = ? AND guild = ? AND stat = ?"))
		{
			statement.setLong(1, uid);
			statement.setLong(2, guild);
			statement.setString(3, stat);
			try(ResultSet resultSet = statement.executeQuery())
			{
				if(resultSet.next())
				{
					return resultSet.getInt("value");
				}
				else
				{
					setStat(connection, uid, guild, stat, 0);
					return 0;
				}
			}
		}
	}

	public static void setStat(Connection connection, long uid, long guild, String stat, int value) throws SQLException
	{
		try(PreparedStatement statement = connection.prepareStatement("REPLACE INTO user_stats(uid, guild, stat, value) VALUES(?, ?, ?, ?)"))
		{
			statement.setLong(1, uid);
			statement.setLong(2, guild);
			statement.setString(3, stat);
			statement.setInt(4, value);

			statement.executeUpdate();
		}
	}

	public static void updateStat(Connection connection, long uid, long guild, String stat, int value) throws SQLException
	{
		int oldValue = getStat(connection, uid, guild, stat);
		if(value > oldValue)
		{
			setStat(connection, uid, guild, stat, value);
		}
	}

	public static void increaseStat(Connection connection, long uid, long guild, String stat, int amount) throws SQLException
	{
		int oldValue = getStat(connection, uid, guild, stat);
		setStat(connection, uid, guild, stat, oldValue + amount);
	}

	public static Map<String, Integer> getStats(Connection connection, long uid, long guild) throws SQLException
	{
		try(PreparedStatement statement = connection.prepareStatement("SELECT stat, value FROM user_stats WHERE uid = ? AND guild = ?"))
		{
			statement.setLong(1, uid);
			statement.setLong(2, guild);
			try(ResultSet resultSet = statement.executeQuery())
			{
				Map<String, Integer> map = new HashMap<>();
				while(resultSet.next())
				{
					map.put(resultSet.getString("stat"), resultSet.getInt("value"));
				}
				return map;
			}
		}
	}

	public static Map<String, Map<Long, Integer>> getStats(Connection connection, long guild) throws SQLException
	{
		try(PreparedStatement statement = connection.prepareStatement("SELECT uid, stat, value FROM user_stats WHERE guild = ?"))
		{
			statement.setLong(1, guild);
			try(ResultSet resultSet = statement.executeQuery())
			{
				Map<String, Map<Long, Integer>> map = new HashMap<>();
				while(resultSet.next())
				{
					Map<Long, Integer> submap = map.computeIfAbsent(resultSet.getString("stat"), s->new HashMap<>());
					submap.put(resultSet.getLong("uid"), resultSet.getInt("value"));
				}
				return map;
			}
		}
	}
}
