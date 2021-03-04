package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandPride extends Command
{
	private final static Map<String, List<Color>> PRIDE_FLAGS = new HashMap<>();
	static
	{
		PRIDE_FLAGS.put("lgbt", Arrays.asList(
				new Color(0xe30303),
				new Color(0xff8b00),
				new Color(0xfbee01),
				new Color(0x008026),
				new Color(0x0007ff),
				new Color(0x750686)));
		PRIDE_FLAGS.put("pride", PRIDE_FLAGS.get("lgbt"));

		PRIDE_FLAGS.put("gay", PRIDE_FLAGS.get("lgbt"));
		PRIDE_FLAGS.put("schwul", PRIDE_FLAGS.get("gay"));

		PRIDE_FLAGS.put("lesbian", Arrays.asList(
				new Color(0xd62900),
				new Color(0xff9b55),
				new Color(0xffffff),
				new Color(0xd462a6),
				new Color(0xa50062)));
		PRIDE_FLAGS.put("lesbisch", PRIDE_FLAGS.get("lesbian"));

		PRIDE_FLAGS.put("bisexual", Arrays.asList(
				new Color(0xd70071),
				new Color(0xd70071),
				new Color(0x9c4e97),
				new Color(0x0035aa),
				new Color(0x0035aa)));
		PRIDE_FLAGS.put("bi", PRIDE_FLAGS.get("bisexual"));
		PRIDE_FLAGS.put("bisexuell", PRIDE_FLAGS.get("bisexual"));

		PRIDE_FLAGS.put("trans", Arrays.asList(
				new Color(0x5bcef5),
				new Color(0xf5a8b4),
				new Color(0xffffff),
				new Color(0xf5a8b4),
				new Color(0x5bcef5)));
		PRIDE_FLAGS.put("transsexual", PRIDE_FLAGS.get("trans"));
		PRIDE_FLAGS.put("transsexuell", PRIDE_FLAGS.get("trans"));
		PRIDE_FLAGS.put("transgender", PRIDE_FLAGS.get("trans"));

		PRIDE_FLAGS.put("non-binary", Arrays.asList(
				new Color(0xfcf434),
				new Color(0xfcfcfc),
				new Color(0x9c59d1),
				new Color(0x2c2c2c)));
		PRIDE_FLAGS.put("enby", PRIDE_FLAGS.get("non-binary"));

		PRIDE_FLAGS.put("pansexual", Arrays.asList(
				new Color(0xff1b8d),
				new Color(0xffd900),
				new Color(0x1bb3ff)));
		PRIDE_FLAGS.put("pan", PRIDE_FLAGS.get("pansexual"));
		PRIDE_FLAGS.put("pansexuell", PRIDE_FLAGS.get("pansexual"));

		PRIDE_FLAGS.put("asexual", Arrays.asList(
				new Color(0x070707),
				new Color(0xa2a2a2),
				new Color(0xffffff),
				new Color(0x7e287f)));
		PRIDE_FLAGS.put("ace", PRIDE_FLAGS.get("asexual"));
		PRIDE_FLAGS.put("asexuell", PRIDE_FLAGS.get("asexual"));

		PRIDE_FLAGS.put("aromatic", Arrays.asList(
				new Color(0x3ca542),
				new Color(0xa8d377),
				new Color(0xfefefe),
				new Color(0xa9a9a9),
				new Color(0x000000)));
		PRIDE_FLAGS.put("aro", PRIDE_FLAGS.get("aromatic"));
		PRIDE_FLAGS.put("aromatisch", PRIDE_FLAGS.get("aromatic"));

		PRIDE_FLAGS.put("graysexual", Arrays.asList(
				new Color(0x740395),
				new Color(0xaeb2aa),
				new Color(0xffffff),
				new Color(0xaeb2aa),
				new Color(0x740395)));
		PRIDE_FLAGS.put("greysexual", PRIDE_FLAGS.get("graysexual"));
		PRIDE_FLAGS.put("gray", PRIDE_FLAGS.get("graysexual"));
		PRIDE_FLAGS.put("grey", PRIDE_FLAGS.get("graysexual"));

		PRIDE_FLAGS.put("aroace", Arrays.asList(
				new Color(0xe38d00),
				new Color(0xedce00),
				new Color(0xffffff),
				new Color(0x62b0dd),
				new Color(0x1a3555)));
		PRIDE_FLAGS.put("aro-ace", PRIDE_FLAGS.get("aroace"));

		PRIDE_FLAGS.put("genderqueer", Arrays.asList(
				new Color(0xb498c8),
				new Color(0xffffff),
				new Color(0x6e903c)));
		PRIDE_FLAGS.put("queer", PRIDE_FLAGS.get("genderqueer"));
	}

	public static void registerAll()
	{
		for(String thing : PRIDE_FLAGS.keySet())
		{
			Command.registerCommand(new CommandPride(thing));
		}
	}

	private final String thingy;

	private CommandPride(String thingy)
	{
		this.thingy = thingy;
	}

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		try
		{
			BufferedImage avatar = ImageIO.read(new URL(message.getAuthor().getEffectiveAvatarUrl()));
			Graphics2D g2d = (Graphics2D) avatar.getGraphics();

			List<Color> colors = PRIDE_FLAGS.get(thingy);
			int height = (int) Math.round(((double)avatar.getHeight() / colors.size()));
			for(int i=0; i<colors.size(); i++)
			{
				int y = i*height;
				Color c = colors.get(i);
				g2d.setColor(new Color(
						c.getRed(), c.getGreen(), c.getBlue(), 127));
				g2d.fillRect(0, y, avatar.getWidth(),
				             i==colors.size()-1?avatar.getHeight()-y:height);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(avatar, "png", out);
			channel.sendFile(out.toByteArray(), thingy+".png").queue();

			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return false;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return thingy;
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
}
