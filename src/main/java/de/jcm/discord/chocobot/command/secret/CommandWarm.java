package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandWarm extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (args.length < 1 || message.getMentionedUsers().isEmpty())
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.secret.warm.error.who")).queue();
			return false;
		}

		User warmer = message.getAuthor();
		User warmed = message.getMentionedUsers().get(0);
		boolean self = warmer.getIdLong() == warmed.getIdLong();

		double temperature = Math.random()*50.0;
		if(args.length == 2)
		{
			try
			{
				temperature = Double.parseDouble(args[1].replace("°C", ""));
			}
			catch(NumberFormatException ignored)
			{
			}
		}

		if(temperature < 0)
		{
			if(self)
			{
				channel.sendMessage(settings.translate("command.secret.warm.cool.self", warmer.getAsMention(), temperature)).queue();
			}
			else
			{
				channel.sendMessage(settings.translate("command.secret.warm.cool.other", warmer.getAsMention(), warmed.getAsMention(), temperature)).queue();
			}
		}
		else if(temperature < 50.0)
		{
			if(self)
			{
				channel.sendMessage(settings.translate("command.secret.warm.warm.self", warmer.getAsMention(), temperature)).queue();
			}
			else
			{
				channel.sendMessage(settings.translate("command.secret.warm.warm.other", warmer.getAsMention(), warmed.getAsMention(), temperature)).queue();
			}
		}
		else
		{
			if(self)
			{
				channel.sendMessage(settings.translate("command.secret.warm.hot.self", warmer.getAsMention(), temperature)).queue();
			}
			else
			{
				channel.sendMessage(settings.translate("command.secret.warm.hot.other", warmer.getAsMention(), warmed.getAsMention(), temperature)).queue();
			}
		}

		return true;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "warm";
	}

	@Override
	protected @Nullable String getHelpText(GuildSettings settings)
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
