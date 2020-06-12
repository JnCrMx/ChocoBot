package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandChii extends Command
{
	public CommandChii()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		channel.sendMessage("Hideki!").queue();
		return true;
	}

	@NotNull
	public String getKeyword()
	{
		return "chii";
	}

	@Nullable
	public String getHelpText()
	{
		return null;
	}

	@Override
	protected @Nullable String getUsage()
	{
		return null;
	}

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
