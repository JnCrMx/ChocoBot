package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.retrieveMemberById(user.getUserId()).complete();

		UserData userData = new UserData();
		userData.userId = member.getId();
		userData.tag = member.getUser().getAsTag();
		userData.nickname = member.getEffectiveName();
		userData.avatarUrl = member.getUser().getAvatarUrl();
		userData.coins = DatabaseUtils.getCoins(user.getUserId(), guild.getIdLong());
		userData.onlineStatus = member.getOnlineStatus();
		userData.timeJoined = member.getTimeJoined().toEpochSecond();
		userData.role = member.getRoles().isEmpty() ? null : member.getRoles().get(0).getName();
		userData.roleColor = member.getRoles().isEmpty() ? 0 : member.getRoles().get(0).getColorRaw();
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

	@POST
	@Path("/{user}/coins")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void setCoins(@BeanParam GuildParam guildParam,
	                     @PathParam("user") long userId,
	                     @FormDataParam("coins") int coins,
	                     @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.retrieveMemberById(user.getUserId()).onErrorMap(t->null).complete();
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new ForbiddenException();

		DatabaseUtils.setCoins(userId, guild.getIdLong(), coins);
	}
}
