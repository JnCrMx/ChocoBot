package de.jcm.discord.chocobot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils
{
	private static Logger logger;
	private static PreparedStatement insertUser;
	private static PreparedStatement getCoins;
	private static PreparedStatement changeCoins;

	private DatabaseUtils()
	{
	}

	public static void prepare()
	{
		logger = LoggerFactory.getLogger(DatabaseUtils.class);

		try
		{
			insertUser = ChocoBot.database.prepareStatement("INSERT INTO coins(uid, coins, last_daily, daily_streak)VALUES (?, 0, 0, 0)");
			getCoins = ChocoBot.database.prepareStatement("SELECT coins FROM coins WHERE uid=?");
			changeCoins = ChocoBot.database.prepareStatement("UPDATE coins SET coins=coins+? WHERE uid=?");
		}
		catch (SQLException var1)
		{
			var1.printStackTrace();
		}

	}

	public static void createEmptyUser(long uid)
	{
		try
		{
			insertUser.setLong(1, uid);
			insertUser.execute();
			logger.debug("Created new user entry for " + uid + ".");
		}
		catch (SQLException var3)
		{
			logger.error("Database error", var3);
		}

	}

	public static int getCoins(long uid)
	{
		try
		{
			getCoins.setLong(1, uid);
			ResultSet resultSet = getCoins.executeQuery();
			if (resultSet.next())
			{
				return resultSet.getInt("coins");
			}
			else
			{
				createEmptyUser(uid);
				return 0;
			}
		}
		catch (SQLException var3)
		{
			logger.error("Database error", var3);
			return -1;
		}
	}

	public static void changeCoins(long uid, int amount)
	{
		try
		{
			changeCoins.setInt(1, amount);
			changeCoins.setLong(2, uid);
			changeCoins.execute();
		}
		catch (SQLException var4)
		{
			logger.error("Database error", var4);
		}

	}
}
