package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.RoleInfo;
import net.dv8tion.jda.api.entities.Guild;
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
			throw new WebApplicationException(Response.Status.UNAUTHORIZED);

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
		throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
	}

	private static class ShopEntry
	{
		public RoleInfo role;
		public String alias;
		public String description;
		public int cost;
	}
}
