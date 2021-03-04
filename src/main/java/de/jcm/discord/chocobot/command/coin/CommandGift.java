package de.jcm.discord.chocobot.command.coin;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class CommandGift extends Command
{
	public CommandGift()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (args.length < 2)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.narg")).queue();
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
						channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.negative")).queue();
						return false;
					}
					else if (amount == 0)
					{
						channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.zero")).queue();
						return false;
					}
					else
					{
						Member srcMember = message.getMember();
						Member dstMember = message.getMentionedMembers().get(0);

						assert srcMember != null;

						long src = srcMember.getIdLong();
						long dst = dstMember.getIdLong();
						if (DatabaseUtils.getCoins(src, guild.getIdLong()) < amount)
						{
							channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.not_enough")).queue();
							return false;
						}
						else
						{
							if (!dstMember.getUser().isBot())
							{
								DatabaseUtils.changeCoins(src, guild.getIdLong(), -amount);
								DatabaseUtils.changeCoins(dst, guild.getIdLong(), amount);
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
					channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.nan")).queue();
					return false;
				}
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.gift.error.who")).queue();
				return false;
			}
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "gift";
	}
}
