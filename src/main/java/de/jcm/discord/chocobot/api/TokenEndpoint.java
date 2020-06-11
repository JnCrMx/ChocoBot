package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
