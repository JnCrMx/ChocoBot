package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public class CommandWarm extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if (args.length < 1 || message.getMentionedUsers().isEmpty())
		{
			sendTempMessage(channel, ChocoBot.errorMessage("Du musst mir schon sagen, wen du erwärmen willst!"));
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
				channel.sendMessage(String.format(
						"%s ist ganz schön kühl und bräuchte mal eine Um(w)armung!",
						warmer.getAsMention())).queue();
			}
			else
			{
				channel.sendMessage(String.format(
						"%s findet %s super cool!",
						warmer.getAsMention(), warmed.getAsMention())).queue();
			}
		}
		else if(temperature < 50.0)
		{
			if(self)
			{
				channel.sendMessage(String.format(
						"%s braucht dringend jemanden, um mit ihr/ihm zu kuscheln und sie/ihn um %.2f°C zu erwärmen!",
						warmer.getAsMention(), temperature)).queue();
			}
			else
			{
				channel.sendMessage(String.format(
						"%s kuschelt sich an %s und wärmt sie/ihn somit um %.2f°C :revolving_hearts:",
						warmer.getAsMention(), warmed.getAsMention(), temperature)).queue();
			}
		}
		else
		{
			if(self)
			{
				channel.sendMessage(String.format(
						"Nicht auf die Herdplatte fassen, %s, die ist ganz schön heiß: %.2f°C :fire:",
						warmer.getAsMention(), temperature)).queue();
			}
			else
			{
				channel.sendMessage(String.format(
						"%s findet %s ultra heiß! :fire:",
						warmer.getAsMention(), warmed.getAsMention())).queue();
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
	protected @Nullable String getHelpText()
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
