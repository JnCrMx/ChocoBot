package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class ChocoBoardListener extends ListenerAdapter
{
	@Override
	public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event)
	{
		User user = event.getAuthor();

		PrivateChannel channel = event.getChannel();
		String message = event.getMessage().getContentRaw().strip();

		if(message.startsWith("?chocoboard") || message.startsWith("?board"))
		{
			GuildSettings settings = DatabaseUtils.getUserSettings(user);

			if(message.contains(" "))
			{
				String subcommand = message.split(" ")[1].toLowerCase(Locale.ROOT);
				if(subcommand.equals("token") || subcommand.equals("t"))
				{
					try(Connection connection = ChocoBot.getDatabase();
					    PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM tokens WHERE user = ?");
					    PreparedStatement insert = connection.prepareStatement("INSERT INTO tokens(token, user) VALUES (?, ?)"))
					{
						statement.setLong(1, event.getAuthor().getIdLong());
						ResultSet result = statement.executeQuery();
						if(result.next())
						{
							channel.sendMessage(ChocoBot.translateError(settings, "chocoboard.token.error.dup")).queue();
						}
						else
						{
							String token = RandomStringUtils.randomAlphanumeric(64);
							insert.setString(1, token);
							insert.setLong(2, user.getIdLong());
							if(insert.executeUpdate() != 0)
							{
								EmbedBuilder builder = new EmbedBuilder();
								builder.setColor(ChocoBot.COLOR_COOKIE);
								builder.setTitle(settings.translate("chocoboard.token.title"), ChocoBot.boardUrl);
								builder.setDescription(settings.translate("chocoboard.token.description", token));

								channel.sendMessage(builder.build()).queue();
							}
							else
							{
								channel.sendMessage(ChocoBot.translateError(settings, "chocoboard.token.error.db")).queue();
							}
						}
					}
					catch(SQLException throwables)
					{
						throwables.printStackTrace();
					}
				}
				else if(subcommand.equals("revoke") || subcommand.equals("r"))
				{
					try(Connection connection = ChocoBot.getDatabase();
					    PreparedStatement delete = connection.prepareStatement("DELETE FROM tokens WHERE user = ?"))
					{
						delete.setLong(1, user.getIdLong());
						int count = delete.executeUpdate();

						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COOKIE);
						builder.setTitle(settings.translate("chocoboard.revoke.title"), ChocoBot.boardUrl);
						builder.setDescription(settings.translate("chocoboard.revoke.description", count));

						channel.sendMessage(builder.build()).queue();
					}
					catch(SQLException throwables)
					{
						throwables.printStackTrace();
					}
				}
			}
			else
			{
				EmbedBuilder builder = new EmbedBuilder();
				builder.setColor(ChocoBot.COLOR_COOKIE);
				builder.setTitle(settings.translate("chocoboard.title"), ChocoBot.boardUrl);
				builder.setDescription(settings.translate("chocoboard.description"));

				channel.sendMessage(builder.build()).queue();
			}
		}
	}
}
