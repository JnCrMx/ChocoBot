package de.jcm.discord.chocobot.plugin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Plugin
{
	protected final PluginStorage storage;

	public Plugin()
	{
		storage = new PluginStorage(getName());
	}

	public void registerCommand(Command command)
	{
		Command.registerPluginCommand(command, this);
	}

	public void registerListener(ListenerAdapter listener)
	{
		ChocoBot.jda.addEventListener(listener);
	}

	public void unregisterListener(ListenerAdapter listener)
	{
		ChocoBot.jda.removeEventListener(listener);
	}

	public Connection getDatabase()
	{
		try
		{
			return ChocoBot.getDatabase();
		}
		catch(SQLException throwables)
		{
			throw new RuntimeException(throwables);
		}
	}

	public final void tryCreateTable(String name, String paramsSQLite, String paramsMySQL)
	{
		if(!name.startsWith("plugin_"+getName()+"_"))
			throw new IllegalArgumentException("name needs to start with \""+"plugin_"+getName()+"_\"!");

		String sql = null;
		if("sqlite".equals(ChocoBot.dbType))
		{
			sql = String.format("CREATE TABLE \"%s\" (%s);", name, paramsSQLite);
		}
		else if("mysql".equals(ChocoBot.dbType))
		{
			sql = String.format("CREATE TABLE `%s` (%s);", name, paramsMySQL);
		}

		try(Connection connection = getDatabase();
		    Statement statement = connection.createStatement())
		{
			statement.execute(sql);
		}
		catch(SQLException ignored)
		{

		}
	}

	public final boolean isEnabled(long guild)
	{
		return storage.getGuildOption(guild, "enabled").orElse("false").equals("true");
	}

	public abstract void onLoaded();
	public abstract void onUnloaded();

	public abstract String getName();
}
