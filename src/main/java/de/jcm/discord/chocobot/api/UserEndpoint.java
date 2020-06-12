package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
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
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.getMemberById(user.getUserId());

		UserData userData = new UserData();
		userData.userId = member.getId();
		userData.tag = member.getUser().getAsTag();
		userData.nickname = member.getEffectiveName();
		userData.avatarUrl = member.getUser().getAvatarUrl();
		userData.coins = DatabaseUtils.getCoins(user.getUserId(), guild.getIdLong());
		userData.onlineStatus = member.getOnlineStatus();
		userData.timeJoined = member.getTimeJoined().toEpochSecond();
		userData.role = member.getRoles().get(0).getName();
		userData.roleColor = member.getRoles().get(0).getColorRaw();
		if(settings == null)
		{
			userData.operator = member.isOwner();
		}
		else
		{
			userData.operator = settings.isOperator(member);
		}

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
		public boolean operator;
	}
}
