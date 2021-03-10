package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandLeaderboard extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		try(Connection connection = ChocoBot.getDatabase())
		{
			Map<String, Map<Long, Integer>> stats = DatabaseUtils.getStats(connection, guild.getIdLong());

			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle(settings.translate("command.leaderboard.title"));
			builder.setColor(ChocoBot.COLOR_COOKIE);

			Map<Long, Integer> currentCoins = new HashMap<>();
			Map<Long, Integer> currentStreaks = new HashMap<>();

			try(PreparedStatement statement = connection.prepareStatement("SELECT uid, coins, daily_streak, last_daily FROM coins WHERE guild = ?"))
			{
				statement.setLong(1, guild.getIdLong());

				try(ResultSet result = statement.executeQuery())
				{
					while(result.next())
					{
						currentCoins.put(result.getLong("uid"), result.getInt("coins"));

						LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(result.getLong("last_daily")), ZoneId.systemDefault());
						if(dateTime.getLong(ChronoField.EPOCH_DAY) + 1L >= LocalDateTime.now().getLong(ChronoField.EPOCH_DAY))
						{
							currentStreaks.put(result.getLong("uid"), result.getInt("daily_streak"));
						}
					}
				}
			}

			makeLeaderboard(builder, settings, "command.leaderboard.entry.current_coins", currentCoins);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.max_coins", stats.getOrDefault("max_coins", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.current_streak", currentStreaks);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.max_streak", stats.getOrDefault("daily.max_streak", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.sponsored", stats.getOrDefault("game.quiz.sponsored", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.played", stats.getOrDefault("game.quiz.played", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.won", stats.getOrDefault("game.quiz.won", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.1st", stats.getOrDefault("game.quiz.won.place.1", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.2nd", stats.getOrDefault("game.quiz.won.place.2", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.quiz.3rd", stats.getOrDefault("game.quiz.won.place.3", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.geschenke.sponsored", stats.getOrDefault("game.geschenke.sponsored", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.geschenke.played", stats.getOrDefault("game.geschenke.played", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.geschenke.collected", stats.getOrDefault("game.geschenke.collected", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, settings, "command.leaderboard.entry.block.played", stats.getOrDefault("game.block.played", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.block.1000", stats.getOrDefault("game.block.prize.1000", new HashMap<>()));
			makeLeaderboard(builder, settings, "command.leaderboard.entry.block.-250", stats.getOrDefault("game.block.prize.-250", new HashMap<>()));

			channel.sendMessage(builder.build()).queue();

			return true;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			return false;
		}
	}

	private static void makeLeaderboard(EmbedBuilder builder, GuildSettings settings, String title, Map<Long, Integer> values)
	{
		Comparator<Map.Entry<Long, Integer>> comparator = Comparator.comparingInt(Map.Entry::getValue);

		AtomicInteger rank = new AtomicInteger(1);

		StringBuffer buffer = values.entrySet().stream()
		                            .sorted(comparator.reversed())
		                            .limit(5)
		                            .collect(StringBuffer::new,
		                                     (s,e)-> s.append(makeBadge(rank.getAndAdd(1))).append(" ")
		                                              .append(ChocoBot.provideUser(e.getKey(), UserData::getTag, "Unknown user"))
		                                              .append(": ").append(e.getValue()).append('\n'),
		                                     StringBuffer::append);
		builder.addField(settings.translate(title), buffer.toString(), true);
	}

	private static String makeBadge(int rank)
	{
		if(rank <= 3)
			return "\uD83E" + ((char)('\uDD47' + rank - 1));
		else
			return rank +".";
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "leaderboard";
	}
}
