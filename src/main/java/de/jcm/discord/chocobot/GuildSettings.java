package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.entities.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuildSettings
{
	private final long guild;
	private final String prefix;
	private final long commandChannel;
	private final long remindChannel;
	private final long warningChannel;
	private final long pollChannel;
	private final List<Long> operators = new ArrayList<>();
	private final List<Long> mutedChannels = new ArrayList<>();

	public GuildSettings(ResultSet resultSet) throws SQLException
	{
		this.guild = resultSet.getLong("id");
		this.prefix = resultSet.getString("prefix");
		this.commandChannel = resultSet.getLong("command_channel");
		this.remindChannel = resultSet.getLong("remind_channel");
		this.warningChannel = resultSet.getLong("warning_channel");
		this.pollChannel = resultSet.getLong("poll_channel");
	}

	void readOperators(ResultSet resultSet) throws SQLException
	{
		operators.clear();
		while(resultSet.next())
		{
			operators.add(resultSet.getLong("id"));
		}
	}

	void readMutedChannels(ResultSet resultSet) throws SQLException
	{
		mutedChannels.clear();
		while(resultSet.next())
		{
			mutedChannels.add(resultSet.getLong("channel"));
		}
	}

	public long getGuildID()
	{
		return guild;
	}

	public Guild getGuild()
	{
		return ChocoBot.jda.getGuildById(guild);
	}

	public String getPrefix()
	{
		return prefix;
	}

	public long getCommandChannelID()
	{
		return commandChannel;
	}

	public TextChannel getCommandChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, commandChannel);
	}

	public long getRemindChannelID()
	{
		return remindChannel;
	}

	public TextChannel getRemindChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, remindChannel);
	}

	public long getWarningChannelID()
	{
		return warningChannel;
	}

	public TextChannel getWarningChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, warningChannel);
	}

	public long getPollChannelID()
	{
		return pollChannel;
	}

	public TextChannel getPollChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, pollChannel);
	}

	public boolean isOperator(Member member)
	{
		if(member.isOwner())
			return true;
		if(operators.contains(member.getIdLong()))
			return true;
		return member.getRoles().stream().anyMatch(r->operators.contains(r.getIdLong()));
	}

	public boolean isChannelMuted(TextChannel channel)
	{
		return mutedChannels.contains(channel.getIdLong());
	}

	@Override
	public String toString()
	{
		return "GuildSettings{" +
				"guild=" + guild +
				", prefix='" + prefix + '\'' +
				", commandChannel=" + commandChannel +
				", remindChannel=" + remindChannel +
				", warningChannel=" + warningChannel +
				", operators=" + operators +
				", mutedChannels=" + mutedChannels +
				'}';
	}
}
