package de.jcm.discord.chocobot.game;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WhoIsItGame extends Game
{
	private static final int REWARD = 150;
	private static final long TIMEOUT = 5000L;

	private static final HashMap<Long, List<ImmutablePair<Member, String>>> avatarCache = new HashMap<>();
	private static final HashMap<Long, Long> avatarCacheAge = new HashMap<>();
	private static final long MAX_CACHE_AGE = 60 * 60 * 1000;

	private static final Random RNG = new Random();

	private ImmutablePair<Member, String> avatar;
	private final AtomicBoolean ready = new AtomicBoolean(false);

	private Message gameMessage;
	private ScheduledFuture<?> deleteFuture;

	private final Map<Long, Long> lastGuesses = new HashMap<>();

	public WhoIsItGame(Member sponsor, TextChannel gameChannel)
	{
		super(sponsor, gameChannel);
	}

	private void provideAvatars(Guild guild, Consumer<List<ImmutablePair<Member, String>>> consumer)
	{
		if(avatarCache.containsKey(guild.getIdLong()) &&
				avatarCacheAge.get(guild.getIdLong()) + MAX_CACHE_AGE > System.currentTimeMillis())
		{
			consumer.accept(avatarCache.get(guild.getIdLong()));
		}
		else
		{
			guild.findMembers(m -> !m.getUser().isBot()).onSuccess(members -> {
				List<ImmutablePair<Member, String>> avatars =
						members.stream()
						       .filter(member -> !member.getUser().isBot())
						       .map(member -> new ImmutablePair<>(member, member.getUser().getAvatarUrl()))
						       .filter(pair -> pair.getRight() != null)
						       //.filter(pair -> pair.getRight().endsWith(".png"))
						       .collect(Collectors.toList());
				avatarCache.put(guild.getIdLong(), avatars);
				avatarCacheAge.put(guild.getIdLong(), System.currentTimeMillis());

				logger.info("Built avatar cache for guild "+guild+" with "+avatars.size()+" entries.");

				consumer.accept(avatars);
			});
		}
	}

	@Override
	protected void play()
	{
		provideAvatars(guild, avatars ->
		{
			if(avatars.size() == 0)
			{
				DatabaseUtils.changeCoins(sponsor.getIdLong(), guild.getIdLong(), getSponsorCost());

				gameChannel.sendMessage(
						ChocoBot.errorMessage(
								"Es konnten keine Profilbilder geladen werden!\n" +
										"Deine Coins wurden rückerstattet."))
				           .queue();
				return;
			}

			avatar = avatars.get(RNG.nextInt(avatars.size()));

			try
			{
				BufferedImage base = ImageIO.read(new URL(avatar.getRight()));
				BufferedImage converted = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_RGB);
				converted.getGraphics().drawImage(base, 0, 0, null);

				int type = RNG.nextInt(2);
				byte[] gif;
				switch(type)
				{
					case 0:
						gif = makePixelGIF(converted);
						break;
					case 1:
						gif = makeZoomGIF(converted);
						break;
					default:
						throw new IllegalStateException("Unexpected value: " + type);
				}

				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(ChocoBot.COLOR_COOKIE);
				builder.setTitle("Wer ist es?");
				builder.setDescription("Schreibe den Namen, die ID oder Nicknamen (*kein Ping!*) " +
						                       "des Nutzers mit diesem Profilbild, sobald du ihn erkennst!\n" +
						                       "Du jedoch kannst nur alle 5 Sekunden einen Namen schreiben.");

				gameChannel.sendMessage(builder.build()).addFile(gif, "weristes.gif").queue(msg1->{
					this.gameMessage = msg1;
					this.ready.set(true);

					EmbedBuilder builder1 = new EmbedBuilder();
					builder1.setColor(ChocoBot.COLOR_COOKIE);
					builder1.setTitle("Wer ist es?");
					builder1.setDescription("Es hat leider niemand richtig geraten! :sob:\n"+
							                        "Es war "+avatar.getLeft().getEffectiveName()+"!");
					builder1.setThumbnail(avatar.getRight());

					builder1.addField(sponsor.getEffectiveName(), "-" + getSponsorCost() + " Coins", false);

					deleteFuture = gameChannel
							.sendMessage(builder1.build())
							.queueAfter(1, TimeUnit.MINUTES, s -> {
								gameMessage.delete().queue();

								this.end();
							});
				});
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
	{
		if(!ready.get())
			return;
		if(avatar == null || avatar.getLeft() == null)
			return;
		if(event.getAuthor().isBot())
			return;
		if(event.getChannel().getIdLong() != gameChannel.getIdLong())
			return;

		long uid = event.getAuthor().getIdLong();

		if(players.stream().noneMatch(m->m.getIdLong() == uid))
			return;

		String guess = event.getMessage().getContentRaw();
		guess = guess.replace("\n", "").toLowerCase(Locale.ROOT);

		if(lastGuesses.getOrDefault(uid, 0L) + TIMEOUT > System.currentTimeMillis())
		{
			event.getMessage().addReaction("U+23F3").queue();
			return;
		}

		Member right = avatar.getLeft();
		if(guess.equals(right.getEffectiveName().toLowerCase(Locale.ROOT)) ||
				guess.equals(right.getUser().getName().toLowerCase(Locale.ROOT)) ||
				guess.equals(right.getUser().getAsTag().toLowerCase(Locale.ROOT)) ||
				guess.equals(right.getId()))
		{
			deleteFuture.cancel(true);
			event.getMessage().addReaction("U+2705").queue();

			EmbedBuilder builder1 = new EmbedBuilder();
			builder1.setColor(ChocoBot.COLOR_COOKIE);
			builder1.setTitle("Wer ist es?");
			builder1.setDescription(Objects.requireNonNull(event.getMember()).getEffectiveName()+" hat es erraten!\n"+
					                        "Es war "+avatar.getLeft().getEffectiveName()+"!");
			builder1.setThumbnail(avatar.getRight());

			builder1.addField(event.getMember().getEffectiveName(), "+" + REWARD + " Coins", false);

			try(Connection connection = ChocoBot.getDatabase())
			{
				DatabaseUtils.increaseStat(connection, uid, guild.getIdLong(), "game."+getName().toLowerCase()+".won", 1);
				if(uid == sponsor.getIdLong())
				{
					DatabaseUtils.changeCoins(connection,
					                          uid,
					                          guild.getIdLong(),
					                          REWARD + getSponsorCost());
				}
				else
				{
					DatabaseUtils.changeCoins(connection,
					                          uid,
					                          guild.getIdLong(),
					                          REWARD);
					builder1.addField(sponsor.getEffectiveName(), "-" + getSponsorCost() + " Coins", false);
				}
			}
			catch(SQLException throwables)
			{
				throwables.printStackTrace();
			}

			gameChannel.sendMessage(builder1.build()).queue();
			gameMessage.delete().queue();

			this.end();
		}
		else
		{
			event.getMessage().addReaction("U+274C").queue();
			lastGuesses.put(uid, System.currentTimeMillis());
		}
	}

	private static byte[] makePixelGIF(BufferedImage base)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		encoder.start(out);
		encoder.setQuality(1);
		encoder.setDelay(1000);

		for(double i=0.01; i<1.0; i*=1.21)
		{
			BufferedImage scaled = new BufferedImage(base.getWidth(), base.getHeight(), base.getType());
			BufferedImage pixel = new BufferedImage(base.getWidth(), base.getHeight(), base.getType());

			AffineTransform at = new AffineTransform();
			at.scale(i, i);
			AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			scaled = scaleOp.filter(base, scaled);

			double b = (int)(base.getWidth()*i);

			at = new AffineTransform();
			at.scale(128.0/b, 128.0/b);
			scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			pixel = scaleOp.filter(scaled, pixel);

			encoder.addFrame(pixel);
		}
		for(int i=0; i<5; i++)
		{
			encoder.addFrame(base);
		}

		encoder.finish();
		return out.toByteArray();
	}

	private static byte[] makeZoomGIF(BufferedImage base)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		encoder.start(out);
		encoder.setQuality(1);
		encoder.setDelay(250);

		for(double i=16; i>1; i/=1.0284)
		{
			BufferedImage scaled = new BufferedImage(base.getWidth(), base.getHeight(), base.getType());

			AffineTransform at = new AffineTransform();
			at.translate((base.getWidth())/2.0, (base.getHeight())/2.0);
			at.scale(i, i);
			at.translate(-base.getWidth()/2.0, -base.getHeight()/2.0);
			AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			scaled = scaleOp.filter(base, scaled);

			encoder.addFrame(scaled);
		}
		for(int i=0; i<20; i++)
		{
			encoder.addFrame(base);
		}

		encoder.finish();
		return out.toByteArray();
	}

	@Override
	public String getName()
	{
		return "Weristes";
	}

	@Override
	protected int getSponsorCost()
	{
		return 100;
	}
}
