package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class CommandSay extends Command
{
	public CommandSay()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
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

		Member targetMember = textChannel.getGuild().retrieveMember(message.getAuthor()).complete();
		if(targetMember == null)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.say.error.server")).queue();
			return false;
		}

		if(!targetMember.hasPermission(textChannel, Permission.MESSAGE_WRITE))
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.say.error.channel_perm")).queue();
			return false;
		}

		GuildSettings targetSettings = DatabaseUtils.getSettings(textChannel.getGuild());
		if(Pattern.compile("@\\S*").matcher(msg).find())
		{
			if(!targetSettings.isOperator(targetMember))
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.say.error.mention")).queue();
				return false;
			}
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setDescription(msg);

		textChannel.sendMessage(eb.build()).queue();
		message.delete().queue();
		return true;
	}

	@NotNull
	public String getKeyword()
	{
		return "say";
	}

	public boolean multipleArguments()
	{
		return false;
	}
}
