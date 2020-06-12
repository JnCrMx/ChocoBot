package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/{guild}/user")
public class UserEndpoint
{
	@GET
	@Path("/self")
	@Produces(MediaType.APPLICATION_JSON)
	public UserData self(@BeanParam GuildParam guildParam,
	                     @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		Guild guild = guildParam.toGuild();

		Member member = guild.getMemberById(user.getUserId());

		UserData userData = new UserData();
		userData.userId = member.getId();
		userData.tag = member.getUser().getAsTag();
		userData.nickname = member.getEffectiveName();
		userData.avatarUrl = member.getUser().getAvatarUrl();
		userData.coins = DatabaseUtils.getCoins(user.getUserId());
		userData.onlineStatus = member.getOnlineStatus();
		userData.timeJoined = member.getTimeJoined().toEpochSecond();
		userData.role = member.getRoles().get(0).getName();
		userData.roleColor = member.getRoles().get(0).getColorRaw();

		return userData;
	}

	private static class UserData
	{
		public String userId;
		public String tag;
		public String nickname;
		public String avatarUrl;
		public int coins;
		public OnlineStatus onlineStatus;
		public long timeJoined;
		public String role;
		public int roleColor;
	}
}
