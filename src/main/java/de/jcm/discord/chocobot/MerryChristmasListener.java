package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static de.jcm.discord.chocobot.game.QuizGame.EMOJIS_ANSWER;

public class MerryChristmasListener extends ListenerAdapter
{
	private final Map<String, List<ChristmasGift>> choices = new HashMap<>();

	private static class ChristmasGift
	{
		private final long guild;
		private final long sender;
		private final long receiver;
		private final int amount;
		private final String message;

		ChristmasGift(long guild, long sender, long receiver, int amount, String message)
		{
			this.guild = guild;
			this.sender = sender;
			this.receiver = receiver;
			this.amount = amount;
			this.message = message;
		}

		long getGuild()
		{
			return guild;
		}

		long getSender()
		{
			return sender;
		}

		long getReceiver()
		{
			return receiver;
		}

		int getAmount()
		{
			return amount;
		}

		String getMessage()
		{
			return message;
		}
	}

	@Override
	public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event)
	{
		User user = event.getAuthor();

		PrivateChannel channel = event.getChannel();
		String message = event.getMessage().getContentRaw().strip();

		if(message.startsWith("?merry-christmas"))
		{
			GuildSettings settings = DatabaseUtils.getUserSettings(user);

			LocalDateTime now = LocalDateTime.now();
			if(now.getMonth() != Month.DECEMBER)
			{
				return; // no message here :P
			}
			if(now.getDayOfMonth() >= 27)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.late")).queue();
				return;
			}

			String[] args = new String[0];
			if(message.contains(" "))
			{
				String argstring = message.substring(message.indexOf(32) + 1);
				args = argstring.split(" ");
			}

			if(args.length < 2)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.narg")).queue();
				return;
			}

			try
			{
				String receiverId = args[0];
				int amount = Integer.parseInt(args[1]);
				if(amount < 0)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.negative")).queue();
				}
				String textMessage = null;
				if(args.length > 2)
				{
					textMessage = String.join(" ", Arrays.asList(args).subList(2, args.length));
				}

				User receiverUser = ChocoBot.jda.retrieveUserById(receiverId).onErrorMap(t->null).complete();
				if(receiverUser == null)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.who")).queue();
					return;
				}

				Map<Guild, Pair<Member, Member>> possibleGuilds = new HashMap<>();
				List<Guild> guilds = ChocoBot.jda.getGuilds();
				for(Guild testGuild : guilds)
				{
					Member tSender = testGuild.retrieveMember(user).onErrorMap(t->null).complete();
					if(tSender == null)
						continue;

					Member tReceiver = testGuild.retrieveMember(receiverUser).onErrorMap(t->null).complete();
					if(tReceiver == null)
						continue;

					possibleGuilds.put(testGuild, new ImmutablePair<>(tSender, tReceiver));
				}
				if(possibleGuilds.size() == 0)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.mutual")).queue();
					return;
				}

				if(possibleGuilds.size() == 1)
				{
					Guild guild = possibleGuilds.keySet().stream().findFirst().get();
					Pair<Member, Member> pair = possibleGuilds.get(guild);
					processGuild(new ChristmasGift(guild.getIdLong(),
					                               pair.getLeft().getIdLong(),
					                               pair.getRight().getIdLong(),
					                               amount, textMessage),
					             event.getChannel());
				}
				else
				{
					EmbedBuilder builder = new EmbedBuilder();
					builder.setTitle(settings.translate("merry-christmas.server_select.title"));
					builder.setColor(ChocoBot.COLOR_COOKIE);
					builder.setDescription(settings.translate("merry-christmas.server_select.description"));
					List<ChristmasGift> choiceList = new ArrayList<>();
					try(Connection connection = ChocoBot.getDatabase())
					{
						int i = 0;
						for(Guild guild : possibleGuilds.keySet())
						{
							Pair<Member, Member> pair = possibleGuilds.get(guild);
							int coins = DatabaseUtils.getCoins(connection, user.getIdLong(), guild.getIdLong());

							if(coins >= amount)
							{
								choiceList.add(new ChristmasGift(guild.getIdLong(),
								                                 pair.getLeft().getIdLong(),
								                                 pair.getRight().getIdLong(),
								                                 amount, textMessage));
								builder.addField(
										EMOJIS_ANSWER[i] + " " + guild.getName(),
										settings.translate(
												"merry-christmas.server_select.entry",
												coins, pair.getRight().getEffectiveName()),
										false);
								i++;
							}
							else
							{
								builder.addField(
										guild.getName(), settings.translate(
												"merry-christmas.server_select.entry.not_enough",
												coins, pair.getRight().getEffectiveName()),
										false);
							}
						}
					}
					catch(SQLException throwables)
					{
						throwables.printStackTrace();
					}
					channel.sendMessage(builder.build()).queue(m->{
						choices.put(m.getId(), choiceList);

						for(int i=0; i<choiceList.size(); i++)
						{
							m.addReaction(EMOJIS_ANSWER[i]).queue();
						}

						m.delete().queueAfter(1, TimeUnit.MINUTES, s->choices.remove(m.getId()), ex->{
							choices.remove(m.getId());
							ex.printStackTrace();
						});
					});
				}
			}
			catch(NumberFormatException var13)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.fmt")).queue();
			}
		}
	}

	@Override
	public void onPrivateMessageReactionAdd(@Nonnull PrivateMessageReactionAddEvent event)
	{
		if(event.getUser() == null || event.getUser().isBot())
			return;

		String messageId = event.getMessageId();
		if(choices.containsKey(messageId))
		{
			int choice = Arrays.binarySearch(EMOJIS_ANSWER, event.getReactionEmote().getEmoji());
			processGuild(choices.get(messageId).get(choice), event.getChannel());

			choices.remove(messageId);
			event.getChannel().deleteMessageById(event.getMessageId()).queue();
		}
	}

	private void processGuild(ChristmasGift gift, PrivateChannel channel)
	{
		GuildSettings settings = DatabaseUtils.getSettings(gift.getGuild());
		assert settings != null;
		try
		{
			int coins = DatabaseUtils.getCoins(gift.getSender(), gift.getGuild());
			if(coins < gift.getAmount())
			{
				channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.not_enough")).queue();
				return;
			}

			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement statement = connection.prepareStatement("INSERT INTO christmas_presents(uid, sender, guild, amount, message, year) VALUES(?, ?, ?, ?, ?, ?)"))
			{
				statement.setLong(1, gift.getReceiver());
				statement.setLong(2, gift.getSender());
				statement.setLong(3, gift.getGuild());
				statement.setInt(4, gift.getAmount());
				statement.setString(5, gift.getMessage());
				statement.setInt(6, LocalDateTime.now().getYear());
				statement.executeUpdate();

				DatabaseUtils.changeCoins(connection, gift.getSender(), gift.getGuild(), -gift.getAmount());
			}

			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(settings.translate("merry-christmas.success.title"));
			builder.setColor(ChocoBot.COLOR_COOKIE);
			builder.setDescription(settings.translate("merry-christmas.success.description"));
			channel.sendMessage(builder.build()).queue();
		}
		catch(Throwable t)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "merry-christmas.error.internal")).queue();
			t.printStackTrace();
		}
	}
}
