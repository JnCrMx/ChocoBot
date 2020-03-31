package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

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

		if(Pattern.compile("@\\S*").matcher(msg).find())
		{
			if (Objects.requireNonNull(message.getMember())
			           .getRoles().stream().noneMatch((r) -> ChocoBot.operatorRoles.contains(r.getId())))
			{
				channel.sendMessage(ChocoBot.errorMessage("Ich erwähne niemanden für dich!")).queue();
				return false;
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
