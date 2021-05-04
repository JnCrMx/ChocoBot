package de.jcm.discord.chocobot.api.guild;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.ApiUser;
import de.jcm.discord.chocobot.api.GuildParam;
import de.jcm.discord.chocobot.command.CommandCustom;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/{guild}/guild/settings/commands")
public class CustomCommandsEndpoint
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<CommandCustom> getCustomCommands(@BeanParam GuildParam guildParam,
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

		List<CommandCustom> customCommands = new ArrayList<>();
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT keyword, message FROM custom_commands WHERE guild = ?"))
		{
			statement.setLong(1, guild.getIdLong());

			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next())
			{
				String keyword = resultSet.getString("keyword");
				String message = resultSet.getString("message");
				customCommands.add(new CommandCustom(keyword, message));
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		return customCommands;
	}

	@PUT
	@Path("/{keyword}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void addCustomCommand(@BeanParam GuildParam guildParam,
	                             @PathParam("keyword") String keyword,
	                             @FormParam("message") String message,
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
		    PreparedStatement statement = connection.prepareStatement("INSERT INTO custom_commands (guild, keyword, message) VALUES (?, ?, ?)"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, keyword);
			statement.setString(3, message);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot add command");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot add command", throwables);
		}
	}

	@PATCH
	@Path("/{keyword}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void updateCustomCommand(@BeanParam GuildParam guildParam,
	                                @PathParam("keyword") String oldKeyword,
	                                @FormParam("keyword") String keyword,
	                                @FormParam("message") String message,
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

		if(keyword == null || keyword.isBlank())
			keyword = oldKeyword;

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("UPDATE custom_commands set keyword = ?, message = ? WHERE guild = ? AND keyword = ?"))
		{
			statement.setString(1, keyword);
			statement.setString(2, message);
			statement.setLong(3, guild.getIdLong());
			statement.setString(4, oldKeyword);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot add command");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot add command", throwables);
		}
	}

	@DELETE
	@Path("/{keyword}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void removeCustomCommand(@BeanParam GuildParam guildParam,
	                                @PathParam("keyword") String keyword,
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
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM custom_commands WHERE guild = ? AND keyword = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, keyword);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot add command");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot add command", throwables);
		}
	}
}
