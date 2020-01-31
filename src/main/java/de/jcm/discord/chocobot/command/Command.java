package de.jcm.discord.chocobot.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public abstract class Command
{
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	static final HashMap<String, Command> commands = new HashMap<>();

	public Command()
	{
	}

	public abstract boolean execute(Message message, TextChannel channel, String... args);

	@NotNull
	public abstract String getKeyword();

	@Nullable
	public abstract String getHelpText();

	public boolean multipleArguments()
	{
		return true;
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
		}
	}

	@Nullable
	public static Command getCommand(String keyword)
	{
		return commands.getOrDefault(keyword.toLowerCase(), null);
	}
}
