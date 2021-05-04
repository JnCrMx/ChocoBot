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
import java.util.HashMap;
import java.util.Map;

@Path("/{guild}/guild/settings/language_overrides")
public class LanguageOverridesEndpoint
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> getLanguageOverrides(@BeanParam GuildParam guildParam,
	                                                @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.retrieveMemberById(user.getUserId()).onErrorMap(t -> null).complete();
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new ForbiddenException();

		assert settings != null;
		return settings.getLanguageOverrides();
	}

	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> getAllTranslations(@BeanParam GuildParam guildParam,
	                                              @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.retrieveMemberById(user.getUserId()).onErrorMap(t -> null).complete();
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new ForbiddenException();

		assert settings != null;

		Map<String, String> map = new HashMap<>(ChocoBot.languages.get(settings.getLanguage()));
		map.putAll(settings.getLanguageOverrides());

		return map;
	}

	@PUT
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void addLanguageOverride(@BeanParam GuildParam guildParam,
	                                @PathParam("key") String key,
	                                @FormParam("value") String value,
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
		    PreparedStatement statement = connection.prepareStatement("INSERT INTO guild_language_overrides (guild, `key`, value) VALUES (?, ?, ?)"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, key);
			statement.setString(3, value);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot add language override");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot add language override", throwables);
		}
	}

	@PATCH
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void updateLanguageOverride(@BeanParam GuildParam guildParam,
	                                   @PathParam("key") String oldKey,
	                                   @FormParam("key") String key,
	                                   @FormParam("value") String value,
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

		if(key == null || key.isBlank())
			key = oldKey;

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("UPDATE guild_language_overrides SET `key` = ?, value = ? WHERE guild = ? AND `key` = ?"))
		{
			statement.setString(1, key);
			statement.setString(2, value);
			statement.setLong(3, guild.getIdLong());
			statement.setString(4, oldKey);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot patch language override");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot update language override", throwables);
		}
	}

	@DELETE
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void removeLanguageOverride(@BeanParam GuildParam guildParam,
	                                   @PathParam("key") String key,
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
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM guild_language_overrides WHERE guild = ? AND `key` = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, key);
			if(statement.executeUpdate() != 1)
			{
				throw new InternalServerErrorException("cannot remove language override");
			}

			DatabaseUtils.deleteCached(guild.getIdLong());
		}
		catch(SQLException throwables)
		{
			throw new InternalServerErrorException("cannot remove language override", throwables);
		}
	}
}
