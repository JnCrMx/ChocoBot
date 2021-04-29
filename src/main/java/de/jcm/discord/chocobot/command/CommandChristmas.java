package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandChristmas extends Command
{
	private static final int BOT_GIFT_AMOUNT = 250;

	private static class ChristmasGift
	{
		private final int id;
		private final long sender;
		private final boolean opened;

		ChristmasGift(int id, long sender, boolean opened)
		{
			this.id = id;
			this.sender = sender;
			this.opened = opened;
		}

		int getId()
		{
			return id;
		}

		long getSender()
		{
			return sender;
		}

		boolean isOpened()
		{
			return opened;
		}
	}

	private static class GiftImage
	{
		final String image;
		final boolean opened;

		final float textSize;
		final double textRotation;
		final int textX;
		final int textY;

		final int avatarX;
		final int avatarY;
		final int avatarWidth;
		final int avatarHeight;

		GiftImage(String image, boolean opened,
		                 float textSize, double textRotation,
		                 int textX, int textY,
		                 int avatarX, int avatarY,
		                 int avatarWidth, int avatarHeight)
		{
			this.image = image;
			this.opened = opened;
			this.textSize = textSize;
			this.textRotation = textRotation;
			this.textX = textX;
			this.textY = textY;
			this.avatarX = avatarX;
			this.avatarY = avatarY;
			this.avatarWidth = avatarWidth;
			this.avatarHeight = avatarHeight;
		}
	}

	private static final GiftImage[] GIFT_IMAGES = {
			new GiftImage("/gift1.png", false,
			              32f, 25.0,
			              24, 22,
			              128, 108,
			              96, 96)
	};
	private static final Point[] GIFT_POSITIONS = {
			new Point(10, 10),
			new Point(10, 200)
	};
	private static final Random RNG = new Random();

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		LocalDateTime now = LocalDateTime.now();
		if(now.getMonth() != Month.DECEMBER)
		{
			return false; // no message here :P
		}
		if(now.getDayOfMonth() <= 23)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.early")).queue();
			return false;
		}

		long uid = message.getAuthor().getIdLong();

		if(args.length == 0)
		{
			List<ChristmasGift> gifts = new ArrayList<>();

			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement statement = connection.prepareStatement("SELECT id, sender, opened FROM christmas_presents WHERE uid=? AND guild=? AND (year = ? OR opened=0)");
			    PreparedStatement botGift = connection.prepareStatement("INSERT INTO christmas_presents(uid, sender, guild, amount, message, year) VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS))
			{
				statement.setLong(1, uid);
				statement.setLong(2, guild.getIdLong());
				statement.setInt(3, now.getYear());
				ResultSet result = statement.executeQuery();
				while(result.next())
				{
					int id = result.getInt("id");
					long sender = result.getLong("sender");
					boolean opened = result.getBoolean("opened");

					gifts.add(new ChristmasGift(id, sender, opened));
				}

				if(gifts.stream().noneMatch(g->g.getSender() == ChocoBot.jda.getSelfUser().getIdLong()))
				{
					botGift.setLong(1, uid);
					botGift.setLong(2, ChocoBot.jda.getSelfUser().getIdLong());
					botGift.setLong(3, guild.getIdLong());
					botGift.setInt(4, BOT_GIFT_AMOUNT);
					botGift.setString(5, settings.translate("command.christmas.bot_message", message.getAuthor().getAsTag()));
					botGift.setInt(6, now.getYear());
					botGift.executeUpdate();

					ResultSet keys = botGift.getGeneratedKeys();
					if(keys.next())
					{
						gifts.add(new ChristmasGift(keys.getInt(1),
						                            ChocoBot.jda.getSelfUser().getIdLong(),
						                            false));
					}
					else
					{
						channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.general")).queue();
					}
				}
			}
			catch(SQLException throwables)
			{
				throwables.printStackTrace();
				channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.general")).queue();
				return false;
			}

			try
			{
				BufferedImage christmasTree = ImageIO.read(getClass().getResourceAsStream("/christmas-tree.png"));
				Graphics2D g = christmasTree.createGraphics();

				List<Point> positions = new ArrayList<>(Arrays.asList(GIFT_POSITIONS));

				// first unopened, then opened
				List<ChristmasGift> giftList = gifts.stream().filter(Predicate.not(ChristmasGift::isOpened)).collect(Collectors.toList());
				List<ChristmasGift> opened = gifts.stream().filter(ChristmasGift::isOpened).collect(Collectors.toList());

				Collections.shuffle(giftList);
				Collections.shuffle(opened);
				giftList.addAll(opened);

				for(ChristmasGift gift : giftList)
				{
					if(positions.isEmpty())
						break;

					GiftImage[] availableImages = Stream.of(GIFT_IMAGES)
					                                    .filter(e->e.opened == gift.isOpened())
														.toArray(GiftImage[]::new);
					if(availableImages.length == 0)
						continue;
					GiftImage gi = availableImages[RNG.nextInt(availableImages.length)];

					Point position = positions.get(RNG.nextInt(positions.size()));
					positions.remove(position);

					Graphics2D subG = (Graphics2D) g.create();
					subG.translate(position.x, position.y);

					BufferedImage giftImage = ImageIO.read(getClass().getResourceAsStream(gi.image));
					subG.drawImage(giftImage, 0, 0, null);

					Graphics2D textG = (Graphics2D) subG.create();
					textG.setColor(Color.BLACK);
					textG.setFont(textG.getFont().deriveFont(gi.textSize));
					textG.rotate(Math.toRadians(gi.textRotation));
					textG.drawString(Integer.toString(gift.getId()), gi.textX, gi.textY);

					Graphics2D avatarG = (Graphics2D) subG.create();
					avatarG.setClip(new Ellipse2D.Double(gi.avatarX-gi.avatarWidth/2f,
					                                     gi.avatarY-gi.avatarHeight/2f,
					                                     gi.avatarWidth, gi.avatarHeight));

					UserData sender = ChocoBot.provideUser(gift.getSender());
					BufferedImage avatar = ImageIO.read(new URL(sender.getAvatarUrl()));

					avatarG.drawImage(avatar,
					                  gi.avatarX-gi.avatarWidth/2,
					                  gi.avatarY-gi.avatarHeight/2,
					                  gi.avatarWidth, gi.avatarHeight, null);
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(christmasTree, "png", out);
				channel.sendFile(out.toByteArray(), "christmas-tree.png").queue();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		else if(args.length == 1)
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement statement = connection.prepareStatement("SELECT sender, amount, message, opened FROM christmas_presents WHERE id=? AND uid=? AND guild=?");
			    PreparedStatement openStatement = connection.prepareStatement("UPDATE christmas_presents SET opened=1 WHERE id=? AND uid=? AND guild=?"))
			{
				int id = Integer.parseInt(args[0]);

				statement.setInt(1, id);
				statement.setLong(2, uid);
				statement.setLong(3, guild.getIdLong());
				ResultSet result = statement.executeQuery();
				if(result.next())
				{
					long senderId = result.getLong("sender");
					int amount = result.getInt("amount");
					String textMessage = result.getString("message");
					boolean opened = result.getBoolean("opened");

					if(opened)
					{
						channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.opened")).queue();
						return false;
					}

					Member sender = guild.retrieveMemberById(senderId).complete();

					DatabaseUtils.changeCoins(connection, uid, guild.getIdLong(), amount);

					openStatement.setInt(1, id);
					openStatement.setLong(2, uid);
					openStatement.setLong(3, guild.getIdLong());
					openStatement.executeUpdate();

					EmbedBuilder builder = new EmbedBuilder();
					builder.setTitle(settings.translate("command.christmas.title"));
					builder.setColor(ChocoBot.COLOR_COOKIE);
					String description = settings.translate("command.christmas.message", sender.getEffectiveName(), amount);
					if(textMessage != null)
						description += "\n\n"+textMessage;
					builder.setDescription(description);
					builder.setFooter(sender.getUser().getAsTag(), sender.getUser().getAvatarUrl());
					channel.sendMessage(builder.build()).queue();
				}
				else
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.noent")).queue();
					return false;
				}
			}
			catch(NumberFormatException var13)
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.fmt")).queue();
				return false;
			}
			catch(SQLException throwables)
			{
				throwables.printStackTrace();
			}
		}
		else
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.christmas.error.narg")).queue();
			return false;
		}

		return false;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "christmas";
	}

	@Override
	protected @Nullable String getHelpText(GuildSettings settings)
	{
		if(LocalDateTime.now().getMonth() != Month.DECEMBER)
			return null;
		else
			return super.getHelpText(settings);
	}

	@Override
	protected @Nullable String getUsage(GuildSettings settings)
	{
		if(LocalDateTime.now().getMonth() != Month.DECEMBER)
			return null;
		else
			return super.getUsage(settings);
	}
}
