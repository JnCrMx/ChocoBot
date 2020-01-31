package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CommandBugReport extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, String... args)
	{
		try
		{
			Instant time = Instant.now();

			File file = new File(ChocoBot.bugreportDirectory, time.toEpochMilli() + ".txt");
			PrintStream p = new PrintStream(file);

			p.println("User-ID: "+message.getAuthor().getId());
			p.println("User-Tag: "+message.getAuthor().getAsTag());

			p.println("Guild-ID: "+channel.getGuild().getId());
			p.println("Guild-Name: "+channel.getGuild().getName());

			p.println("Channel-ID: "+channel.getId());
			p.println("Channel-Name: "+channel.getName());

			p.println("Timestamp: " + time.toEpochMilli());
			p.println("Time: "+ LocalDateTime.ofInstant(time, ZoneId.systemDefault()));

			if(args.length==0)
			{
				p.println("===NO MESSAGE===");
			}
			else
			{
				p.println("===MESSAGE START===");
				p.println(args[0]);
				p.println("===MESSAGE END===");
			}

			p.flush();
			p.close();

			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("Fehlermeldung");
			eb.setColor(ChocoBot.COLOR_COOKIE);
			eb.setDescription("Deine Fehlermeldung wurde gespeichert!");

			channel.sendMessage(eb.build()).queue();

			return true;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();

			channel.sendMessage(ChocoBot.errorMessage("Deine Fehlermeldung konnte nicht gespeichert werden!"))
				.queue();

			return false;
		}
	}

	@Override
	public @NotNull String getKeyword()
	{
		return "bugreport";
	}

	@Override
	public @Nullable String getHelpText()
	{
		return "Sende eine Fehlermeldung.";
	}

	@Override
	public boolean multipleArguments()
	{
		return false;
	}
}
