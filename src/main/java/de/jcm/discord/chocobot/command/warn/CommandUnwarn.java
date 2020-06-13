package de.jcm.discord.chocobot.command.warn;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandUnwarn extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if(args.length!=1)
		{
			channel.sendMessage(ChocoBot.errorMessage(
					"Du musst mir die ID der zu löschenden Warnung sagen!")).queue();
			return false;
		}

		Member member = message.getMember();
		assert member != null;

		if(!settings.isOperator(message.getMember()))
		{
			channel.sendMessage(ChocoBot.errorMessage("Du darfst keine Verwarnungen löschen!")).queue();
			return false;
		}

		int id;
		try
		{
			id = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException ignored)
		{
			channel.sendMessage(ChocoBot.errorMessage("Ich kann die ID nicht verstehen!")).queue();
			return false;
		}

		try
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement getWarning = connection.prepareStatement("SELECT message FROM warnings WHERE id = ? AND guild = ?");
			    PreparedStatement deleteWarning = connection.prepareStatement("DELETE FROM warnings WHERE id = ? AND guild = ?"))
			{
				getWarning.setInt(1, id);
				getWarning.setLong(2, guild.getIdLong());
				try(ResultSet resultSet = getWarning.executeQuery())
				{
					if(resultSet.next())
					{
						long messageId = resultSet.getLong("message");

						deleteWarning.setInt(1, id);
						deleteWarning.setLong(2, guild.getIdLong());
						deleteWarning.execute();

						TextChannel warnTextChannel = settings.getWarningChannel();
						warnTextChannel.deleteMessageById(messageId).queue();

						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_WARN);
						builder.setTitle("Erfolg");
						builder.setDescription("Die Verwarnung wurde erfolgreich gelöscht!");

						channel.sendMessage(builder.build()).queue();

						return true;
					}
					else
					{
						channel.sendMessage(ChocoBot.errorMessage("Die Verwarnung konnte nicht gefunden werden!"))
						       .queue();
						return false;
					}
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "unwarn";
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return "Nimm eine Verwarnung zurück.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c <Verwarnungs-ID> (nur Operatoren) : %h";
	}
}
