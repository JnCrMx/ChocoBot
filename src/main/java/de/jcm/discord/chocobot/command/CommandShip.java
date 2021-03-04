package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandShip extends Command
{
	private static final String EMOJI_NULL = ":broken_heart:";
	private static final String EMOJI_FULL = ":cupid:";
	private static final String EMOJI_69 = "lmao";
	private static final String[] EMOJIS = new String[]{":black_heart:", ":heart_decoration:", ":hearts:", ":heart:", ":heartbeat:", ":two_hearts:", ":revolving_hearts:", ":heartpulse:", ":sparkling_heart:", ":gift_heart:"};
	private static MessageDigest sha256;

	public CommandShip()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if (!message.getMentionedRoles().isEmpty())
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.ship.error.roles")).queue();
			return false;
		}
		else if (message.getContentRaw().contains("@everyone") ||
				message.getContentRaw().contains("@here"))
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.ship.error.many")).queue();
			return false;
		}
		else
		{
			boolean w = false;
			StringBuilder word1 = new StringBuilder(args[0]);
			StringBuilder word2 = new StringBuilder();
			if (args.length > 2)
			{
				for (int i = 1; i < args.length; ++i)
				{
					String arg = args[i];
					if (!arg.isBlank())
					{
						if (arg.equals("x") && !w)
						{
							w = true;
						}
						else if (!w)
						{
							word1.append(" ").append(arg);
						}
						else if (word2.length() == 0)
						{
							word2.append(arg);
						}
						else
						{
							word2.append(" ").append(arg);
						}
					}
				}
			}
			else if (args.length == 2)
			{
				word2.append(args[1]);
			}

			word1.trimToSize();
			word2.trimToSize();
			String w1 = solveMentions(word1.toString());
			String w2 = solveMentions(word2.toString());
			if (w1.length() != 0 && w2.length() != 0)
			{
				if (w1.equals(w2))
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.ship.error.self", w1)).queue();
					return false;
				}
				else
				{
					int percent = calculateShip(w1, w2);
					EmbedBuilder builder = new EmbedBuilder();
					builder.setColor(ChocoBot.COLOR_LOVE);
					builder.setTitle(w1 + " x " + w2);
					StringBuilder dBuilder = builder.getDescriptionBuilder();
					String emoji;
					if (percent == 0)
					{
						emoji = EMOJI_NULL;
					}
					else if (percent == 100)
					{
						emoji = EMOJI_FULL;
					}
					else
					{
						List<Emote> emotes;
						if (percent == 69 && !(emotes = channel.getGuild().getEmotesByName(EMOJI_69, false)).isEmpty())
						{
							emoji = emotes.get(0).getAsMention();
						}
						else
						{
							emoji = EMOJIS[(int) Math.floor((double) EMOJIS.length / 100.0D * (double) (percent - 1))];
						}
					}

					dBuilder.append(emoji);
					dBuilder.append(" ");
					dBuilder.append(percent);
					dBuilder.append("%");
					dBuilder.append(" ");
					dBuilder.append(emoji);
					channel.sendMessage(builder.build()).queue();
					return true;
				}
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.ship.error.narg")).queue();
				return false;
			}
		}
	}

	public static int calculateShip(String word1, String word2)
	{
		byte[] hash1 = sha256.digest(word1.toLowerCase().getBytes());
		byte[] hash2 = sha256.digest(word2.toLowerCase().getBytes());

		byte finalByte = 0;
		for (int i = 0; i < hash1.length; ++i)
		{
			finalByte ^= hash1[i];
			finalByte ^= hash2[i];
		}

		double first = Byte.toUnsignedInt(finalByte);
		return (int) (first * 100.0D / 255.0D);
	}

	private static String solveMentions(String string)
	{
		Pattern pattern = Pattern.compile("<@(|!)([0-9]*)>");
		Matcher matcher = pattern.matcher(string);
		return matcher.replaceAll((r) -> ChocoBot.provideUser(matcher.group(2), UserData::getName, "Unknown user"));
	}

	@NotNull
	public String getKeyword()
	{
		return "ship";
	}

	static
	{
		try
		{
			sha256 = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException var1)
		{
			var1.printStackTrace();
		}
	}
}
