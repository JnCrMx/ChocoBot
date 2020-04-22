package de.jcm.discord.chocobot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils
{
	private static Logger logger;

	private DatabaseUtils()
	{
	}

	public static void prepare()
	{
		logger = LoggerFactory.getLogger(DatabaseUtils.class);
	}

	public static void createEmptyUser(long uid)
	{
		try
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement insertUser = connection.prepareStatement("INSERT INTO coins(uid, coins, last_daily, daily_streak)VALUES (?, 0, 0, 0)"))
			{
				insertUser.setLong(1, uid);
				insertUser.execute();
			}
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
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement getCoins = connection.prepareStatement("SELECT coins FROM coins WHERE uid=?"))
			{
				getCoins.setLong(1, uid);
				try(ResultSet resultSet = getCoins.executeQuery())
				{
					if(resultSet.next())
					{
						return resultSet.getInt("coins");
					}
					else
					{
						createEmptyUser(uid);
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

	public static void changeCoins(long uid, int amount)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement changeCoins = connection.prepareStatement("UPDATE coins SET coins=coins+? WHERE uid=?"))
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
