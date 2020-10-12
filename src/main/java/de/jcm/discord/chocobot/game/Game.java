package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class Game extends ListenerAdapter
{
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	final Member sponsor;
	List<Member> players;
	GameState state;
	final TextChannel gameChannel;
	final Guild guild;
	private Message confirmMessage;
	private Message announceMessage;

	Game(Member sponsor, TextChannel gameChannel)
	{
		this.state = GameState.CONFIRM;
		this.sponsor = sponsor;
		this.gameChannel = gameChannel;
		this.guild = gameChannel!=null ? gameChannel.getGuild() : null;
	}

	public void start()
	{
		if (DatabaseUtils.getCoins(this.sponsor.getIdLong(), guild.getIdLong()) < this.getSponsorCost())
		{
			this.gameChannel.sendMessage(ChocoBot.errorMessage("Du hast dafür nicht genug Coins! Du brauchst mindestens " + this.getSponsorCost() + ".")).queue();
			this.cleanup();
		}
		else
		{
			this.confirm();
		}
	}

	private void confirm()
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_GAME);
		builder.setAuthor("@" + this.sponsor.getEffectiveName());
		builder.setTitle("Bestätigen?");
		builder.setDescription("Wenn du wirklich fortfahren willst, reagiere mit :white_check_mark:!");
		builder.addField("Preis", this.getSponsorCost() + " Coins", false);
		this.gameChannel.sendMessage(builder.build()).queue((m) ->
		{
			this.confirmMessage = m;
			this.confirmMessage.addReaction("✅").queue();
			this.gameChannel.getJDA().addEventListener(this);
			m.delete().queueAfter(10L, TimeUnit.SECONDS, (v) ->
			{
				this.gameChannel.getJDA().removeEventListener(this);
				this.cancel();
			}, (t) ->
			{
			});
		});
	}

	private void cancel()
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_GAME);
		builder.setTitle("Abgebrochen!");
		builder.setDescription(this.sponsor.getAsMention() + " Dein Spiel wurde abgeborchen!");
		builder.setColor(ChocoBot.COLOR_ERROR);
		this.gameChannel.sendMessage(builder.build()).queueAfter(10L, TimeUnit.SECONDS);
	}

	private void announce()
	{
		try(Connection connection = ChocoBot.getDatabase())
		{
			DatabaseUtils.increaseStat(connection, sponsor.getIdLong(), guild.getIdLong(), "game."+getName().toLowerCase()+".sponsored", 1);
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		this.players = new ArrayList<>();
		this.players.add(this.sponsor);
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(ChocoBot.COLOR_GAME);
		eb.setTitle("Spiele-Ansage");
		eb.setDescription("In wenigen Sekunden beginnt das Spiel " + this.getName() + "!");
		eb.setFooter("Reagiere mit ✅ um beizutreten!");
		this.gameChannel.sendMessage(eb.build()).queue((s) ->
		{
			this.announceMessage = s;
			this.state = GameState.ANNOUNCE;
			this.announceMessage.addReaction("✅").queue();
			this.announceMessage.delete().queueAfter(10L, TimeUnit.SECONDS, (v) ->
			{
				this.logger.info("Started game {}({}) with {} player(s).", this.getName(), this.hashCode(), this.players.size());

				try(Connection connection = ChocoBot.getDatabase())
				{
					for(Member player : players)
					{
						DatabaseUtils.increaseStat(connection, player.getIdLong(), guild
								.getIdLong(), "game."+getName().toLowerCase()+".played", 1);
					}
				}
				catch(SQLException throwables)
				{
					throwables.printStackTrace();
				}

				this.play();
			});
		});
	}

	void end()
	{
		this.cleanup();
		this.logger.info("Finished game {}({}).", this.getName(), this.hashCode());
	}

	private void cleanup()
	{
		this.state = GameState.FINISHED;
		this.gameChannel.getJDA().removeEventListener(this);
	}

	protected abstract void play();

	public abstract String getName();

	protected abstract int getSponsorCost();

	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
	{
		if (!event.getUser().isBot())
		{
			if (this.state == GameState.CONFIRM && event.getMessageIdLong() == this.confirmMessage.getIdLong() && event.getUser().getIdLong() == this.sponsor.getIdLong() && event.getReactionEmote().getEmoji().equals("✅"))
			{
				this.logger.info("Announced game {}({}) by {}({}).", this.getName(), this.hashCode(), this.sponsor.getUser().getAsTag(), this.sponsor.getUser().getId());
				this.confirmMessage.delete().queue();
				DatabaseUtils.changeCoins(this.sponsor.getUser().getIdLong(), guild.getIdLong(), -this.getSponsorCost());
				this.announce();
			}

			if (this.state == GameState.ANNOUNCE && event.getMessageIdLong() == this.announceMessage.getIdLong() && !this.players.contains(event.getMember()))
			{
				this.players.add(event.getMember());
			}

		}
	}

	public void onMessageReactionRemove(@Nonnull MessageReactionRemoveEvent event)
	{
		if (this.state == GameState.ANNOUNCE && event.getMessageIdLong() == this.announceMessage.getIdLong())
		{
			this.players.remove(event.getMember());
		}
	}
}
