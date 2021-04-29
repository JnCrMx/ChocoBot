package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MirrorListener extends ListenerAdapter
{
	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event)
	{
		if(event.getAuthor().isBot())
			return;

		String message = event.getMessage().getContentRaw();
		GuildSettings settings = DatabaseUtils.getSettings(event.getGuild());
		if(settings == null)
			return;

		if(message.equals(settings.translate("secret.mirror.infinity")))
		{
			StringWriter writer = new StringWriter();
			PrintWriter writer1 = new PrintWriter(writer);

			ArithmeticException exception = new ArithmeticException("infinity + 1");
			exception.printStackTrace(writer1);

			writer1.close();

			event.getChannel().sendMessage("```"+ writer +"```").queue();

			return;
		}

		Pattern pattern = Pattern.compile(settings.translate("secret.mirror.pattern"));
		Matcher matcher = pattern.matcher(message);
		if(matcher.matches())
		{
			BigInteger count = BigInteger.ONE;
			if(matcher.group(2)!=null)
			{
				try
				{
					count = new BigInteger(matcher.group(2));
				}
				catch (NumberFormatException ignored)
				{

				}
			}
			count=count.add(BigInteger.ONE);

			String msg = settings.translate("secret.mirror.message", count.toString());
			if(msg.length() > Message.MAX_CONTENT_LENGTH)
			{
				try
				{
					Objects.requireNonNull(event.getMember())
					       .kick(settings.translate("secret.mirror.kick")).queue();
				}
				catch(HierarchyException | InsufficientPermissionException e)
				{
					event.getChannel().sendMessage(
							ChocoBot.translateError(settings, "secret.mirror.kick.fail")).queue();
				}
				return;
			}
			event.getChannel().sendMessage(msg).queue();
		}
	}
}
