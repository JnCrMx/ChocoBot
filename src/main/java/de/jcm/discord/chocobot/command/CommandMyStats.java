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
			{"max_coins"},
			{"daily.max_streak"},
			{},
			{null, "Quiz", null},
			{"game.quiz.played"},
			{"game.quiz.sponsored"},
			{"game.quiz.won"},
			{"game.quiz.won.place.1"},
			{"game.quiz.won.place.2"},
			{"game.quiz.won.place.3"},
			{},
			{null, "Geschenke", null},
			{"game.geschenke.played"},
			{"game.geschenke.sponsored"},
			{"game.geschenke.collected"},
			{},
			{null, "Block", null},
			{"game.block.played"},
			{"game.block.prize.1000"},
			{"game.block.prize.500"},
			{"game.block.prize.100"},
			{"game.block.prize.0"},
			{"game.block.prize.-20"},
			{"game.block.prize.-100"},
			{"game.block.prize.-250"},
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
					if(line[0].equals("--inline--"))
					{
						builder.addBlankField(true);
					}
					else
					{
						builder.addField(settings.translate("command.mystats.entry."+line[0]),
						                 String.valueOf(stats.getOrDefault(line[0], 0)), false);
					}
				}
				else if(line.length == 3)
				{
					builder.addField(line[1], "", false);
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
}
