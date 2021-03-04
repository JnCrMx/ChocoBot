package de.jcm.discord.chocobot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueEventUnsubscribeListener extends ListenerAdapter
{
	@Override
	public void onPrivateMessageReactionAdd(@Nonnull PrivateMessageReactionAddEvent event)
	{
		if(!ChocoBot.githubBugReportEnabled)
			return;

		MessageReaction.ReactionEmote emote = event.getReactionEmote();
		if(emote.isEmoji() && emote.getEmoji().equals("\u274c"))
		{
			GuildSettings settings = DatabaseUtils.getUserSettings(event.getUser());

			event.getChannel().retrieveMessageById(event.getMessageId())
			     .queue(message ->
			            {
				            if(!message.getEmbeds().isEmpty())
				            {
					            MessageEmbed embed = message.getEmbeds().get(0);
					            String url = String.valueOf(embed.getUrl());
					            Pattern pattern = Pattern.compile("https://github.com/JnCrMx/ChocoBot/issues/(\\d*)");
					            Matcher matcher = pattern.matcher(url);
					            if(matcher.find())
					            {
						            int id = Integer.parseInt(matcher.group(1));

						            try(Connection connection = ChocoBot.getDatabase();
						                PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM bugreports WHERE id=?"))
						            {
						            	deleteStatement.setInt(1, id);
						            	deleteStatement.execute();
							            if(deleteStatement.getUpdateCount()==1)
							            {
								            EmbedBuilder eb = new EmbedBuilder();
								            eb.setTitle(settings.translate("issue_events.unsubscribe.title"));
								            eb.setColor(ChocoBot.COLOR_COOKIE);
								            eb.setDescription(settings.translate("issue_events.unsubscribe.description", id));
								            event.getChannel().sendMessage(eb.build()).queue();
							            }
							            else
							            {
								            event.getChannel().sendMessage(ChocoBot.translateError(settings, "issue_events.unsubscribe.error.noent")).queue();
							            }
						            }
						            catch(SQLException e)
						            {
						            	e.printStackTrace();
							            event.getChannel().sendMessage(ChocoBot.translateError(settings, "issue_events.unsubscribe.error.internal")).queue();
						            }
					            }
				            }
			            });
		}
	}
}
