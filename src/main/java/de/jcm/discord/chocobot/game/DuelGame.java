package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public abstract class DuelGame extends ListenerAdapter
{
	private TextChannel channel;
	private Member challenger, challengee;

	private Message challengeMessage;

	DuelGame(Member challenger, Member challengee, TextChannel channel)
	{
		this.channel = channel;

		this.challenger = challenger;
		this.challengee = challengee;
	}

	public void start()
	{

	}

	private void challenge()
	{
		String message = String.format("%s, %s möchte dich zu %s herausfordern.\n" +
				                               "Reagiere mit :white_check_mark: um anzunehmen.",
		                               challengee.getAsMention(), challenger.getNickname(), getName());
		channel.sendMessage(message).queue(s->{
			challengeMessage = s;

			channel.getJDA().addEventListener(this);

			challengeMessage.addReaction("✅").queue();
			challengeMessage.delete().queueAfter(10L, TimeUnit.SECONDS, v->{
				channel.getJDA().removeEventListener(this);
				cancel();
			});
		});
	}

	private void cancel()
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_GAME);
		builder.setTitle("Abgebrochen!");
		builder.setDescription(challenger.getAsMention() + " Deine Herausforderung wurde nicht angenommen!");
		builder.setColor(ChocoBot.COLOR_ERROR);
		channel.sendMessage(builder.build()).queueAfter(10L, TimeUnit.SECONDS);
	}

	public abstract String getName();
}
