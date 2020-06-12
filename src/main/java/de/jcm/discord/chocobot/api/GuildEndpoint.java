package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/{guild}/guild")
public class GuildEndpoint
{
	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	public GuildInfo info(@BeanParam GuildParam guildParam,
	                      @Context ContainerRequestContext request)
	{
		if(!guildParam.checkAccess((ApiUser) request.getProperty("user")))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		GuildSettings settings = DatabaseUtils.getSettings(guildParam.getGuildId());
		GuildInfo info = new GuildInfo();

		if(settings != null)
		{
			GuildChannel commandChannel = settings.getCommandChannel();
			info.commandChannelId = commandChannel.getId();
			info.commandChannelName = commandChannel.getName();
		}

		Guild guild = guildParam.toGuild();
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
