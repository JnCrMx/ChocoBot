package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

public class CommandCredits extends Command
{
	public CommandCredits()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Credits");
		builder.setColor(ChocoBot.COLOR_COOKIE);
		builder.addField("für", ChocoBot.provideUser(443141932714033192L, UserData::getTag, "ChocoKeks"), false);
		builder.addField("zu", "Weihnachten 2019", false);
		channel.sendMessage(builder.build()).queue();
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
}
