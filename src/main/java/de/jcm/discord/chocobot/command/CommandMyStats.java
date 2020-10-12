package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class CommandMyStats extends Command
{
	private static final String[][] statEntries = {
			{"Maximale Coins \uD83D\uDCB0", "max_coins"},
			{"Maximale Daily Streak \uD83D\uDC8E", "daily.max_streak"},
			{},
			{"Quiz"},
			{"Gespielt \uD83C\uDFAE", "game.quiz.played"},
			{"Gesponsort \uD83D\uDCEF", "game.quiz.sponsored"},
			{"Gewonnen \uD83C\uDFC5", "game.quiz.won"},
			{"Platz 1 \uD83E\uDD47", "game.quiz.won.place.1"},
			{"Platz 2 \uD83E\uDD48", "game.quiz.won.place.2"},
			{"Platz 3 \uD83E\uDD49", "game.quiz.won.place.3"},
			{},
			{"Geschenke"},
			{"Gespielt \uD83C\uDFAE", "game.geschenke.played"},
			{"Gesponsort \uD83D\uDCEF", "game.geschenke.sponsored"},
			{"Gesammelt \uD83C\uDF81", "game.geschenke.collected"},
			{},
			{"Block"},
			{"Gespielt \uD83C\uDFAE", "game.block.played"},
			{"+1000 \uD83D\uDCB8", "game.block.prize.1000"},
			{"-250 \uD83D\uDE08", "game.block.prize.-250"},
	};

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		try(Connection connection = ChocoBot.getDatabase())
		{
			Map<String, Integer> stats = DatabaseUtils.getStats(connection, message.getAuthor().getIdLong(), guild.getIdLong());

			EmbedBuilder builder = new EmbedBuilder();
			builder.setAuthor(message.getAuthor().getName());
			builder.setTitle("Statistiken");
			builder.setColor(ChocoBot.COLOR_COOKIE);

			for(String[] line : statEntries)
			{
				if(line.length == 0)
				{
					builder.addBlankField(false);
				}
				else if(line.length == 1)
				{
					builder.addField(line[0], "", false);
				}
				else
				{
					builder.addField(line[0], String.valueOf(stats.getOrDefault(line[1], 0)), true);
				}
			}

			channel.sendMessage(builder.build()).queue();

			return true;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			return false;
		}
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "mystats";
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return "Zeige eigene Statistiken an.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c : %h";
	}
}
