package de.jcm.discord.chocobot.command;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

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

	private static String replaceMember(String string, String mention, Member member)
	{
		string = string.replace("$"+mention+"."+"id", member.getId());
		string = string.replace("$"+mention+"."+"name", member.getUser().getName());
		string = string.replace("$"+mention+"."+"displayname", member.getEffectiveName());
		string = string.replace("$"+mention+"."+"nickname", Objects.requireNonNullElse(member.getNickname(), ""));
		string = string.replace("$"+mention+"."+"ping", member.getAsMention());
		string = string.replace("$"+mention+"."+"avatar", member.getUser().getEffectiveAvatarUrl());
		string = string.replace("$"+mention, member.getEffectiveName());

		return string;
	}

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		String string = this.message;

		string = replaceMember(string, "self", Objects.requireNonNull(message.getMember()));
		string = replaceMember(string, "sender", Objects.requireNonNull(message.getMember()));
		string = replaceMember(string, "0", Objects.requireNonNull(message.getMember()));

		List<Member> mentions = message.getMentionedMembers();
		for(int i=0; i<mentions.size(); i++)
		{
			string = replaceMember(string, Integer.toString(i+1), mentions.get(i));
		}

		for(int i=0; i<args.length; i++)
		{
			string = string.replace("$"+(i + 1), args[i]);
		}

		string = string.replaceAll("\\$[\\w.]*", "");

		channel.sendMessage(string).queue();
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
	protected @Nullable String getHelpText(GuildSettings settings)
	{
		return null;
	}

	@Override
	protected @Nullable String getUsage(GuildSettings settings)
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
