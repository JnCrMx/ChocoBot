package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;

import javax.annotation.security.PermitAll;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TokenFilter implements ContainerRequestFilter
{
	@Context
	private ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext context)
	{
		if(context.getMethod().equals("OPTIONS"))
			return;
		if(resourceInfo.getResourceMethod().isAnnotationPresent(PermitAll.class))
			return;

		String authentication = context.getHeaderString(HttpHeaders.AUTHORIZATION);
		if(authentication == null)
		{
			context.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
		if(!authentication.startsWith("Bearer "))
		{
			context.abortWith(Response.status(Response.Status.NOT_ACCEPTABLE).build());
			return;
		}

		String token = authentication.substring(authentication.indexOf(' ')+1);
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT user FROM tokens WHERE token = ?"))
		{
			statement.setString(1, token);
			ResultSet result = statement.executeQuery();
			if(result.next())
			{
				ApiUser user = new ApiUser(result.getLong("user"));
				context.setProperty("user", user);
			}
			else
			{
				context.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			context.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
		}
	}
}
