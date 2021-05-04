package de.jcm.discord.chocobot.api.guild;

import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.ApiUser;
import de.jcm.discord.chocobot.api.GuildParam;
import de.jcm.discord.chocobot.api.data.ChannelInfo;
import de.jcm.discord.chocobot.api.data.GuildInfo;
import de.jcm.discord.chocobot.api.data.RoleInfo;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/{guild}/guild")
public class InfoEndpoint
{
	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	public GuildInfo getInfo(@BeanParam GuildParam guildParam,
	                         @Context ContainerRequestContext request)
	{
		if(!guildParam.checkAccess((ApiUser) request.getProperty("user")))
			throw new ForbiddenException();

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

		info.owner = new UserData();
		if(guild.getOwner() != null)
		{
			info.owner.tag = guild.getOwner().getUser().getAsTag();
			info.owner.nickname = guild.getOwner().getEffectiveName();
			info.owner.avatarUrl = guild.getOwner().getUser().getAvatarUrl();
		}

		return info;
	}

	@GET
	@Path("/members")
	@Produces(MediaType.APPLICATION_JSON)
	public List<UserData> getMembers(@BeanParam GuildParam guildParam,
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

		return guild.getMembers().stream().map(m -> { //TODO: Fix
			UserData userData = new UserData();

			userData.userId = m.getId();
			userData.tag = m.getUser().getAsTag();
			userData.nickname = m.getEffectiveName();
			userData.avatarUrl = m.getUser().getAvatarUrl();
			userData.coins = DatabaseUtils.getCoins(m.getIdLong(), guild.getIdLong());
			userData.onlineStatus = m.getOnlineStatus();
			userData.timeJoined = m.getTimeJoined().toEpochSecond();
			userData.role = m.getRoles().isEmpty() ? null : m.getRoles().get(0).getName();
			userData.roleColor = m.getRoles().isEmpty() ? 0 : m.getRoles().get(0).getColorRaw();
			if(settings == null)
			{
				userData.operator = m.isOwner();
			}
			else
			{
				userData.operator = settings.isOperator(m);
			}

			return userData;
		}).collect(Collectors.toList());
	}

	@GET
	@Path("/channels")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ChannelInfo> getChannels(@BeanParam GuildParam guildParam,
	                                     @QueryParam("type") @DefaultValue("TEXT") ChannelType type,
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

		return guild.getChannels().stream()
		            .filter(c -> c.getType() == type)
		            .map(ChannelInfo::fromChannel)
		            .collect(Collectors.toList());
	}

	@GET
	@Path("/roles")
	@Produces(MediaType.APPLICATION_JSON)
	public List<RoleInfo> getRoles(@BeanParam GuildParam guildParam,
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

		return guild.getRoles().stream()
		            .map(RoleInfo::fromRole)
		            .collect(Collectors.toList());
	}
}
