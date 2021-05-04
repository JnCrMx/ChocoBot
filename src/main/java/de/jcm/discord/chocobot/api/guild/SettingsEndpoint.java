package de.jcm.discord.chocobot.api.guild;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.ApiUser;
import de.jcm.discord.chocobot.api.GuildParam;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Path("/{guild}/guild/settings")
public class SettingsEndpoint
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public GuildSettings getSettings(@BeanParam GuildParam guildParam,
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

		return settings;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void setSettings(@BeanParam GuildParam guildParam,
	                        GuildSettings guildSettings,
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

		if(guild.getIdLong() != guildSettings.getGuildID())
			throw new NotFoundException();

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("REPLACE INTO guilds (id, prefix, command_channel, remind_channel, warning_channel, poll_channel) " +
				                                                              "VALUES(?, ?, ?, ?, ?, ?)"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, guildSettings.getPrefix());
			statement.setLong(3, guildSettings.getCommandChannelID());
			statement.setLong(4, guildSettings.getRemindChannelID());
			statement.setLong(5, guildSettings.getWarningChannelID());
			statement.setLong(6, guildSettings.getPollChannelID());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@DELETE
	public void deleteSettings(@BeanParam GuildParam guildParam,
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

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM guilds WHERE id = ?"))
		{
			statement.setLong(1, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@PUT
	@Path("/operators/{role}")
	public void addOperatorRole(@BeanParam GuildParam guildParam,
	                            @PathParam("role") long role,
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

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("INSERT INTO guild_operators (id, guild) VALUES(?, ?)"))
		{
			statement.setLong(1, role);
			statement.setLong(2, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@DELETE
	@Path("/operators/{role}")
	public void removeOperatorRole(@BeanParam GuildParam guildParam,
	                               @PathParam("role") long role,
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

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM guild_operators WHERE id = ? AND guild = ?"))
		{
			statement.setLong(1, role);
			statement.setLong(2, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@PUT
	@Path("/muted/{channel}")
	public void addMutedChannel(@BeanParam GuildParam guildParam,
	                            @PathParam("channel") long channel,
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

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("INSERT INTO guild_muted_channels (channel, guild) VALUES(?, ?)"))
		{
			statement.setLong(1, channel);
			statement.setLong(2, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@DELETE
	@Path("/muted/{channel}")
	public void removeMutedChannel(@BeanParam GuildParam guildParam,
	                               @PathParam("channel") long channel,
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

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM guild_muted_channels WHERE channel = ? AND guild = ?"))
		{
			statement.setLong(1, channel);
			statement.setLong(2, guild.getIdLong());

			if(statement.executeUpdate() != 1)
				throw new InternalServerErrorException();

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}
}
