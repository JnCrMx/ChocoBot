package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class CommandWarns extends Command
{
	private PreparedStatement listWarnings;

	public CommandWarns()
	{
		try
		{
			this.listWarnings = ChocoBot.database.prepareStatement(
					"SELECT id, reason, time, warner FROM warnings WHERE uid = ?");
		}
		catch (SQLException var2)
		{
			var2.printStackTrace();
		}
	}

	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		boolean isMod = Objects.requireNonNull(message.getMember()).getRoles().stream()
				.anyMatch(r -> ChocoBot.operatorRoles.contains(r.getId()));

		long targetId;
		if(args.length==0)
		{
			targetId = message.getAuthor().getIdLong();
		}
		else if(message.getMentionedUsers().size()==1)
		{
			targetId = message.getMentionedUsers().get(0).getIdLong();
			if(targetId!=message.getAuthor().getIdLong() && !isMod)
			{
				channel.sendMessage(ChocoBot.errorMessage(
						"Du darfst nur deine eigenen Verwarnungen sehen!")).queue();
				return false;
			}
		}
		else
		{
			channel.sendMessage(ChocoBot.errorMessage(
					"Ich verstehe nicht, von wem du die Verwarnungen sehen willst.")).queue();
			return false;
		}

		try
		{
			listWarnings.setLong(1, targetId);
			ResultSet resultSet = listWarnings.executeQuery();

			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_WARN);
			builder.setTitle("Verwarnungen von "+
					Objects.requireNonNull(ChocoBot.jda.getUserById(targetId)).getAsTag());

			int count = 0;
			for(;resultSet.next();count++)
			{
				int id = resultSet.getInt("id");
				String reason = resultSet.getString("reason");
				if(isMod)
				{
					long warnerId = resultSet.getLong("warner");
					String warnerTag = Objects.requireNonNull(ChocoBot.jda.getUserById(warnerId)).getAsTag();

					builder.addField("["+id+"] von "+warnerTag, reason, false);
				}
				else
				{
					builder.addField("[" + id + "]", reason, false);
				}
			}

			builder.setDescription("**"+count+"** Verwarnungen:");

			channel.sendMessage(builder.build()).queue();

			return true;
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
		return "warns";
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return "Zeige deine Warns oder die eines anderen Nutzers an.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c : Zeige deine Verwarnungen an.\n" +
				"%c <Nutzer> (nur Operatoren) : Zeige die Verwarnungen eines anderen Nutzers an.";
	}
}
