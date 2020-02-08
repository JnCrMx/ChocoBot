package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandUnwarn extends Command
{
	private PreparedStatement getWarning;
	private PreparedStatement deleteWarning;

	public CommandUnwarn()
	{
		try
		{
			this.getWarning = ChocoBot.database.prepareStatement("SELECT message FROM warnings WHERE id = ?");
			this.deleteWarning = ChocoBot.database.prepareStatement("DELETE FROM warnings WHERE id = ?");
		}
		catch (SQLException var2)
		{
			var2.printStackTrace();
		}
	}

	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		if(args.length!=1)
		{
			channel.sendMessage(ChocoBot.errorMessage(
					"Du musst mir die ID der zu löschenden Warnung sagen!")).queue();
			return false;
		}

		Member member = message.getMember();
		assert member != null;

		if(member.getRoles().stream().noneMatch(r -> ChocoBot.warningRoles.contains(r.getId())))
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
			getWarning.setInt(1, id);
			ResultSet resultSet = getWarning.executeQuery();
			if(resultSet.next())
			{
				long messageId = resultSet.getLong("message");

				deleteWarning.setInt(1, id);
				deleteWarning.execute();

				GuildChannel warnChannel = ChocoBot.jda.getGuildChannelById(ChocoBot.warningChannel);
				assert warnChannel != null;
				if (warnChannel.getType() == ChannelType.TEXT)
				{
					TextChannel warnTextChannel = (TextChannel) warnChannel;
					warnTextChannel.deleteMessageById(messageId).queue();
				}

				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(ChocoBot.COLOR_WARN);
				builder.setTitle("Erfolg");
				builder.setDescription("Die Verwarnung wurde erfolgreich gelöscht!");

				channel.sendMessage(builder.build()).queue();

				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Die Verwarnung konnte nicht gefunden werden!")).queue();
				return false;
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
}
