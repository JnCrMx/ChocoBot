package de.jcm.discord.chocobot.command.warn;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public class CommandUnwarn extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if(args.length!=1)
		{
			sendTempMessage(channel, ChocoBot.errorMessage(
					"Du musst mir die ID der zu löschenden Warnung sagen!"));
			return false;
		}

		Member member = message.getMember();
		assert member != null;

		if(member.getRoles().stream().noneMatch(r -> ChocoBot.operatorRoles.contains(r.getId())))
		{
			sendTempMessage(channel, ChocoBot.errorMessage("Du darfst keine Verwarnungen löschen!"));
			return false;
		}

		int id;
		try
		{
			id = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException ignored)
		{
			sendTempMessage(channel, ChocoBot.errorMessage("Ich kann die ID nicht verstehen!"));
			return false;
		}

		try
		{
			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement getWarning = connection.prepareStatement("SELECT message FROM warnings WHERE id = ?");
			    PreparedStatement deleteWarning = connection.prepareStatement("DELETE FROM warnings WHERE id = ?"))
			{
				getWarning.setInt(1, id);
				try(ResultSet resultSet = getWarning.executeQuery())
				{
					if(resultSet.next())
					{
						long messageId = resultSet.getLong("message");

						deleteWarning.setInt(1, id);
						deleteWarning.execute();

						GuildChannel warnChannel = ChocoBot.jda.getGuildChannelById(ChocoBot.warningChannel);
						assert warnChannel != null;
						if(warnChannel.getType() == ChannelType.TEXT)
						{
							TextChannel warnTextChannel = (TextChannel) warnChannel;
							warnTextChannel.deleteMessageById(messageId).queue();
						}

						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_WARN);
						builder.setTitle("Erfolg");
						builder.setDescription("Die Verwarnung wurde erfolgreich gelöscht!");

						sendTempMessage(channel, builder.build());

						return true;
					}
					else
					{
						sendTempMessage(channel, ChocoBot.errorMessage("Die Verwarnung konnte nicht gefunden werden!"));
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

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
