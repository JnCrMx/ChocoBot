package de.jcm.discord.chocobot.command.subscription;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubscriptionListener extends ListenerAdapter
{
	private final Logger logger = LoggerFactory.getLogger(SubscriptionListener.class);

	private String[] keywordCache;

	public SubscriptionListener()
	{
		updateCache();
	}

	@Override
	public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event)
	{
		if(!event.getAuthor().isBot())
		{
			String message = event.getMessage().getContentRaw();
			if(message.startsWith("?"))
			{
				this.logger.info("Private Command \"{}\" from user {} ({}) received.",
				                 message, event.getAuthor().getAsTag(), event.getAuthor().getId());

				message = message.substring("?".length()).trim();
				String keyword = message;
				String argument = "";
				if(message.contains(" "))
				{
					keyword = message.substring(0, message.indexOf(' '));
					argument = message.substring(message.indexOf(' ')+1);
				}

				if(!(argument.startsWith("/") && (argument.endsWith("/g") || argument.endsWith("/gi"))))
				{
					argument = argument.toLowerCase();
				}

				GuildSettings settings = DatabaseUtils.getUserSettings(event.getAuthor());

				switch(keyword)
				{
					case "subscribe":
						subscribe(event.getAuthor(), settings, event.getChannel(), argument);
						break;
					case "sublist":
						sublist(event.getAuthor(), settings, event.getChannel());
						break;
					case "unsubscribe":
						unsubscribe(event.getAuthor(), settings, event.getChannel(), argument);
					default:
				}
			}
		}
	}

	private boolean checkSubscription(User user, String keyword)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement checkSubscription = connection.prepareStatement("SELECT * FROM subscriptions WHERE subscriber=? AND keyword=?"))
		{
			checkSubscription.setLong(1, user.getIdLong());
			checkSubscription.setString(2, keyword);
			try(ResultSet result = checkSubscription.executeQuery())
			{
				return result.next();
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private void subscribe(User user, GuildSettings settings, MessageChannel channel, String keyword)
	{
		if(keyword.isBlank())
		{
			channel.sendMessage(ChocoBot.translateError(settings, "subscription.subscribe.error.empty"))
			       .queue();
			return;
		}

		if(checkSubscription(user, keyword))
		{
			channel.sendMessage(ChocoBot.translateError(settings, "subscription.subscribe.error.dup"))
			       .queue();
			return;
		}

		if(keyword.startsWith("/") && (keyword.endsWith("/g") || keyword.endsWith("/gi")))
		{
			try
			{
				boolean i = keyword.endsWith("/gi");
				Pattern.compile(keyword.substring(1, keyword.length() - (i?3:2)), i?Pattern.CASE_INSENSITIVE:0);
			}
			catch(Throwable t)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "subscription.subscribe.error.regex", t.getMessage())).queue();
				return;
			}
		}

		try
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement insertSubscription = connection.prepareStatement("INSERT INTO subscriptions (subscriber, keyword) VALUES(?, ?)"))
			{
				insertSubscription.setLong(1, user.getIdLong());
				insertSubscription.setString(2, keyword);
				insertSubscription.execute();
			}

			if(checkSubscription(user, keyword))
			{
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle(settings.translate("subscription.subscribe.title"));
				eb.setDescription(settings.translate("subscription.subscribe.description", keyword));
				eb.setFooter(settings.translate("subscription.subscribe.footer", keyword));
				eb.setColor(ChocoBot.COLOR_COOKIE);
				channel.sendMessage(eb.build()).queue();

				updateCache();
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "subscription.subscribe.error.internal"))
				       .queue();
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	private void sublist(User user, GuildSettings settings, MessageChannel channel)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement byUser = connection.prepareStatement("SELECT * FROM subscriptions WHERE subscriber=?"))
		{
			byUser.setLong(1, user.getIdLong());
			try(ResultSet result = byUser.executeQuery())
			{
				StringBuilder b = new StringBuilder();
				while(result.next())
				{
					b.append(result.getString("keyword"));
					b.append('\n');
				}

				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle(settings.translate("subscription.sublist.title"));
				eb.setDescription(b.toString());
				eb.setFooter(settings.translate("subscription.sublist.footer"));
				eb.setColor(ChocoBot.COLOR_COOKIE);

				channel.sendMessage(eb.build()).queue();
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	private void unsubscribe(User user, GuildSettings settings, MessageChannel channel, String keyword)
	{
		if(keyword.isBlank())
		{
			channel.sendMessage(ChocoBot.translateError(settings, "subscription.unsubscribe.error.empty")).queue();
			return;
		}

		if(!checkSubscription(user, keyword))
		{
			channel.sendMessage(ChocoBot.translateError(settings, "subscription.unsubscribe.error.noent")).queue();
			return;
		}

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement deleteSubscription = connection.prepareStatement("DELETE FROM subscriptions WHERE subscriber=? AND keyword=?"))
		{
			deleteSubscription.setLong(1, user.getIdLong());
			deleteSubscription.setString(2, keyword);
			deleteSubscription.execute();

			if(!checkSubscription(user, keyword))
			{
				EmbedBuilder eb = new EmbedBuilder();
				eb.setTitle(settings.translate("subscription.unsubscribe.title"));
				eb.setDescription(settings.translate("subscription.unsubscribe.description", keyword));
				eb.setColor(ChocoBot.COLOR_COOKIE);
				channel.sendMessage(eb.build()).queue();

				updateCache();
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "subscription.unsubscribe.error.internal"))
				       .queue();
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event)
	{
		if(event.getAuthor().isBot())
			return;

		Guild guild = event.getGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);
		if(settings == null)
			return;
		if(settings.isChannelMuted(event.getChannel()))
			return;

		String message = event.getMessage().getContentRaw();

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement byKeyword = connection.prepareStatement("SELECT subscriber FROM subscriptions WHERE keyword=?"))
		{
			for(String keyword : keywordCache)
			{
				if(keyword.startsWith("/") && (keyword.endsWith("/g") || keyword.endsWith("/gi")))
				{
					try
					{
						boolean i = keyword.endsWith("/gi");
						Pattern pattern = Pattern.compile(keyword.substring(1, keyword.length() - (i?3:2)),
						                                  i?Pattern.CASE_INSENSITIVE:0);
						Matcher matcher = pattern.matcher(message);
						if(matcher.find())
						{
							byKeyword.setString(1, keyword);
							try(ResultSet result = byKeyword.executeQuery())
							{
								while(result.next())
								{
									guild.retrieveMemberById(result.getLong("subscriber"))
									     .queue(m->checkAndNotify(event, matcher.group(), m));
								}
							}
						}
					}
					catch(Throwable ignore)
					{

					}
				}
				else
				{
					// remove all links from the message, because we want to ignore them
					String messagePlain = message
							.replaceAll("(http(s|)*://|www\\.)\\S*", "")
							.toLowerCase();
					if(messagePlain.contains(keyword.toLowerCase()))
					{
						byKeyword.setString(1, keyword);
						try(ResultSet result = byKeyword.executeQuery())
						{
							while(result.next())
							{
								guild.retrieveMemberById(result.getLong("subscriber"))
								     .onErrorMap(t->null)
								     .queue(m->checkAndNotify(event, keyword, m));
							}
						}
					}
				}
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}

	private void checkAndNotify(GuildMessageReceivedEvent event, String keyword, Member member)
	{
		if(member == null)
			return;
		if(member.getIdLong() == event.getAuthor().getIdLong())
			return;
		if(!member.hasPermission(event.getChannel(), Permission.MESSAGE_READ))
			return;
		GuildSettings settings = DatabaseUtils.getSettings(event.getGuild());

		member.getUser().openPrivateChannel()
		      .queue(p -> p.sendMessage(
		      		settings.translate(
		      				"subscription.message",
					        keyword,
					        event.getChannel().getGuild().getIdLong(),
						    event.getChannel().getIdLong(),
					        event.getMessage().getIdLong())).queue());
	}

	private void updateCache()
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement listKeywords = connection.prepareStatement("SELECT DISTINCT keyword FROM subscriptions"))
		{
			ArrayList<String> keywords = new ArrayList<>();

			try(ResultSet result = listKeywords.executeQuery())
			{
				while(result.next())
				{
					keywords.add(result.getString("keyword"));
				}
			}

			keywordCache = keywords.toArray(String[]::new);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
}
