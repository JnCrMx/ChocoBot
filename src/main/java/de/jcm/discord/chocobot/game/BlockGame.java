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
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BlockGame extends Command
{
	private static final int COST = 10;

	private final Random random = new Random();

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		Member player = message.getMember();

		assert player != null;

		if (DatabaseUtils.getCoins(player.getIdLong(), guild.getIdLong()) < COST)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du hast dafür nicht genug Coins! Du brauchst mindestens "
					+COST+".")).queue();
			return false;
		}
		else
		{
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_GAME);
			builder.setAuthor("@" + player.getEffectiveName());
			builder.setTitle("Bestätigen?");
			builder.setDescription("Wenn du wirklich fortfahren willst, reagiere mit :white_check_mark:!");
			builder.addField("Preis", COST+" Coins", false);
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

							int rnd = random.nextInt(100);

							String image = null;
							String text = "Fehler! Deine 10 Coins werden dir rückerstattet.";
							int coins = 10;

							// 1% -> +1000 Coins
							if(rnd < 1)
							{
								image = "large-coin-stack.png";
								text = "OMG! Du hast soeben %d Coins aus dem Block bekommen, " +
										"du Glückspilz!";
								coins = +1000;
							}
							// 9% -> +500 Coins
							else if(rnd<1+ 9)
							{
								image = "medium-coin-stack.png";
								text = "Du Glücklicher! Du hast %d Coins aus dem Block ergattern können!";
								coins = +500;
							}
							// 10% -> +100 Coins
							else if(rnd<1+9+ 10)
							{
								image = "small-coin-stack.png";
								text = "Du hattest Glück, In dem Block waren %d Münzen!";
								coins = +100;
							}
							// 30% -> +/- 0 Coins
							else if(rnd<1+9+10+ 30)
							{
								text = "Schade... offentsichtlich enthielt dieser Block nichts.";
								coins = 0;
							}
							// 20% -> -20 Coins
							else if(rnd<1+9+10+30+ 20)
							{
								image = "goomba.png";
								text = "Oha. Ein Gumba, der aus dem Block kam, hat dir soeben %d Coins gemopst!";
								coins = -20;
							}
							// 25% -> -100 Coins
							else if(rnd<1+9+10+30+20+ 25)
							{
								image = "koopa.png";
								text = "Aus dem Block kam... ein Koopa... der dir %d Münzen gestohlen hat...";
								coins = -100;
							}
							// 5% -> -250 Coins
							else if(rnd<1+9+10+30+20+25+ 5)
							{
								image = "bowser.png";
								text = "***Gwahaha!** Danke, dass du mich soeben aus dem Block befreit hast.... " +
										"Dafür habe ich dir %d deiner glänzenden Münzen geklaut!*";
								coins = -250;
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
									String.format(text, coins<0?-coins:coins)).queue();
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

	@Override
	public @Nullable String getHelpText()
	{
		return "Öffne einen ?-Block.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c : %h";
	}
}
