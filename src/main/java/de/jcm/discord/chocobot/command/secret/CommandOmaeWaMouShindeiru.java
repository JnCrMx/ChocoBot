package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandOmaeWaMouShindeiru extends Command
{
	public CommandOmaeWaMouShindeiru()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (args.length != 3)
		{
			return false;
		}
		else if (!args[0].equalsIgnoreCase("wa"))
		{
			return false;
		}
		else if (!args[1].equalsIgnoreCase("mou"))
		{
			return false;
		}
		else if (!args[2].equalsIgnoreCase("shindeiru"))
		{
			return false;
		}
		else
		{
			channel.sendMessage(settings.translate("command.omae.secret.message")).queue();
			return true;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "omae";
	}

	public String getHelpText(GuildSettings settings)
	{
		return null;
	}

	@Override
	protected @Nullable String getUsage(GuildSettings settings)
	{
		return null;
	}

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
