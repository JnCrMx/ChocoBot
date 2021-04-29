package de.jcm.discord.chocobot.plugin;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.jcm.discord.chocobot.ChocoBot;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PluginStorage
{
	private final String name;
	private final LoadingCache<String, String> globalCache =
			Caffeine.newBuilder()
			        .maximumSize(1_000)
			        .expireAfterWrite(10, TimeUnit.MINUTES)
					.build(this::getGlobalOption0);
	private final LoadingCache<Pair<Long, String>, String> guildCache =
			Caffeine.newBuilder()
			        .maximumSize(10_000)
			        .expireAfterWrite(10, TimeUnit.MINUTES)
					.build(this::getGuildOption0);


	public PluginStorage(String name)
	{
		this.name = name;
	}

	private String getGlobalOption0(String key)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT value FROM plugin_config WHERE name=? AND key_=?"))
		{
			statement.setString(1, name);
			statement.setString(2, key);
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				return resultSet.getString("value");
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return null;
	}

	private String getGuildOption0(Pair<Long, String> p)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT value FROM plugin_guild_config WHERE name=? AND guild=? AND key_=?"))
		{
			statement.setString(1, name);
			statement.setLong(2, p.getLeft());
			statement.setString(3, p.getRight());
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				return resultSet.getString("value");
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return null;
	}

	public Optional<String> getGlobalOption(String key)
	{
		return Optional.ofNullable(globalCache.get(key));
	}

	public Optional<String> getGuildOption(long guild, String key)
	{
		return Optional.ofNullable(guildCache.get(new ImmutablePair<>(guild, key)));
	}
}
