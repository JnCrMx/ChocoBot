package de.jcm.discord.chocobot.api;

import com.google.protobuf.Api;
import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.User;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Path("/token")
public class TokenEndpoint
{
	@POST
	@Path("/check")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@PermitAll
	public boolean check(@FormDataParam("token") String token)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM tokens WHERE token = ?"))
		{
			statement.setString(1, token);
			ResultSet result = statement.executeQuery();
			return result.next();
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return false;
	}

	@GET
	@Path("/guilds")
	@Produces(MediaType.APPLICATION_JSON)
	public List<GuildId> guilds(@Context ContainerRequestContext request)
	{
		User user = ((ApiUser)request.getProperty("user")).toUser();

		return user.getMutualGuilds().stream()
		    .map(g->
		         {
		         	GuildId gi = new GuildId();
		         	gi.id = g.getId();
		         	gi.name = g.getName();
		         	gi.iconUrl = g.getIconUrl();
		         	return gi;
		         })
			.collect(Collectors.toList());
	}

	private static class GuildId
	{
		public String id;
		public String name;
		public String iconUrl;
	}
}
