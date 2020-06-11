package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/user")
public class UserEndpoint
{
	@Context Configuration configuration;

	@GET
	@Path("/self")
	@Produces(MediaType.APPLICATION_JSON)
	public UserData self(@Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		Guild guild = (Guild) configuration.getProperty("guild");

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
