package de.jcm.discord.chocobot;

import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UsernameCrawler implements Runnable
{
	private static final Logger logger = LoggerFactory.getLogger(UsernameCrawler.class);
	private static final Random random = new Random();

	private static final String QUERY_SQL = "SELECT DISTINCT uid FROM " +
			"(" +
			"SELECT bugreports.reporter AS uid FROM bugreports UNION " +
			"SELECT coins.uid AS uid\tFROM coins UNION " +
			"SELECT reminders.uid AS uid FROM reminders UNION " +
			"SELECT reminders.issuer AS uid FROM reminders UNION " +
			"SELECT shop_inventory.user AS uid FROM shop_inventory UNION " +
			"SELECT subscriptions.subscriber AS uid FROM subscriptions UNION " +
			"SELECT tokens.user AS uid FROM tokens UNION " +
			"SELECT user_stats.uid AS uid FROM user_stats UNION " +
			"SELECT warnings.uid AS uid FROM warnings UNION " +
			"SELECT warnings.warner AS uid FROM warnings" +
			")a";
	private static final String UPDATE_SQL = "REPLACE INTO name_cache (id, name) VALUES(?, ?)";

	private final JDA jda;

	public UsernameCrawler(JDA jda)
	{
		this.jda = jda;
	}

	@Override
	public void run()
	{
		List<RestAction<User>> actionList = new ArrayList<>();

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement(QUERY_SQL))
		{
			ResultSet result = statement.executeQuery();
			while(result.next())
			{
				actionList.add(jda.retrieveUserById(result.getLong("uid"))
				                  // add a random delay up to 100s to prevent blocking the queue
				                  .delay(random.nextInt(100), TimeUnit.SECONDS)
				                  .onErrorMap(t->null));
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		logger.info("Crawling {} users. This might take a while.", actionList.size());

		if(!actionList.isEmpty())
		{
			RestAction.allOf(actionList).queue(users->{
				logger.info("Successfully crawled {} users.", users.size());

				ChocoBot.userCache.clear();
				users.stream()
				     .map(UserData.FROM_USER)
				     .map(data->new ImmutablePair<>(Long.parseLong(data.userId), data))
				     .forEach(pair -> ChocoBot.userCache.put(pair.getLeft(), pair.getRight()));

				try(Connection connection = ChocoBot.getDatabase();
					PreparedStatement statement = connection.prepareStatement(UPDATE_SQL))
				{
					for(User user : users)
					{
						if(user != null)
						{
							statement.setLong(1, user.getIdLong());
							statement.setString(2, user.getAsTag());
							statement.addBatch();
						}
					}
					statement.executeBatch();
				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			});
		}
	}
}
