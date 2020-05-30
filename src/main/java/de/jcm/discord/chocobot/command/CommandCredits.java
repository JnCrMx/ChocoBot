package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public class CommandCredits extends Command
{
	public CommandCredits()
	{
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Credits");
		builder.setColor(ChocoBot.COLOR_COOKIE);
		builder.addField("für", Objects.requireNonNull(channel.getJDA().getUserById(443141932714033192L)).getAsTag(), false);
		builder.addField("zu", "Weihnachten 2019", false);
		sendTempMessage(channel, builder.build());
		return true;
	}

	@NotNull
	public String getKeyword()
	{
		return "credits";
	}

	public String getHelpText()
	{
		return "Zeige die Credits an.";
	}

	@Override
	protected String getUsage()
	{
		return "%c : %h";
	}

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
