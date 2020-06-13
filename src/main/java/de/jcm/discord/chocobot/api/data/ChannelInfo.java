package de.jcm.discord.chocobot.api.data;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;

public class ChannelInfo
{
	public String id;
	public String name;
	public ChannelType type;

	public static ChannelInfo fromChannel(GuildChannel channel)
	{
		ChannelInfo info = new ChannelInfo();

		info.id = channel.getId();
		info.name = channel.getName();
		info.type = channel.getType();

		return info;
	}
}
