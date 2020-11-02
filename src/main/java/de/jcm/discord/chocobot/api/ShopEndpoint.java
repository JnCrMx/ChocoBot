package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.RoleInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/{guild}/shop")
public class ShopEndpoint
{
	@GET
	@Path("/items")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ShopEntry> list(@BeanParam GuildParam guildParam,
	                            @Context ContainerRequestContext request)
	{
		if(!guildParam.checkAccess((ApiUser) request.getProperty("user")))
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT role, alias, description, cost FROM shop_roles WHERE guild = ?"))
		{
			statement.setLong(1, guildParam.getGuildId());

			ArrayList<ShopEntry> list = new ArrayList<>();

			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next())
			{
				long roleId = resultSet.getLong("role");
				String alias = resultSet.getString("alias");
				String description = resultSet.getString("description");
				int cost = resultSet.getInt("cost");

				Role role = guild.getRoleById(roleId);
				if(role == null)
					continue;

				ShopEntry entry = new ShopEntry();
				entry.role = RoleInfo.fromRole(role);
				entry.alias = alias;
				entry.description = description;
				entry.cost = cost;

				list.add(entry);
			}

			return list;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		throw new InternalServerErrorException();
	}

	@GET
	@Path("/inventory")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> inventory(@BeanParam GuildParam guildParam,
	                            @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");

		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT role FROM shop_inventory WHERE guild = ? AND user = ?"))
		{
			statement.setLong(1, guildParam.getGuildId());
			statement.setLong(2, user.getUserId());

			ArrayList<String> list = new ArrayList<>();

			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next())
			{
				long roleId = resultSet.getLong("role");
				list.add(Long.toString(roleId));
			}

			return list;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		throw new InternalServerErrorException();
	}

	@PUT
	@Path("/inventory/{role}")
	public void buyItem(@BeanParam GuildParam guildParam,
	                    @PathParam("role") String role,
	                    @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");

		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		long roleId = Long.parseLong(role);
		int cost = Integer.MAX_VALUE;

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT role, cost FROM shop_roles WHERE guild = ? AND role = ?");
		    PreparedStatement testStatement = connection.prepareStatement("SELECT 1 FROM shop_inventory WHERE role = ? AND user = ?"))
		{
			statement.setLong(1, guildParam.getGuildId());
			statement.setLong(2, roleId);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				roleId = resultSet.getLong("role");
				cost = resultSet.getInt("cost");

				testStatement.setLong(1, roleId);
				testStatement.setLong(2, user.getUserId());
				if(testStatement.executeQuery().next())
				{
					throw new WebApplicationException(Response.Status.CONFLICT);
				}
			}
			else
			{
				throw new NotFoundException();
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		if(DatabaseUtils.getCoins(user.getUserId(), guildParam.getGuildId()) < cost)
		{
			throw new WebApplicationException(Response.Status.PAYMENT_REQUIRED);
		}

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement buyStatement = connection.prepareStatement("INSERT INTO shop_inventory (role, user, guild) VALUES (?, ?, ?)"))
		{
			buyStatement.setLong(1, roleId);
			buyStatement.setLong(2, user.getUserId());
			buyStatement.setLong(3, guildParam.getGuildId());

			if(buyStatement.executeUpdate() == 0)
			{
				throw new InternalServerErrorException();
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			throw new InternalServerErrorException();
		}

		DatabaseUtils.changeCoins(user.getUserId(), guildParam.getGuildId(), -cost);
	}

	@PUT
	@Path("/items")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putItems(@BeanParam GuildParam guildParam,
	                     ShopEntry entry,
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
			PreparedStatement statement = connection.prepareStatement("REPLACE INTO shop_roles (role, guild, alias, description, cost) VALUES (?, ?, ?, ?, ?)"))
		{
			statement.setLong(1, Long.parseLong(entry.role.id));
			statement.setLong(2, guild.getIdLong());
			statement.setString(3, entry.alias);
			statement.setString(4, entry.description);
			statement.setInt(5, entry.cost);

			if(statement.executeUpdate() == 0)
				throw new InternalServerErrorException();
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	@PATCH
	@Path("/items")
	@Consumes(MediaType.APPLICATION_JSON)
	public void patchItems(@BeanParam GuildParam guildParam,
	                           ShopEntry entry,
	                           @Context ContainerRequestContext request)
	{
		putItems(guildParam, entry, request);
	}

	@DELETE
	@Path("/items/{role}")
	public void deleteItems(@BeanParam GuildParam guildParam,
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
		    PreparedStatement statement = connection.prepareStatement("DELETE FROM shop_roles WHERE role = ? AND guild = ?"))
		{
			statement.setLong(1, role);
			statement.setLong(2, guild.getIdLong());

			if(statement.executeUpdate() == 0)
				throw new NotFoundException();
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
	}

	private static class ShopEntry
	{
		public RoleInfo role;
		public String alias;
		public String description;
		public int cost;
	}
}
