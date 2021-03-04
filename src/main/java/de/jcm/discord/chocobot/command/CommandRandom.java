package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CommandRandom extends Command
{
	private final Random random = new Random();

	public CommandRandom()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (!message.getMentionedUsers().isEmpty())
		{
			List<User> users = message.getMentionedUsers();
			User user = users.get(this.random.nextInt(users.size()));
			channel.sendMessage(settings.translate("command.random.user", user.getAsMention())).queue();
			return true;
		}
		else
		{
			if (!message.getMentionedRoles().isEmpty())
			{
				List<Role> roles = message.getMentionedRoles();

				guild.findMembers(m-> Collections.disjoint(m.getRoles(), roles)).onSuccess(members->{
					Member member = members.get(this.random.nextInt(members.size()));
					channel.sendMessage(settings.translate("command.random.user", member.getAsMention())).queue();
				});
				return true;
			}
			else if (args[0].equals("@everyone") || args[0].equals("@here"))
			{
				boolean here = args[0].equals("@here");
				guild.findMembers(m-> !here || m.getOnlineStatus() != OnlineStatus.OFFLINE)
				     .onSuccess(members->{
					     Member member = members.get(this.random.nextInt(members.size()));
					     channel.sendMessage(settings.translate("command.random.user", member.getAsMention())).queue();
				     });
				return true;
			}
			else
			{
				if(args.length==2)
				{
					try
					{
						BigInteger start = new BigInteger(args[0]);
						BigInteger end = new BigInteger(args[1]);

						if(start.compareTo(end) > 0)
						{
							BigInteger a = end;
							end = start;
							start = a;
						}

						BigInteger integer;
						do
						{
							integer = new BigInteger(end.subtract(start).bitLength(), random).add(start);
						}
						while(integer.compareTo(end)>0);

						channel.sendMessage(settings.translate("command.random.number", integer.toString())).queue();
						return true;
					}
					catch (NumberFormatException ignored)
					{

					}
				}

				if (args.length >= 2)
				{
					String word = args[this.random.nextInt(args.length)];

					if(word.equals("@everyone") || word.equals("@here"))
					{
						if(!settings.isOperator(Objects.requireNonNull(message.getMember())))
						{
							channel.sendMessage(ChocoBot.translateError(settings, "command.random.error.perm")).queue();
							return false;
						}
					}

					channel.sendMessage(settings.translate("command.random.word", word)).queue();
					return true;
				}
				else
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.random.error.unsupported")).queue();
					return false;
				}
			}
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "random";
	}
}
