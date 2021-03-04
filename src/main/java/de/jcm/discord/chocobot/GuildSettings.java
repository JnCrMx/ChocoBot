package de.jcm.discord.chocobot;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.jcm.discord.chocobot.api.data.ChannelInfo;
import de.jcm.discord.chocobot.api.data.RoleInfo;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class GuildSettings
{
	public static  final Logger LOGGER = LoggerFactory.getLogger(GuildSettings.class);

	private long guild;
	private String prefix;
	private long commandChannel;
	private long remindChannel;
	private long warningChannel;
	private long pollChannel;
	private String language;

	private List<Long> operators = new ArrayList<>();
	private List<Long> mutedChannels = new ArrayList<>();
	private Map<String, String> languageOverrides = new HashMap<>();

	public GuildSettings()
	{

	}

	public GuildSettings(ResultSet resultSet) throws SQLException
	{
		this.guild = resultSet.getLong("id");
		this.prefix = resultSet.getString("prefix");
		this.commandChannel = resultSet.getLong("command_channel");
		this.remindChannel = resultSet.getLong("remind_channel");
		this.warningChannel = resultSet.getLong("warning_channel");
		this.pollChannel = resultSet.getLong("poll_channel");
		this.language = resultSet.getString("language");
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

	void readLanguageOverrides(ResultSet resultSet) throws SQLException
	{
		languageOverrides.clear();
		while(resultSet.next())
		{
			languageOverrides.put(resultSet.getString("key"),
			                      resultSet.getString("value"));
		}
	}

	@JsonIgnore
	public long getGuildID()
	{
		return guild;
	}

	@JsonGetter("guild")
	public String getGuildIDString()
	{
		return Long.toString(guild);
	}

	@JsonIgnore
	public Guild getGuild()
	{
		return ChocoBot.jda.getGuildById(guild);
	}

	@JsonGetter("prefix")
	public String getPrefix()
	{
		return prefix;
	}

	@JsonIgnore
	public long getCommandChannelID()
	{
		return commandChannel;
	}

	@JsonGetter("commandChannel")
	public ChannelInfo getCommandChannelInfo()
	{
		return ChannelInfo.fromChannel(getCommandChannel());
	}

	@JsonIgnore
	public TextChannel getCommandChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, commandChannel);
	}

	@JsonIgnore
	public long getRemindChannelID()
	{
		return remindChannel;
	}

	@JsonGetter("remindChannel")
	public ChannelInfo getRemindChannelInfo()
	{
		return ChannelInfo.fromChannel(getRemindChannel());
	}

	@JsonIgnore
	public TextChannel getRemindChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, remindChannel);
	}

	@JsonIgnore
	public long getWarningChannelID()
	{
		return warningChannel;
	}

	@JsonGetter("warningChannel")
	public ChannelInfo getWarningChannelInfo()
	{
		return ChannelInfo.fromChannel(getWarningChannel());
	}

	@JsonIgnore
	public TextChannel getWarningChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, warningChannel);
	}

	@JsonIgnore
	public long getPollChannelID()
	{
		return pollChannel;
	}

	@JsonGetter("pollChannel")
	public ChannelInfo getPollChannelInfo()
	{
		return ChannelInfo.fromChannel(getPollChannel());
	}

	@JsonIgnore
	public TextChannel getPollChannel()
	{
		return (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, pollChannel);
	}

	@JsonIgnore
	public boolean isOperator(Member member)
	{
		if(member.isOwner())
			return true;
		if(operators.contains(member.getIdLong())) // beta, not changeable in Web UI
			return true;
		return member.getRoles().stream().anyMatch(r->operators.contains(r.getIdLong()));
	}

	@JsonIgnore
	public boolean isChannelMuted(TextChannel channel)
	{
		return mutedChannels.contains(channel.getIdLong());
	}

	@JsonIgnore
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

	@JsonGetter("operators")
	public List<RoleInfo> getOperators()
	{
		Guild guild = getGuild();
		return operators.stream()
		                .map(guild::getRoleById)
		                .filter(Objects::nonNull)
		                .map(RoleInfo::fromRole)
		                .collect(Collectors.toList());
	}

	@JsonGetter("mutedChannels")
	public List<ChannelInfo> getMutedChannels()
	{
		return mutedChannels.stream()
		                    .map(ChocoBot.jda::getGuildChannelById)
		                    .filter(Objects::nonNull)
		                    .map(ChannelInfo::fromChannel)
		                    .collect(Collectors.toList());
	}

	@JsonSetter("guild")
	public void setGuild(String guild)
	{
		this.guild = Long.parseLong(guild);
	}

	@JsonSetter("prefix")
	public void setPrefix(String prefix)
	{
		this.prefix = prefix;
	}

	@JsonSetter("commandChannel")
	public void setCommandChannel(ChannelInfo commandChannel)
	{
		this.commandChannel = Long.parseLong(commandChannel.id);
	}

	@JsonSetter("remindChannel")
	public void setRemindChannel(ChannelInfo remindChannel)
	{
		this.remindChannel = Long.parseLong(remindChannel.id);
	}

	@JsonSetter("warningChannel")
	public void setWarningChannel(ChannelInfo warningChannel)
	{
		this.warningChannel = Long.parseLong(warningChannel.id);
	}

	@JsonSetter("pollChannel")
	public void setPollChannel(ChannelInfo pollChannel)
	{
		this.pollChannel = Long.parseLong(pollChannel.id);
	}

	@JsonSetter("operators")
	public void setOperators(List<RoleInfo> operators)
	{
		this.operators = operators.stream()
		                          .map(r->r.id)
		                          .map(Long::parseLong)
		                          .collect(Collectors.toList());
	}

	@JsonSetter("mutedChannels")
	public void setMutedChannels(List<ChannelInfo> mutedChannels)
	{
		this.mutedChannels = mutedChannels.stream()
		                                  .map(c->c.id)
		                                  .map(Long::parseLong)
		                                  .collect(Collectors.toList());
	}

	private String getLanguageEntry(String key)
	{
		String translation = languageOverrides.getOrDefault(key, ChocoBot.languages.get(language).get(key));
		if(translation == null)
		{
			LOGGER.warn("Cannot find translation for key \"{}\"!", key);
		}

		return translation;
	}

	public String translate(String key, Object...args)
	{
		String translation = getLanguageEntry(key);
		translation = translation.replace("\\n", "\n");

		int keyStart;
		while((keyStart = translation.indexOf("@{"))!=-1)
		{
			int keyEnd = translation.indexOf('}', keyStart);

			String newKey = translation.substring(keyStart+2, keyEnd);

			String before = translation.substring(0, keyStart);
			String after = translation.substring(keyEnd+1);

			translation = before + translate(newKey, args) + after;
		}

		return String.format(translation, args);
	}
}
