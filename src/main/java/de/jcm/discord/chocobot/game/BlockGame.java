package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BlockGame extends Command
{
	private static final int COST = 10;

	private final Random random = new Random();

	public static class Prize
	{
		private final double probability;
		private final int coins;
		private final String image;

		public Prize(double probability, int coins, String image)
		{
			this.probability = probability;
			this.coins = coins;
			this.image = image;
		}

		public double getProbability()
		{
			return probability;
		}

		public int getCoins()
		{
			return coins;
		}

		public String getImage()
		{
			return image;
		}

		public String getMessage(GuildSettings settings)
		{
			return settings.translate("game.block.prize."+coins+".message", coins);
		}
	}

	public static final Prize[] PRIZES = {
			new Prize(1.0/100, +1000, "large-coin-stack.png"),
			new Prize(9.0/100, +500, "medium-coin-stack.png"),
			new Prize(10.0/100, +100, "small-coin-stack.png"),
			new Prize(30.0/100, 0, null),
			new Prize(20.0/100, -20, "goomba.png"),
			new Prize(25.0/100, -100, "koopa.png"),
			new Prize((5.0-0.01)/100, -250, "bowser.png"),
			new Prize(0.01/100, +100000, "mueller.png")
	};

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		Member player = message.getMember();

		assert player != null;

		if (DatabaseUtils.getCoins(player.getIdLong(), guild.getIdLong()) < COST)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "game.error.not_enough", COST)).queue();
			return false;
		}
		else
		{
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_GAME);
			builder.setAuthor("@" + player.getEffectiveName());
			builder.setTitle(settings.translate("game.confirm.title"));
			builder.setDescription(settings.translate("game.confirm.description"));
			builder.addField(settings.translate("game.confirm.cost.key"), settings.translate("game.confirm.cost.value", COST), false);
			channel.sendMessage(builder.build()).queue((m) ->
			{
				m.addReaction("✅").queue();
				m.delete().queueAfter(10L, TimeUnit.SECONDS, (s) ->
				{
				}, (f) ->
				{
				});
				channel.getJDA().addEventListener(new ListenerAdapter()
				{
					GameState state;
					Message gameMessage;

					{
						this.state = GameState.CONFIRM;
					}

					public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
					{
						if (this.state == GameState.CONFIRM)
						{
							if (event.getMessageIdLong() == m.getIdLong() && event.getUser().getIdLong() == player.getIdLong() && event.getReactionEmote().isEmoji() && event.getReactionEmote().getEmoji().equals("✅"))
							{
								m.delete().queue();

								channel.sendFile(
										ChocoBot.class.getResourceAsStream("/block.png"),
										"block.png").queue(m->
								{
									gameMessage = m;
									m.addReaction("✊").queue();
								});

								try(Connection connection = ChocoBot.getDatabase())
								{
									DatabaseUtils.changeCoins(connection, player.getIdLong(), guild.getIdLong(), -COST);
									DatabaseUtils.increaseStat(connection, player.getIdLong(), guild.getIdLong(), "game.block.played", 1);
								}
								catch(SQLException throwables)
								{
									throwables.printStackTrace();
								}

								this.state = GameState.RUNNING;
							}
						}
						else if (this.state == GameState.RUNNING && this.gameMessage!=null &&
								event.getMessageIdLong() == this.gameMessage.getIdLong() &&
								event.getUser().getIdLong() == player.getIdLong() &&
								event.getReactionEmote().isEmoji() &&
								event.getReactionEmote().getEmoji().equals("✊"))
						{
							this.gameMessage.delete().queue();

							double rnd = random.nextDouble();

							String image = null;
							String text = settings.translate("game.block.error.general", COST);
							int coins = 10;

							double sum = 0.0;
							for(Prize prize : PRIZES)
							{
								if(rnd >= sum && rnd <= sum + prize.getProbability())
								{
									coins = prize.getCoins();
									image = prize.getImage();
									text = prize.getMessage(settings);
									break;
								}
								sum += prize.getProbability();
							}

							try
							{
								if(image!=null)
								{
									channel.sendFile(
											ChocoBot.class.getResourceAsStream("/" + image),
											image).queue();
								}
								channel.sendFile(
										ChocoBot.class.getResourceAsStream("/empty-block.png"),
										"empty-block.png").queue();
							}
							catch (Exception ignored)
							{

							}

							try(Connection connection = ChocoBot.getDatabase())
							{
								DatabaseUtils.increaseStat(connection, player.getIdLong(), guild.getIdLong(), "game.block.prize."+coins, 1);
							}
							catch(SQLException throwables)
							{
								throwables.printStackTrace();
							}

							// don't let the player get negative coins
							int c;
							if(coins<0 && (c = DatabaseUtils.getCoins(player.getIdLong(), guild.getIdLong()))<-coins)
							{
								coins = -c;
							}

							DatabaseUtils.changeCoins(player.getIdLong(), guild.getIdLong(), coins);
							channel.sendMessage(player.getAsMention()+" "+
									String.format(text, Math.abs(coins))).queue();
						}
					}
				});
			});
		}
		return true;
	}

	@Override
	public @NotNull String getKeyword()
	{
		return "block";
	}
}
