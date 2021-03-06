package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.ChannelInfo;
import de.jcm.discord.chocobot.api.data.RoleInfo;
import de.jcm.discord.chocobot.api.data.UserData;
import de.jcm.discord.chocobot.command.CommandCustom;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	@GET
	@Path("/settings")
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
	@Path("/settings")
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
	@Path("/settings")
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
	@Path("/settings/operators/{role}")
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
	@Path("/settings/operators/{role}")
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
	@Path("/settings/muted/{channel}")
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
	@Path("/settings/muted/{channel}")
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

	@GET
	@Path("/settings/commands")
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
	@Path("/settings/commands/{keyword}")
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
	@Path("/settings/commands/{keyword}")
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
	@Path("/settings/commands/{keyword}")
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

	@GET
	@Path("/settings/language_overrides")
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
	@Path("/settings/language_overrides/all")
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
	@Path("/settings/language_overrides/{key}")
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
	@Path("/settings/language_overrides/{key}")
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
	@Path("/settings/language_overrides/{key}")
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
