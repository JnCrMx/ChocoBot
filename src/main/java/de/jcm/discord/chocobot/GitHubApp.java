package de.jcm.discord.chocobot;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubApp
{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private File privateKey;
	private int appId;
	private int installationId;

	private String jwt;
	private LocalDateTime jwtExpiration;

	private String installationToken;
	private ZonedDateTime installationTokenExpiration;

	private String user;
	private String repository;

	public GitHubApp(File privateKey, int appId, int installationId, String user, String repository)
	{
		this.privateKey = privateKey;
		this.appId = appId;
		this.installationId = installationId;

		this.user = user;
		this.repository = repository;
	}

	private String getJwt()
	{
		if(jwtExpiration == null ||
				jwtExpiration.isBefore(LocalDateTime.now()))
		{
			try
			{
				createJwt();
			}
			catch(NoSuchAlgorithmException | IOException | InvalidKeySpecException e)
			{
				e.printStackTrace();
			}
		}

		return jwt;
	}

	private void createJwt() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException
	{
		long time = System.currentTimeMillis();

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(loadKey());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(spec);

		Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);

		jwtExpiration = LocalDateTime.now().plusMinutes(10);
		jwt = JWT.create()
		   .withIssuer(Integer.toString(appId))
		   .withIssuedAt(new Date(time))
		   .withExpiresAt(new Date(time+10*60*1000))
		   .sign(algorithm);

		logger.info("Created new JWT with expiration at "+JWT.decode(jwt).getExpiresAt().toInstant());
	}

	private byte[] loadKey() throws IOException
	{
		InputStream in = new FileInputStream(privateKey);

		return in.readAllBytes();
	}

	public String getToken()
	{
		if(installationTokenExpiration == null ||
				installationTokenExpiration.isBefore(ZonedDateTime.now()))
			createInstallationToken();

		return installationToken;
	}

	private void createInstallationToken()
	{
		WebTarget target =
				ChocoBot.client.target("https://api.github.com/app/installations/")
				               .path(Integer.toString(installationId))
				               .path("access_tokens");
		HashMap<?, ?> response = target.request("application/vnd.github.machine-man-preview+json")
		      .header("Authorization", "Bearer "+getJwt())
		      .post(Entity.form(new Form()), HashMap.class);

		installationToken = (String) response.get("token");
		String ex = (String) response.get("expires_at");

		installationTokenExpiration = ZonedDateTime.parse(ex, DateTimeFormatter.ISO_DATE_TIME);

		logger.info("Created new installation token with expiration at "+installationTokenExpiration);
	}

	public HashMap<?, ?> createIssue(String title, @Nullable String body, @Nullable User reporter)
	{
		HashMap<String, Object> entity = new HashMap<>();
		entity.put("title", title);

		String bodyText = ((body!=null?body:"")+(reporter!=null?"\n\n*reported by: "+reporter.getAsTag()+"*":"")).trim();
		if(!bodyText.isBlank())
		{
			entity.put("body", bodyText);
		}

		entity.put("labels", List.of("bug"));

		WebTarget target = ChocoBot.client.target("https://api.github.com/repos/")
				.path(user)
				.path(repository)
				.path("/issues");
		Response response = target.request()
		      .header("Authorization", "token "+getToken())
		      .post(Entity.json(entity));
		if(response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
		{
			throw new RuntimeException("request failed: "+response);
		}
		return response.readEntity(HashMap.class);
	}

	public List<?> getIssueEvents(int issueId)
	{
		WebTarget target = ChocoBot.client.target("https://api.github.com/repos/")
				.path(user)
				.path(repository)
				.path("/issues")
				.path(Integer.toString(issueId))
				.path("/events");
		Response response = target.request()
		             .header("Authorization", "token "+getToken())
		             .get();
		if(response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL)
			return response.readEntity(List.class);
		else
		{
			System.out.println(response.readEntity(String.class));
			throw new RuntimeException("Got status " +
					                           response.getStatusInfo().getStatusCode() +
					                           " " +
					                           response.getStatusInfo().getReasonPhrase());
		}
	}

	public Map<?, ?> getIssueEvent(Map<?, ?> event)
	{
		WebTarget target = ChocoBot.client.target((String) event.get("url"));
		return target.request()
		             .header("Authorization", "token "+getToken())
		             .get(Map.class);
	}

	public List<?> getIssueComments(int issueId)
	{
		WebTarget target = ChocoBot.client.target("https://api.github.com/repos/")
		                                  .path(user)
		                                  .path(repository)
		                                  .path("/issues")
		                                  .path(Integer.toString(issueId))
		                                  .path("/comments");
		return target.request()
		             .header("Authorization", "token "+getToken())
		             .get(List.class);
	}

	public <T> T get(String url, Class<T> clazz)
	{
		return ChocoBot.client.target(url).request()
		             .header("Authorization", "token "+getToken())
		             .get(clazz);
	}
}
