package de.jcm.discord.chocobot.command.warn;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.data.UserData;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandWarns extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		boolean isMod = settings.isOperator(message.getMember());

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
				channel.sendMessage(ChocoBot.translateError(settings, "command.warns.error.perm")).queue();
				return false;
			}
		}
		else
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.warns.error.who")).queue();
			return false;
		}

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement listWarnings = connection.prepareStatement("SELECT id, reason, time, warner FROM warnings WHERE uid = ? AND guild = ?"))
		{
			listWarnings.setLong(1, targetId);
			listWarnings.setLong(2, guild.getIdLong());
			try(ResultSet resultSet = listWarnings.executeQuery())
			{

				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(ChocoBot.COLOR_WARN);
				builder.setTitle(settings.translate("command.warns.title", ChocoBot.provideUser(targetId, UserData::getTag, "Unknown user")));
				int count = 0;
				for(; resultSet.next(); count++)
				{
					int id = resultSet.getInt("id");
					String reason = resultSet.getString("reason");
					if(isMod)
					{
						long warnerId = resultSet.getLong("warner");
						String warnerTag = ChocoBot.jda.retrieveUserById(warnerId).map(User::getAsTag).complete();

						builder.addField(settings.translate("command.warns.warn.operator", id, warnerTag), reason, false);
					}
					else
					{
						builder.addField(settings.translate("command.warns.warn.operator", id), reason, false);
					}
				}

				builder.setDescription(settings.translate("command.warns.message", count));

				channel.sendMessage(builder.build()).queue();

				return true;
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
		return "warns";
	}
}
