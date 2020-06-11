package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/guild")
public class GuildEndpoint
{
	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	public GuildInfo info()
	{
		GuildInfo info = new GuildInfo();

		GuildChannel commandChannel = ChocoBot.jda.getGuildChannelById(ChocoBot.commandChannel);
		info.commandChannelId = commandChannel.getId();
		info.commandChannelName = commandChannel.getName();

		Guild guild = commandChannel.getGuild();
		info.guildId = guild.getId();
		info.guildName = guild.getName();
		info.iconUrl = guild.getIconUrl();

		info.owner = new GuildOwner();
		info.owner.tag = guild.getOwner().getUser().getAsTag();
		info.owner.nickname = guild.getOwner().getEffectiveName();
		info.owner.avatarUrl = guild.getOwner().getUser().getAvatarUrl();

		return info;
	}

	private static class GuildInfo
	{
		public String guildName;
		public String guildId;
		public String commandChannelId;
		public String commandChannelName;
		public String iconUrl;
		public GuildOwner owner;
	}

	private static class GuildOwner
	{
		public String tag;
		public String nickname;
		public String avatarUrl;
	}
}
