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
import org.jetbrains.annotations.Nullable;

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
			builder.setAuthor(message.getAuthor().getName());
			builder.setTitle("Bestenliste");
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

			makeLeaderboard(builder, "momentane Coins \uD83D\uDCB0", currentCoins);
			makeLeaderboard(builder, "maximale Coins \uD83D\uDCB0", stats.getOrDefault("max_coins", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, "momentane Streak \uD83D\uDC8E", currentStreaks);
			makeLeaderboard(builder, "maximale Streak \uD83D\uDC8E", stats.getOrDefault("daily.max_streak", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, "Quiz gesponsert \uD83D\uDCEF", stats.getOrDefault("game.quiz.sponsored", new HashMap<>()));
			makeLeaderboard(builder, "Quiz gespielt \uD83C\uDFAE", stats.getOrDefault("game.quiz.played", new HashMap<>()));
			makeLeaderboard(builder, "Quiz gewonnen \uD83C\uDFC5", stats.getOrDefault("game.quiz.won", new HashMap<>()));
			makeLeaderboard(builder, "Quiz als 1. Platz gewonnen \uD83E\uDD47", stats.getOrDefault("game.quiz.won.place.1", new HashMap<>()));
			makeLeaderboard(builder, "Quiz als 2. Platz gewonnen \uD83E\uDD48", stats.getOrDefault("game.quiz.won.place.2", new HashMap<>()));
			makeLeaderboard(builder, "Quiz als 3. Platz gewonnen \uD83E\uDD49", stats.getOrDefault("game.quiz.won.place.3", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, "Geschenke gesponsert \uD83D\uDCEF", stats.getOrDefault("game.geschenke.sponsored", new HashMap<>()));
			makeLeaderboard(builder, "Geschenke gespielt \uD83C\uDFAE", stats.getOrDefault("game.geschenke.played", new HashMap<>()));
			makeLeaderboard(builder, "Geschenke gesammelt \uD83C\uDF81", stats.getOrDefault("game.geschenke.collected", new HashMap<>()));

			builder.addBlankField(false);
			makeLeaderboard(builder, "Block gespielt \uD83C\uDFAE", stats.getOrDefault("game.block.played", new HashMap<>()));
			makeLeaderboard(builder, "1000 Coins gewonnen \uD83D\uDCB8", stats.getOrDefault("game.block.prize.1000", new HashMap<>()));
			makeLeaderboard(builder, "250 Coins verloren \uD83D\uDE08", stats.getOrDefault("game.block.prize.-250", new HashMap<>()));

			channel.sendMessage(builder.build()).queue();

			return true;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			return false;
		}
	}

	private static void makeLeaderboard(EmbedBuilder builder, String title, Map<Long, Integer> values)
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
		builder.addField(title, buffer.toString(), true);
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

	@Override
	protected @Nullable String getHelpText()
	{
		return "Zeigt die Bestenliste an.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c : %h";
	}
}
