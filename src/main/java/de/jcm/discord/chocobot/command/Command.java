package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public abstract class Command
{
	private static final Logger logger_ = LoggerFactory.getLogger(Command.class);
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	static final HashMap<String, Command> commands = new HashMap<>();

	protected Command()
	{
	}

	public abstract boolean execute(Message message, TextChannel channel, String... args);

	@NotNull
	protected abstract String getKeyword();

	@Nullable
	protected abstract String getHelpText();

	public boolean multipleArguments()
	{
		return true;
	}

	public boolean usableEverywhere()
	{
		return false;
	}

	public static void registerCommand(Command command)
	{
		String keyword = command.getKeyword().toLowerCase();
		if (commands.containsKey(keyword))
		{
			throw new IllegalArgumentException("Command with same keyword already exists");
		}
		else
		{
			commands.put(keyword, command);
			logger_.info("Registered command "+keyword+": "+command);
		}
	}

	public void showUsage(TextChannel channel)
	{
		if(getUsage()==null)
		{
			return;
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(ChocoBot.prefix+getKeyword());

		String usage = getUsage();
		usage = usage.replace("%p", ChocoBot.prefix);
		usage = usage.replace("%c", ChocoBot.prefix+getKeyword());
		usage = usage.replace("%C", getKeyword());
		if(getHelpText()!=null)
		{
			usage = usage.replace("%h", getHelpText());
		}

		eb.setDescription(usage);

		eb.setColor(ChocoBot.COLOR_COOKIE);
		sendTempMessage(channel, eb.build());
	}

	@Nullable
	protected abstract String getUsage();

	@Nullable
	public static Command getCommand(String keyword)
	{
		return commands.getOrDefault(keyword.toLowerCase(), null);
	}
}
