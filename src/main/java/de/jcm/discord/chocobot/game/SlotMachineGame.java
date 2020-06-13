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
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SlotMachineGame extends Command
{
	private static final int COST = 10;
	private static final int SPECIAL_MULTIPLIER = 2;
	private static final int PRIZE_THREE = 1000;
	private static final int PRIZE_TWO_NEXT = 100;
	private static final int PRIZE_TWO = 50;
	private static final int PRIZE_THREE_CATEGORY = 25;
	private static final int PRIZE_TWO_NEXT_CATEGORY = 15;
	private static final int PRIZE_TWO_CATEGORY = 10;
	private static final String[] EMOJIS_FRUITS = new String[]{"\ud83c\udf4e", "\ud83c\udf4b", "\ud83c\udf49", "\ud83c\udf53", "\ud83c\udf48", "\ud83c\udf52", "\ud83c\udf4d"};
	private static final String[] EMOJIS_HEARTS = new String[]{"❤️", "\ud83e\udde1", "\ud83d\udc9b", "\ud83d\udc9a", "\ud83d\udc99", "\ud83d\udc9c", "\ud83d\udda4"};
	private static final String[] EMOJIS_MONEY = new String[]{"\ud83d\udcb8", "\ud83d\udcb5", "\ud83d\udcb4", "\ud83d\udcb6", "\ud83d\udcb7", "\ud83d\udcb0", "\ud83d\udcb3"};
	private static final String[] EMOJIS_OTHER = new String[]{"\ud83c\udfb2", "\ud83c\udfc6", "\ud83c\udfab", "\ud83c\udfae"};
	private static final String[][] EMOJIS_NORMAL;
	private static final String[] EMOJIS_SPECIAL;
	private static final int SPECIAL;
	private static final String[] EMOJIS_WHEEL;
	private final Random random = new Random();

	private final String FORMAT_PLAYING =
					"%s\n"+
					"``---[Slot-Machine]---``\n" +
					"           %s : %s : %s \n\n" +
					"           %s : %s : %s **<**\n\n" +
					"           %s : %s : %s \n" +
					"``--------------------``";
	private final String FORMAT_WON =
					"%s\n"+
					"``---[Slot-Machine]---``\n" +
					"           %s : %s : %s \n\n" +
					"           %s : %s : %s **<**\n\n" +
					"           %s : %s : %s \n" +
					"``-----[GEWONNEN]-----``\n"+
					"**%d Coins!**";
	private final String FORMAT_LOST =
					"%s\n"+
					"``---[Slot-Machine]---``\n" +
					"           %s : %s : %s \n\n" +
					"           %s : %s : %s **<**\n\n" +
					"           %s : %s : %s \n" +
					"``-----[VERLOREN]-----``";

	public SlotMachineGame()
	{
	}

	private String[][] buildWheelStrings(int[] wheels)
	{
		String[][] array = new String[3][3];

		array[0][0] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[0] + 1)%EMOJIS_WHEEL.length];
		array[0][1] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[0])%EMOJIS_WHEEL.length];
		array[0][2] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[0] - 1)%EMOJIS_WHEEL.length];

		array[1][0] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[1] + 1)%EMOJIS_WHEEL.length];
		array[1][1] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[1])%EMOJIS_WHEEL.length];
		array[1][2] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[1] - 1)%EMOJIS_WHEEL.length];

		array[2][0] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[2] + 1)%EMOJIS_WHEEL.length];
		array[2][1] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[2])%EMOJIS_WHEEL.length];
		array[2][2] = EMOJIS_WHEEL[(EMOJIS_WHEEL.length + wheels[2] - 1)%EMOJIS_WHEEL.length];

		return array;
	}

	private String buildMessage(Member player, int[] wheels)
	{
		String[][] a = buildWheelStrings(wheels);

		return String.format(FORMAT_PLAYING, player.getAsMention(),
				a[0][0], a[1][0], a[2][0],
				a[0][1], a[1][1], a[2][1],
				a[0][2], a[1][2], a[2][2]);
	}

	private void updateMessage(Message message, Member player, int[] wheels)
	{
		wheels[0] += 1;
		wheels[1] += 2;
		wheels[2] += 3;

		for(int i=0;i<wheels.length;i++)
			wheels[i] = wheels[i] % EMOJIS_WHEEL.length;

		message.editMessage(buildMessage(player, wheels)).submit();
	}

	private int getCategory(String emoji)
	{
		if (ArrayUtils.contains(EMOJIS_SPECIAL, emoji))
		{
			return SPECIAL;
		}
		else
		{
			for (int i = 0; i < EMOJIS_NORMAL.length; ++i)
			{
				if (ArrayUtils.contains(EMOJIS_NORMAL[i], emoji))
				{
					return i;
				}
			}

			return -1;
		}
	}

	private int calculatePrize(int[] wheels)
	{
		String e1 = EMOJIS_WHEEL[wheels[0]];
		String e2 = EMOJIS_WHEEL[wheels[1]];
		String e3 = EMOJIS_WHEEL[wheels[2]];
		int c1 = this.getCategory(e1);
		int c2 = this.getCategory(e2);
		int c3 = this.getCategory(e3);
		if (e1.equals(e2) && e1.equals(e3))
		{
			return PRIZE_THREE * (c1 == SPECIAL ? SPECIAL_MULTIPLIER : 1);
		}
		else if (!e1.equals(e2) && !e2.equals(e3))
		{
			if (e1.equals(e3))
			{
				return PRIZE_TWO * (c1 == SPECIAL ? SPECIAL_MULTIPLIER : 1);
			}
			else if (c1 == c2 && c1 == c3)
			{
				return PRIZE_THREE_CATEGORY * (c1 == SPECIAL ? SPECIAL_MULTIPLIER : 1);
			}
			else if (c1 != c2 && c2 != c3)
			{
				return c1 == c3 ? PRIZE_TWO_CATEGORY * (c1 == SPECIAL ? SPECIAL_MULTIPLIER : 1) : 0;
			}
			else
			{
				return PRIZE_TWO_NEXT_CATEGORY * (c2 == SPECIAL ? SPECIAL_MULTIPLIER : 1);
			}
		}
		else
		{
			return PRIZE_TWO_NEXT * (c2 == SPECIAL ? SPECIAL_MULTIPLIER : 1);
		}
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		Member player = message.getMember();

		assert player != null;

		if (DatabaseUtils.getCoins(player.getIdLong(), guild.getIdLong()) < COST)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du hast dafür nicht genug Coins! Du brauchst mindestens "+COST+".")).queue();
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
					private int[] wheels;
					GameState state;
					Message gameMessage;
					ScheduledFuture<?> future;

					{
						this.state = GameState.CONFIRM;
					}

					public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
					{
						if (this.state == GameState.CONFIRM)
						{
							if (event.getMessageIdLong() == m.getIdLong() &&
									event.getUser().getIdLong() == player.getIdLong() &&
									event.getReactionEmote().isEmoji() &&
									event.getReactionEmote().getEmoji().equals("✅"))
							{
								m.delete().queue();
								this.wheels = random.ints(3L, 0, EMOJIS_WHEEL.length)
										.toArray();
								channel.sendMessage(buildMessage(player, wheels)).queue((m2) ->
								{
									this.gameMessage = m2;
									this.gameMessage.addReaction("\ud83d\udccd").queue();
									this.future = ChocoBot.executorService.scheduleWithFixedDelay(() ->
											SlotMachineGame.this.updateMessage(gameMessage, player, wheels),
											0L, 1L, TimeUnit.SECONDS);
								});
								DatabaseUtils.changeCoins(player.getIdLong(), guild.getIdLong(), -COST);
								this.state = GameState.RUNNING;
							}
						}
						else if (this.state == GameState.RUNNING &&
								gameMessage!=null &&
								event.getMessageIdLong() == this.gameMessage.getIdLong() &&
								event.getUser().getIdLong() == player.getIdLong() &&
								event.getReactionEmote().isEmoji() &&
								event.getReactionEmote().getEmoji().equals("\ud83d\udccd"))
						{
							this.future.cancel(true);
							int prize = SlotMachineGame.this.calculatePrize(this.wheels);
							if (prize > 0)
							{
								String[][] a = buildWheelStrings(wheels);

								String msg = String.format(FORMAT_WON, player.getAsMention(),
										a[0][0], a[1][0], a[2][0],
										a[0][1], a[1][1], a[2][1],
										a[0][2], a[1][2], a[2][2],
										prize);
								gameMessage.editMessage(msg).queue();

								DatabaseUtils.changeCoins(player.getIdLong(), guild.getIdLong(), prize);
							}
							else
							{
								String[][] a = buildWheelStrings(wheels);

								String msg = String.format(FORMAT_LOST, player.getAsMention(),
										a[0][0], a[1][0], a[2][0],
										a[0][1], a[1][1], a[2][1],
										a[0][2], a[1][2], a[2][2]);
								gameMessage.editMessage(msg).queue();
							}

							this.gameMessage.clearReactions().queue();
						}

					}
				});
			});
			return true;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "slot-machine";
	}

	@Nullable
	public String getHelpText()
	{
		return "Spiele an einer Slot-Maschine.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c : %h";
	}

	static
	{
		EMOJIS_NORMAL = new String[][]{EMOJIS_FRUITS, EMOJIS_HEARTS, EMOJIS_MONEY, EMOJIS_OTHER};
		EMOJIS_SPECIAL = new String[]{"\ud83c\udfb1", "\ud83d\udc8e", "\ud83c\udf6a"};
		SPECIAL = EMOJIS_NORMAL.length;
		String[] array = new String[0];

		for (String[] strings : EMOJIS_NORMAL)
		{
			array = ArrayUtils.addAll(array, strings);
		}

		array = ArrayUtils.addAll(array, array);
		array = ArrayUtils.addAll(array, EMOJIS_SPECIAL);
		EMOJIS_WHEEL = array;
	}
}
