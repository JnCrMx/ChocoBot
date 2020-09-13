package de.jcm.discord.chocobot;

import de.jcm.discord.chocobot.api.ApiServer;
import de.jcm.discord.chocobot.command.*;
import de.jcm.discord.chocobot.command.coin.CommandCoins;
import de.jcm.discord.chocobot.command.coin.CommandDaily;
import de.jcm.discord.chocobot.command.coin.CommandGift;
import de.jcm.discord.chocobot.command.secret.*;
import de.jcm.discord.chocobot.command.subscription.SubscriptionListener;
import de.jcm.discord.chocobot.command.warn.CommandUnwarn;
import de.jcm.discord.chocobot.command.warn.CommandWarn;
import de.jcm.discord.chocobot.command.warn.CommandWarns;
import de.jcm.discord.chocobot.game.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.dbcp2.BasicDataSource;
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
	public static final Color COLOR_COOKIE = new Color(253, 189, 59);
	public static final Color COLOR_LOVE = new Color(255, 79, 237);
	public static final Color COLOR_ERROR = new Color(255, 0, 0);
	public static final Color COLOR_COINS = new Color(255, 255, 0);
	public static final Color COLOR_WARN = new Color(255, 0, 0);
	public static final Color COLOR_GAME = new Color(0, 255, 229);

	private static String redditUsername;
	private static String redditPassword;
	private static String redditAppId;
	private static String redditAppSecret;

	private static BasicDataSource dataSource = new BasicDataSource();

	public static ScheduledExecutorService executorService;
	private static Future<?> remindFuture;
	public static JDA jda;
	public static final Client client = ClientBuilder.newClient();
	public static String redditToken;

	public static GitHubApp githubApp;
	private static Future<?> issueEventFuture;
	public static boolean githubBugReportEnabled;
	public static File bugReportDirectory;

	public static String boardUrl;
	private static ApiServer apiServer;

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

	public static Connection getDatabase() throws SQLException
	{
		return dataSource.getConnection();
	}

	public static void main(String[] args) throws LoginException, SQLException, FileNotFoundException, ClassNotFoundException
	{
		Yaml yaml = new Yaml();
		Map<String, Object> obj = yaml.load(new FileInputStream("config.yml"));

		//Load config
		boardUrl = (String) obj.get("boardUrl");
		int apiPort = (int) obj.get("apiPort");

		Map<String, Object> redditLogin = (Map<String, Object>) obj.get("reddit");
		boolean redditEnabled = (boolean)
				redditLogin.getOrDefault("enabled", true);
		if(redditEnabled)
		{
			redditUsername = (String) redditLogin.get("username");
			redditPassword = (String) redditLogin.get("password");
			redditAppId = (String) redditLogin.get("appId");
			redditAppSecret = (String) redditLogin.get("appSecret");
		}

		Map<String, Object> discordLogin = (Map<String, Object>) obj.get("discord");
		String discordToken = (String) discordLogin.get("token");

		Logger logger = LoggerFactory.getLogger(ChocoBot.class);

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
				apiServer.shutdown();
				logger.info("Stopped API.");

				try
				{
					dataSource.close();
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

		Map<String, Object> dbConfig = (Map<String, Object>) obj.get("database");
		String dbType = (String) dbConfig.getOrDefault("type", "sqlite");
		if("sqlite".equals(dbType))
		{
			String dbPath = (String) dbConfig.getOrDefault("path", "chocobot.sqlite");
			dataSource.setUrl("jdbc:sqlite:"+dbPath);
			dataSource.setAutoCommitOnReturn(true);
			dataSource.setMaxTotal(1);

			dataSource.setAbandonedUsageTracking(true);
			dataSource.setLogAbandoned(true);
			dataSource.setRemoveAbandonedOnBorrow(true);
			dataSource.setRemoveAbandonedTimeout(1);

			logger.info("Connected to SQLite database.");

			try(Connection connection = getDatabase();
			    Statement initStatement = connection.createStatement())
			{
				tryCreateTable(initStatement, "CREATE TABLE \"coins\" (\"uid\" INTEGER, \"guild\" INTEGER, \"coins\" INTEGER, \"last_daily\" INTEGER, \"daily_streak\" INTEGER, PRIMARY KEY(\"uid\", \"guild\"));");
				tryCreateTable(initStatement, "CREATE TABLE \"warnings\" (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT,\"uid\" INTEGER, \"guild\" INTEGER, \"reason\" TEXT,\"time\" INTEGER,\"message\" INTEGER, \"warner\" INTEGER);");
				tryCreateTable(initStatement, "CREATE TABLE \"reminders\" (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT, \"uid\" INTEGER, \"guild\" INTEGER, \"message\" TEXT, \"time\" INTEGER, \"issuer\" INTEGER, \"done\" INTEGER DEFAULT 0);");
				tryCreateTable(initStatement, "CREATE TABLE \"bugreports\" (\"id\" INTEGER PRIMARY KEY, \"reporter\" INTEGER, \"last_event_time\" INTEGER);");
				tryCreateTable(initStatement, "CREATE TABLE \"subscriptions\" (\"id\" INTEGER PRIMARY KEY, \"subscriber\" INTEGER, \"keyword\" TEXT);");
				tryCreateTable(initStatement, "CREATE TABLE \"tokens\" (\"token\" VARCHAR(256) PRIMARY KEY, \"user\" INTEGER);");
				tryCreateTable(initStatement, "CREATE TABLE \"polls\" (\"message\" INTEGER PRIMARY KEY, \"guild\" INTEGER, \"question\" TEXT);");
				tryCreateTable(initStatement, "CREATE TABLE \"poll_answers\" (\"answer\" VARCHAR(256), \"poll\" INTEGER, \"votes\" INTEGER, PRIMARY KEY(\"answer\", \"poll\"));");
				tryCreateTable(initStatement, "CREATE TABLE \"guilds\" (\"id\" INTEGER PRIMARY KEY, \"prefix\" VARCHAR(16), \"command_channel\" INTEGER, \"remind_channel\" INTEGER, \"warning_channel\" INTEGER, \"poll_channel\" INTEGER);");
				tryCreateTable(initStatement, "CREATE TABLE \"guild_operators\" (\"id\" INTEGER, \"guild\" INTEGER, PRIMARY KEY(\"id\", \"guild\"));");
				tryCreateTable(initStatement, "CREATE TABLE \"guild_muted_channels\" (\"channel\" INTEGER PRIMARY KEY, \"guild\" INTEGER);");
				tryCreateTable(initStatement, "CREATE TABLE \"shop_roles\" (\"role\" INTEGER PRIMARY KEY, \"guild\" INTEGER, \"alias\" VARCHAR(256), \"description\" TEXT, \"cost\" INTEGER, UNIQUE (\"alias\", \"guild\"));");
				tryCreateTable(initStatement, "CREATE TABLE \"shop_inventory\" (\"role\" INTEGER, \"user\" INTEGER, \"guild\" INTEGER, PRIMARY KEY(\"role\", \"user\"));");
			}
		}
		else if("mysql".equals(dbType))
		{
			Class.forName("com.mysql.cj.jdbc.Driver");
			String connectionURL = "jdbc:mysql://address="
					+ "(host=" + dbConfig.getOrDefault("host", "localhost") + ")"
					+ "(port=" + dbConfig.getOrDefault("port", 3306) + ")"
					+ "(autoReconnect=true)"
					+ "/" + dbConfig.getOrDefault("database", "chocobot");
			dataSource.setUrl(connectionURL);
			dataSource.setUsername((String) dbConfig.get("user"));
			dataSource.setPassword((String) dbConfig.get("password"));
			dataSource.setAutoCommitOnReturn(true);

			dataSource.setAbandonedUsageTracking(true);
			dataSource.setLogAbandoned(true);
			dataSource.setRemoveAbandonedOnBorrow(true);
			dataSource.setRemoveAbandonedTimeout(60);

			logger.info("Connected to MySQL database.");

			try(Connection connection = getDatabase();
				Statement initStatement = connection.createStatement())
			{
				tryCreateTable(initStatement, "CREATE TABLE `coins` (`uid` BIGINT, `guild` BIGINT, `coins` INT, `last_daily` BIGINT, `daily_streak` INT, PRIMARY KEY(`uid`, `guild`));");
				tryCreateTable(initStatement, "CREATE TABLE `warnings` (`id` INT PRIMARY KEY AUTO_INCREMENT,`uid` BIGINT,`guild` BIGINT,`reason` TEXT,`time` BIGINT, `message` BIGINT, `warner` BIGINT);");
				tryCreateTable(initStatement, "CREATE TABLE `reminders` (`id` INT PRIMARY KEY AUTO_INCREMENT, `uid` BIGINT, `guild` BIGINT, `message` TEXT, `time` BIGINT, `issuer` BIGINT, `done` BOOLEAN DEFAULT 0);");
				tryCreateTable(initStatement, "CREATE TABLE `bugreports` (`id` INT PRIMARY KEY, `reporter` BIGINT, `last_event_time` BIGINT);");
				tryCreateTable(initStatement, "CREATE TABLE `subscriptions` (`id` INT PRIMARY KEY AUTO_INCREMENT, `subscriber` BIGINT, `keyword` TEXT);");
				tryCreateTable(initStatement, "CREATE TABLE `tokens` (`token` VARCHAR(256) PRIMARY KEY, `user` BIGINT);");
				tryCreateTable(initStatement, "CREATE TABLE `polls` (`message` BIGINT PRIMARY KEY, `guild` BIGINT, `question` TEXT);");
				tryCreateTable(initStatement, "CREATE TABLE `poll_answers` (`answer` VARCHAR(256), `poll` BIGINT, `votes` INT, PRIMARY KEY(`answer`, `poll`));");
				tryCreateTable(initStatement, "CREATE TABLE `guilds` (`id` BIGINT PRIMARY KEY, `prefix` VARCHAR(16), `command_channel` BIGINT, `remind_channel` BIGINT, `warning_channel` BIGINT, `poll_channel` BIGINT);");
				tryCreateTable(initStatement, "CREATE TABLE `guild_operators` (`id` BIGINT, `guild` BIGINT, PRIMARY KEY(`id`, `guild`));");
				tryCreateTable(initStatement, "CREATE TABLE `guild_muted_channels` (`channel` BIGINT PRIMARY KEY, `guild` BIGINT);");
				tryCreateTable(initStatement, "CREATE TABLE `shop_roles` (`role` BIGINT PRIMARY KEY, `guild` BIGINT, `alias` VARCHAR(256), `description` TEXT, `cost` INT, UNIQUE `ident` (`alias`, `guild`));");
				tryCreateTable(initStatement, "CREATE TABLE `shop_inventory` (`role` BIGINT, `user` BIGINT, `guild` BIGINT, PRIMARY KEY(`role`, `user`));");
			}
		}
		
		DatabaseUtils.prepare();
		QuizGame.prepare();

		logger.info("Registering commands...");
		Command.registerCommand(new SlotMachineGame());
		Command.registerCommand(new BlockGame());
		Command.registerCommand(new CommandGame(GiftGame.class));
		Command.registerCommand(new CommandGame(QuizGame.class));

		Command.registerCommand(new CommandCoins());
		Command.registerCommand(new CommandDaily());
		Command.registerCommand(new CommandGift());

		Command.registerCommand(new CommandShop());

		Command.registerCommand(new CommandSay());
		Command.registerCommand(new CommandRandom());
		Command.registerCommand(new CommandRemind());
		Command.registerCommand(new CommandShip());
		Command.registerCommand(new CommandStats());

		if(redditEnabled)
			Command.registerCommand(new CommandMeme());
		Command.registerCommand(new CommandGif());
		Command.registerCommand(new CommandKeks());

		Command.registerCommand(new CommandWarn());
		Command.registerCommand(new CommandWarns());
		Command.registerCommand(new CommandUnwarn());

		Command.registerCommand(new CommandHelp());
		Command.registerCommand(new CommandCredits());
		Command.registerCommand(new CommandBugReport());

		Command.registerCommand(new CommandOmaeWaMouShindeiru());
		Command.registerCommand(new CommandChii());
		Command.registerCommand(new CommandArch());
		Command.registerCommand(new CommandLinux());
		Command.registerCommand(new CommandWarm());
		Command.registerCommand(new CommandQuit());
		Command.registerCommand(new CommandAnime());

		logger.info("Registered commands.");

		jda = (new JDABuilder(discordToken))
				.addEventListeners(new ChocoBot())
				.addEventListeners(new CommandListener())
				.addEventListeners(new MirrorListener())
				.addEventListeners(new IssueEventUnsubscribeListener())
				.addEventListeners(new SubscriptionListener())
				.addEventListeners(new ChocoBoardListener())
				.setActivity(Activity.listening("deinen Befehlen")).build();

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

		if(redditEnabled)
		{
			initReddit();
			logger.info("Initialized Reddit API.");
		}

		Map<String, Object> githubLogin = (Map<String, Object>) obj.get("github");
		githubBugReportEnabled = (boolean)
				githubLogin.getOrDefault("enabled", true);
		if(githubBugReportEnabled)
		{
			githubApp = new GitHubApp(
					new File((String) githubLogin.get("privateKey")),
					(Integer) githubLogin.get("appId"),
					(Integer) githubLogin.get("installationId"),
					(String) githubLogin.get("user"),
					(String) githubLogin.get("repository"));
			logger.info("Initialized GitHub App.");
			issueEventFuture = executorService.scheduleWithFixedDelay(
					new IssueEventRunnable(), 0L, 10L, TimeUnit.MINUTES);
		}
		else
		{
			bugReportDirectory = new File("bugreports");
			bugReportDirectory.mkdirs();
		}

		executorService.scheduleAtFixedRate(new PollRunnable(), 0L, 1L, TimeUnit.HOURS);

		apiServer = new ApiServer(apiPort);
		logger.info("Started API.");

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
