package de.jcm.discord.chocobot.command.warn;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

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
			channel.sendMessage(ChocoBot.translateError(settings, "command.unwarn.error.narg")).queue();
			return false;
		}

		Member member = message.getMember();
		assert member != null;

		if(!settings.isOperator(message.getMember()))
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.unwarn.error.perm")).queue();
			return false;
		}

		int id;
		try
		{
			id = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException ignored)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.unwarn.error.fmt")).queue();
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
						builder.setTitle(settings.translate("commadn.unwarn.title"));
						builder.setDescription(settings.translate("command.unwarn.message"));

						channel.sendMessage(builder.build()).queue();

						return true;
					}
					else
					{
						channel.sendMessage(ChocoBot.translateError(settings, "command.unwarn.error.noent")).queue();
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
}
