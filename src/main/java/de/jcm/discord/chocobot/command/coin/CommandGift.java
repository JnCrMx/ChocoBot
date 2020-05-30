package de.jcm.discord.chocobot.command.coin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public class CommandGift extends Command
{
	public CommandGift()
	{
	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if (args.length < 2)
		{
			sendTempMessage(channel, ChocoBot.errorMessage("Du musst mir schon sagen wem du wie viele Coins schenken möchtest!"));
			return false;
		}
		else
		{
			Pattern pattern = Pattern.compile("<@(|!)([0-9]*)>");
			if (!message.getMentionedMembers().isEmpty() && pattern.matcher(args[0]).matches())
			{
				try
				{
					int amount = Integer.parseInt(args[1]);
					if (amount < 0)
					{
						sendTempMessage(channel, ChocoBot.errorMessage("Das wäre Diebstahl!"));
						return false;
					}
					else if (amount == 0)
					{
						sendTempMessage(channel, ChocoBot.errorMessage("Du bist aber geizig!"));
						return false;
					}
					else
					{
						Member srcMember = message.getMember();
						Member dstMember = message.getMentionedMembers().get(0);

						assert srcMember != null;

						long src = srcMember.getIdLong();
						long dst = dstMember.getIdLong();
						if (DatabaseUtils.getCoins(src) < amount)
						{
							sendTempMessage(channel, ChocoBot.errorMessage("Du kannst nicht mehr verschenken als du selbst hast!"));
							return false;
						}
						else
						{
							if (!dstMember.getUser().isBot())
							{
								DatabaseUtils.changeCoins(src, -amount);
								DatabaseUtils.changeCoins(dst, amount);
							}

							EmbedBuilder builder = new EmbedBuilder();
							builder.setTitle("Geschenk");
							builder.setColor(ChocoBot.COLOR_COINS);
							String var10001 = srcMember.getEffectiveName();
							builder.setDescription(var10001 + " hat " + dstMember.getEffectiveName() + " " + amount + " " + (amount == 1 ? "Coin" : "Coins") + " geschenkt!");
							channel.sendMessage(builder.build()).queue();
							if (dstMember.getIdLong() == ChocoBot.jda.getSelfUser().getIdLong())
							{
								channel.sendMessage("Danke! :heart:").queue();
							}

							return true;
						}
					}
				}
				catch (NumberFormatException var13)
				{
					sendTempMessage(channel, ChocoBot.errorMessage("Ich kann leider nicht verstehen, wie viele Coins du verschenken willst."));
					return false;
				}
			}
			else
			{
				sendTempMessage(channel, ChocoBot.errorMessage("Ich kann leider nicht verstehen, wem du Coins schenken willst."));
				return false;
			}
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "gift";
	}

	@Nullable
	public String getHelpText()
	{
		return "Schenke einem Nutzer Coins.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c <Nutzer> <Anzahl> : %h";
	}
}
