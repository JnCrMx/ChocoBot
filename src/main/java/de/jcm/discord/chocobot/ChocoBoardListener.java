package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChocoBoardListener extends ListenerAdapter
{
	@Override
	public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event)
	{
		Member member = ChocoBot.jda.getGuildChannelById(ChocoBot.commandChannel)
		                   .getGuild().getMember(event.getAuthor());
		if(member == null)
			return;

		PrivateChannel channel = event.getChannel();
		String message = event.getMessage().getContentRaw().strip();

		if(message.startsWith("?chocoboard") || message.startsWith("?board"))
		{
			if(message.contains(" "))
			{
				String argument = message.split(" ")[1];
				if(argument.equals("token"))
				{
					try(Connection connection = ChocoBot.getDatabase();
					    PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM tokens WHERE user = ?");
					    PreparedStatement insert = connection.prepareStatement("INSERT INTO tokens(token, user, operator) VALUES (?, ?,?)"))
					{
						statement.setLong(1, event.getAuthor().getIdLong());
						ResultSet result = statement.executeQuery();
						if(result.next())
						{
							channel.sendMessage(ChocoBot.errorMessage(
									"Du hast bereits einen Token. " +
											"Nutze ``?chocoboard revoke`` um diesen zu entfernen."))
							       .queue();
						}
						else
						{
							String token = RandomStringUtils.randomAlphanumeric(64);
							insert.setString(1, token);
							insert.setLong(2, member.getIdLong());
							insert.setBoolean(3, member.getRoles().stream()
							                           .anyMatch(r -> ChocoBot.operatorRoles.contains(r.getId())));
							if(insert.executeUpdate() != 0)
							{
								EmbedBuilder builder = new EmbedBuilder();
								builder.setColor(ChocoBot.COLOR_COOKIE);
								builder.setTitle("ChocoBoard", ChocoBot.boardUrl);
								builder.setDescription("Dein Token lautet:\n\n "+token);

								channel.sendMessage(builder.build()).queue();
							}
							else
							{
								channel.sendMessage(ChocoBot.errorMessage("Es gab einen Datenbankfehler!")).queue();
							}
						}
					}
					catch(SQLException throwables)
					{
						throwables.printStackTrace();
					}
				}
				else if(argument.equals("revoke"))
				{
					try(Connection connection = ChocoBot.getDatabase();
					    PreparedStatement delete = connection.prepareStatement("DELETE FROM tokens WHERE user = ?"))
					{
						delete.setLong(1, member.getIdLong());
						int count = delete.executeUpdate();

						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COOKIE);
						builder.setTitle("ChocoBoard", ChocoBot.boardUrl);
						builder.setDescription(count+" Token(s) erfolgreich gelöscht!");

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
				builder.setTitle("ChocoBoard", ChocoBot.boardUrl);
				builder.setDescription("Klicke auf den Titel um zum ChocoBoard zu gelangen!");

				channel.sendMessage(builder.build()).queue();
			}
		}
	}
}
