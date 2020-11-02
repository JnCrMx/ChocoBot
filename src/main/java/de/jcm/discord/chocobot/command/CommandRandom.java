package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

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
			channel.sendMessage("Es ist: " + user.getAsMention()).queue();
			return true;
		}
		else
		{
			if (!message.getMentionedRoles().isEmpty())
			{
				List<Role> roles = message.getMentionedRoles();

				guild.findMembers(m-> Collections.disjoint(m.getRoles(), roles)).onSuccess(members->{
					Member member = members.get(this.random.nextInt(members.size()));
					channel.sendMessage("Es ist: " + member.getAsMention()).queue();
				});
				return true;
			}
			else if (args[0].equals("@everyone") || args[0].equals("@here"))
			{
				boolean here = args[0].equals("@here");
				guild.findMembers(m-> !here || m.getOnlineStatus() != OnlineStatus.OFFLINE)
				     .onSuccess(members->{
					     Member member = members.get(this.random.nextInt(members.size()));
					     channel.sendMessage("Es ist: " + member.getAsMention()).queue();
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

						channel.sendMessage("Es ist die "+integer.toString()+"!").queue();
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
							channel.sendMessage(ChocoBot.errorMessage("Nein, ich werde das nicht tun!")).queue();
							return false;
						}
					}

					channel.sendMessage("Es ist: " + word).queue();
					return true;
				}
				else
				{
					channel.sendMessage(ChocoBot.errorMessage("Diese Art des Zufalls ist momentan nicht unterstützt!")).queue();
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

	@Nullable
	public String getHelpText()
	{
		return "Nutze verschiedene Zufallsfunktionen.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c <Nutzer> [<Nutzer> ...] : Finde einen zufälligen Nutzer aus den angegebenen Nutzern.\n" +
				"%c <Rolle> [<Rolle> ...] : Finde einen zufälligen Nutzer aus den angegebenen Rollen.\n" +
				"%c @everyone : Finde einen zufälligen Nutzer von diesem Server.\n" +
				"%c @here : Finde einen zufälligen Nutzer von diesem Server, der online ist.\n" +
				"%c <Start> <Ende> : Zeige eine Zufallszahl zwischen <Start> und <Ende> an.\n" +
				"%c <Wort> <Wort> [<Wort> ...] : Wähle zufällig ein Wort aus.";
	}
}
