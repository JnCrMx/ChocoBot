package de.jcm.discord.chocobot;

import de.jcm.discord.chocobot.command.*;
import de.jcm.discord.chocobot.command.secret.CommandChii;
import de.jcm.discord.chocobot.command.secret.CommandOmaeWaMouShindeiru;
import de.jcm.discord.chocobot.command.secret.MirrorListener;
import de.jcm.discord.chocobot.game.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChocoBot extends ListenerAdapter
{
	public static String prefix = "?";

	public static final Color COLOR_COOKIE = new Color(253, 189, 59);
	public static final Color COLOR_LOVE = new Color(255, 79, 237);
	public static final Color COLOR_ERROR = new Color(255, 0, 0);
	public static final Color COLOR_COINS = new Color(255, 255, 0);
	public static final Color COLOR_WARN = new Color(255, 0, 0);
	public static final Color COLOR_GAME = new Color(0, 255, 229);

	public static String warningChannel;
	public static List<String> warningRoles;

	public static String remindChannel;

	private static String redditUsername;
	private static String redditPassword;
	private static String redditAppId;
	private static String redditAppSecret;

	public static Connection database;
	public static ScheduledExecutorService executorService;
	private static Future<?> remindFuture;
	public static JDA jda;
	public static final Client client = ClientBuilder.newClient();
	public static String redditToken;

	public static final File bugreportDirectory = new File("bugreports");

	private static void tryCreateTable(Statement statement, String sql)
	{
		try
		{
			statement.execute(sql);
		}
		catch (SQLException ignored)
		{
		}
	}

	public static void main(String[] args) throws LoginException, SQLException, FileNotFoundException
	{
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.load(new FileInputStream("config.yml"));

		//Load config
		prefix = (String) obj.get("prefix");

		Map<String, Object> redditLogin = (Map<String, Object>) obj.get("reddit");
		redditUsername = (String) redditLogin.get("username");
		redditPassword = (String) redditLogin.get("password");
		redditAppId = (String) redditLogin.get("appId");
		redditAppSecret = (String) redditLogin.get("appSecret");

		Map<String, Object> discordLogin = (Map<String, Object>) obj.get("discord");
		String discordToken = (String) discordLogin.get("token");

		Map<String, Object> serverConfig = (Map<String, Object>) obj.get("server");
		warningChannel = (String) serverConfig.get("warningChannel");
		warningRoles = (List<String>) serverConfig.get("warningRoles");
		remindChannel = (String) serverConfig.get("remindChannel");

		Logger logger = LoggerFactory.getLogger(ChocoBot.class);
		bugreportDirectory.mkdirs();

		logger.info("Starting ChocoBot...");
		Runtime.getRuntime().addShutdownHook(new Thread(() ->
		{
			try
			{
				logger.info("Stopping ChocoBot...");
				remindFuture.cancel(true);
				logger.info("Stopped remind thread.");
				jda.shutdown();
				logger.info("Stopped JDA.");

				try
				{
					database.close();
					logger.info("Closed database.");
				}
				catch (SQLException var2)
				{
					var2.printStackTrace();
				}

				logger.info("Stopped ChocoBot.");
				Runtime.getRuntime().halt(0);
			}
			catch (Throwable var3)
			{
				var3.printStackTrace();
			}

		}));
		logger.info("Registered shutdown hook.");

		executorService = Executors.newSingleThreadScheduledExecutor();

		database = DriverManager.getConnection("jdbc:sqlite:chocobot.sqlite");
		logger.info("Connected to SQLite database.");
		Statement initStatement = database.createStatement();
		tryCreateTable(initStatement, "CREATE TABLE \"coins\" (\"uid\" INTEGER, \"coins\" INTEGER, \"last_daily\" INTEGER, \"daily_streak\" INTEGER, PRIMARY KEY(\"uid\"));");
		tryCreateTable(initStatement, "CREATE TABLE \"warnings\" (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT,\"uid\" INTEGER,\"reason\" TEXT,\"time\" INTEGER,\"message\" INTEGER, \"warner\" INTEGER);");
		tryCreateTable(initStatement, "CREATE TABLE \"reminders\" (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT, \"uid\" INTEGER, \"message\" TEXT, \"time\" INTEGER, \"issuer\" INTEGER, \"done\" INTEGER DEFAULT 0);");

		DatabaseUtils.prepare();
		QuizGame.prepare();

		Command.registerCommand(new SlotMachineGame());
		Command.registerCommand(new BlockGame());
		Command.registerCommand(new CommandGame(GiftGame.class));
		Command.registerCommand(new CommandGame(QuizGame.class));

		Command.registerCommand(new CommandCoins());
		Command.registerCommand(new CommandDaily());
		Command.registerCommand(new CommandGift());

		Command.registerCommand(new CommandSay());
		Command.registerCommand(new CommandRandom());
		Command.registerCommand(new CommandRemind());
		Command.registerCommand(new CommandMeme());
		Command.registerCommand(new CommandGif());
		Command.registerCommand(new CommandWarn());
		Command.registerCommand(new CommandWarns());
		Command.registerCommand(new CommandUnwarn());
		Command.registerCommand(new CommandHelp());
		Command.registerCommand(new CommandCredits());
		Command.registerCommand(new CommandBugReport());

		Command.registerCommand(new CommandKeks());
		Command.registerCommand(new CommandShip());

		Command.registerCommand(new CommandOmaeWaMouShindeiru());
		Command.registerCommand(new CommandChii());
		logger.info("Registered commands.");

		jda = (new JDABuilder(discordToken))
				.addEventListeners(new ChocoBot())
				.addEventListeners(new CommandListener())
				.addEventListeners(new MirrorListener())
				.setActivity(Activity.listening("dem Prefix '"+prefix+"'")).build();

		try
		{
			jda.awaitReady();
		}
		catch (InterruptedException var4)
		{
			var4.printStackTrace();
		}

		logger.info("Started JDA.");
		remindFuture = executorService.scheduleWithFixedDelay(new RemindRunnable(jda), 0L, 10L, TimeUnit.SECONDS);
		logger.info("Started remind thread.");
		initReddit();
		logger.info("Initialized Reddit API.");
		logger.info("Started ChocoBot.");
	}

	private static void initReddit()
	{
		Client client2 = ClientBuilder.newClient();
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
				.nonPreemptive().credentials(redditAppId, redditAppSecret).build();
		client2.register(feature);
		WebTarget raccess = client2.target("https://www.reddit.com/api/v1/access_token");
		HashMap<?, ?> response = (HashMap<?,?>) raccess.request("application/json")
				.post(Entity.form((new Form()).param("grant_type", "password")
						.param("username", redditUsername)
						.param("password", redditPassword)), HashMap.class);
		redditToken = (String) response.get("access_token");
		int exp = (Integer) response.get("expires_in");
		executorService.schedule(ChocoBot::initReddit, exp - 10, TimeUnit.SECONDS);
	}

	public static MessageEmbed errorMessage(String message)
	{
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Fehler");
		eb.setColor(COLOR_ERROR);
		eb.setDescription(message);
		return eb.build();
	}
}
