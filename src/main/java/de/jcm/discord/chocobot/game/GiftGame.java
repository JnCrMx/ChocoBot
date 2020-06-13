package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class GiftGame extends Game
{
	private static final String EMOJI_GIFT = "\ud83c\udf81";
	private static final int REWARD_PER_GIFT = 30;
	private ArrayList<String> targets;
	private ArrayList<String> found;
	private HashMap<Member, Integer> scores;
	private Message gameMessage;

	public GiftGame(Member sponsor, TextChannel gameChannel)
	{
		super(sponsor, gameChannel);
	}

	private List<Message> getMessages(TextChannel c, int amount)
	{
		MessageHistory history = c.getHistory();

		int toReceive;
		while ((toReceive=amount - history.size())>0) {
			history.retrievePast(Math.min(toReceive, 100)).complete();
		}

		ArrayList<Message> messages = new ArrayList<>(history.getRetrievedHistory());
		if(messages.size()>amount)
		{
			messages.subList(amount, messages.size()-1).clear();
		}
		return messages;
	}

	protected void play()
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_GAME);
		builder.setTitle("Geschenke!");

		int giftCount = players.size()*5;
		int messageCount = giftCount*5;

		builder.setDescription("In den letzen "+messageCount+" Nachrichten wurden "+
				giftCount+" Geschenke versteckt. Such nach ihnen und reagiere auf sie!");
		this.gameChannel.sendMessage(builder.build()).queue((m) ->
		{
			this.gameMessage = m;
			this.gameMessage.delete().queueAfter(20L, TimeUnit.SECONDS, (v) ->
			{
				this.targets.forEach(this::removeReactions);
				EmbedBuilder builder1 = new EmbedBuilder();
				builder1.setColor(ChocoBot.COLOR_GAME);
				builder1.setTitle("Punkte");
				builder1.setDescription("Die folgenden Punktzahlen wurden erreicht:");
				this.scores.entrySet().stream().sorted(Comparator.comparingInt(Entry::getValue)).forEach((e) ->
				{
					int count = e.getValue();
					int coins = count * REWARD_PER_GIFT;
					builder1.addField(e.getKey().getEffectiveName(), count + EMOJI_GIFT + " => +" + coins + " Coins", false);
					DatabaseUtils.changeCoins(e.getKey().getIdLong(), guild.getIdLong(), coins);
				});
				this.gameChannel.sendMessage(builder1.build()).queue();
				this.end();
			});
		});
		this.scores = new HashMap<>();
		for(Member player : players)
		{
			scores.put(player, 0);  // We want to show every player in the scoreboard,
									// even if they don't find any gifts.
		}
		ChocoBot.executorService.execute(()->
		{
			try
			{
				List<Message> history = getMessages(gameChannel, messageCount+1);
				history.remove(0); // Remove the game message itself

				if(history.size()<giftCount)
				{
					throw new ArrayIndexOutOfBoundsException("not enough messages: "
							+history.size()+" < "+giftCount);
				}
				if(history.size()<messageCount)
				{
					logger.error("We did not receive enough messages ("+history.size()+" < "+messageCount+
							"), but we will continue, because it is still enough to hide all the gifts (" +
							history.size()+" >= "+giftCount+")!");
				}
				if(history.size()>messageCount)
				{
					logger.warn("We received too many massages ("+history.size()+" > "+messageCount+
							"). That is kinda weird!");
				}

				Random random = new Random();
				this.targets = new ArrayList<>();
				this.found = new ArrayList<>();

				for (int i = 0; i < giftCount; ++i)
				{
					if(history.isEmpty())
					{
						throw new ArrayIndexOutOfBoundsException("not enough messages: "+
								"history became empty while hiding gifts");
					}

					Message target = history.remove(random.nextInt(history.size()));
					target.addReaction(EMOJI_GIFT).queue();
					this.targets.add(target.getId());
				}

				this.state = GameState.RUNNING;
			}
			catch (Throwable t)
			{
				logger.error("Fatal error in gift game!", t);

				gameMessage.delete().queue();
				this.state = GameState.FINISHED;

				DatabaseUtils.changeCoins(sponsor.getIdLong(), guild.getIdLong(), getSponsorCost());
				gameChannel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler! " +
						"Deine Coins wurden rückerstattet!")).queue();
			}
		});
	}

	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
	{
		super.onMessageReactionAdd(event);
		if (!event.getUser().isBot())
		{
			if (this.state == GameState.RUNNING && event.getChannel().getId().equals(this.gameChannel.getId()) && this.targets.contains(event.getMessageId()) && !this.found.contains(event.getMessageId()) && this.players.contains(event.getMember()))
			{
				this.scores.put(event.getMember(), this.scores.getOrDefault(event.getMember(), 0) + 1);
				this.found.add(event.getMessageId());
			}

		}
	}

	private void removeReactions(String messageId)
	{
		this.gameChannel.retrieveMessageById(messageId).queue((s) ->
				s.clearReactions().queue());
	}

	public String getName()
	{
		return "Geschenke";
	}

	public int getSponsorCost()
	{
		return 100;
	}
}
