package de.jcm.discord.chocobot.command;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandCustom extends Command
{
	public CommandCustom()
	{

	}

	public CommandCustom(String keyword, String message)
	{
		this.keyword = keyword;
		this.message = message;
	}

	private String keyword;
	private String message;

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		channel.sendMessage(this.message).queue();
		return true;
	}

	@Override
	@JsonGetter("keyword")
	protected @NotNull String getKeyword()
	{
		return keyword;
	}

	@JsonSetter("keyword")
	public void setKeyword(String keyword)
	{
		this.keyword = keyword;
	}

	@JsonGetter("message")
	public String getMessage()
	{
		return message;
	}

	@JsonSetter("message")
	public void setMessage(String message)
	{
		this.message = message;
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return null;
	}

	@Override
	protected @Nullable String getUsage()
	{
		return null;
	}

	public static CommandCustom forGuild(long guildId, String keyword)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT message FROM custom_commands WHERE guild = ? AND keyword = ?"))
		{
			statement.setLong(1, guildId);
			statement.setString(2, keyword);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				String message = resultSet.getString("message");
				return new CommandCustom(keyword, message);
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return null;
	}
}
