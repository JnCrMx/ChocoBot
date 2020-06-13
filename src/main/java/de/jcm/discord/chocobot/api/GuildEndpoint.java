package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.ChannelInfo;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Path("/{guild}/guild")
public class GuildEndpoint
{
	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	public GuildInfo getInfo(@BeanParam GuildParam guildParam,
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

		info.owner = new UserData();
		info.owner.tag = guild.getOwner().getUser().getAsTag();
		info.owner.nickname = guild.getOwner().getEffectiveName();
		info.owner.avatarUrl = guild.getOwner().getUser().getAvatarUrl();

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
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.getMemberById(user.getUserId());
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		return guild.getMembers().stream().map(m -> {
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
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.getMemberById(user.getUserId());
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		return guild.getChannels().stream()
		            .filter(c -> c.getType() == type)
		            .map(ChannelInfo::fromChannel)
		            .collect(Collectors.toList());
	}

	@GET
	@Path("/settings")
	@Produces(MediaType.APPLICATION_JSON)
	public GuildSettings getSettings(@BeanParam GuildParam guildParam,
	                                 @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.getMemberById(user.getUserId());
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		return settings;
	}

	@POST
	@Path("/settings")
	@Consumes(MediaType.APPLICATION_JSON)
	public void setSettings(@BeanParam GuildParam guildParam,
	                                 GuildSettings guildSettings,
	                                 @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.getMemberById(user.getUserId());
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

		if(guild.getIdLong() != guildSettings.getGuildID())
			throw new WebApplicationException(Response.Status.NOT_FOUND);

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("UPDATE guilds SET prefix = ?, " +
				                                                              "command_channel = ?, " +
				                                                              "remind_channel = ?, " +
				                                                              "warning_channel = ?, " +
				                                                              "poll_channel = ?" +
				                                                              " WHERE id = ?"))
		{
			statement.setString(1, guildSettings.getPrefix());
			statement.setLong(2, guildSettings.getCommandChannelID());
			statement.setLong(3, guildSettings.getRemindChannelID());
			statement.setLong(4, guildSettings.getWarningChannelID());
			statement.setLong(5, guildSettings.getPollChannelID());
			statement.setLong(6, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	private static class GuildInfo
	{
		public String guildName;
		public String guildId;
		public String commandChannelId;
		public String commandChannelName;
		public String iconUrl;
		public UserData owner;
	}

}
