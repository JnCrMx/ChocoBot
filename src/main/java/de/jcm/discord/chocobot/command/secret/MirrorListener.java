package de.jcm.discord.chocobot.command.secret;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MirrorListener extends ListenerAdapter
{
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event)
	{
		if(event.getAuthor().isBot())
			return;

		String message = event.getMessage().getContentRaw();

		if(message.equals("∞x Spiegel"))
		{
			StringWriter writer = new StringWriter();
			PrintWriter writer1 = new PrintWriter(writer);

			ArithmeticException exception = new ArithmeticException("infinity + 1");
			exception.printStackTrace(writer1);

			writer1.close();

			event.getChannel().sendMessage("```"+writer.toString()+"```").queue();

			return;
		}

		Pattern pattern = Pattern.compile("(|(\\d*)x )Spiegel");
		Matcher matcher = pattern.matcher(message);
		if(matcher.find())
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

			event.getChannel().sendMessage(count+"x Spiegel").queue();
		}
	}
}
