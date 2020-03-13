package de.jcm.discord.chocobot.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandSay extends Command
{
	public CommandSay()
	{
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		String msg = args[0];
		TextChannel textChannel = channel;
		if (!message.getMentionedChannels().isEmpty())
		{
			TextChannel mentionedChannel = message.getMentionedChannels().get(0);
			if (msg.startsWith(mentionedChannel.getAsMention()))
			{
				msg = msg.replaceFirst(mentionedChannel.getAsMention(), "").trim();
				textChannel = mentionedChannel;
			}
		}

		textChannel.sendMessage(msg).queue();
		message.delete().queue();
		return true;
	}

	@NotNull
	public String getKeyword()
	{
		return "say";
	}

	public String getHelpText()
	{
		return "Sage etwas in einem Channel.";
	}

	public boolean multipleArguments()
	{
		return false;
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c <Nachricht> : Sage etwas in diesem Kanal.\n" +
				"%c <Kanal> <Nachricht> : Sage etwas in einem anderen Kanal.";
	}
}
