package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.api.guild.*;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

public class ApiServer
{
	private final HttpServer server;

	private static URI getBaseURI(int port)
	{
		return UriBuilder.fromUri("http://localhost/").port(port).build();
	}

	public ApiServer(int port)
	{
		ResourceConfig rc = new ResourceConfig();
		rc.registerClasses(TokenEndpoint.class,

		                   InfoEndpoint.class,
		                   SettingsEndpoint.class,
		                   CustomCommandsEndpoint.class,
		                   LanguageOverridesEndpoint.class,
		                   CommandAliasesEndpoint.class,

		                   UserEndpoint.class,
		                   ReminderEndpoint.class,
		                   PollEndpoint.class,
		                   ShopEndpoint.class);
		rc.registerClasses(CORSFilter.class);
		rc.registerClasses(TokenFilter.class);
		rc.registerClasses(JacksonFeature.class);
		rc.registerClasses(MultiPartFeature.class);

		server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(port), rc);
	}

	public void start()
	{
		try
		{
			server.start();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Provider
	private static class CORSFilter implements ContainerResponseFilter
	{
		@Override
		public void filter(ContainerRequestContext request, ContainerResponseContext response)
		{
			response.getHeaders().add("Access-Control-Allow-Origin", "*");
			response.getHeaders().add("Access-Control-Allow-Headers",
			                          "origin, content-type, accept, authorization, access-control-allow-origin");
			response.getHeaders().add("Access-Control-Allow-Credentials", "true");
			response.getHeaders().add("Access-Control-Allow-Methods",
			                          "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
		}
	}

	public void shutdown()
	{
		server.shutdown();
	}
}
