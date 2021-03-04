package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public abstract class Command
{
	private static final Logger logger_ = LoggerFactory.getLogger(Command.class);
	final Logger logger = LoggerFactory.getLogger(this.getClass());
	static final HashMap<String, Command> commands = new HashMap<>();

	protected Command()
	{
	}

	public abstract boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args);

	@NotNull
	protected abstract String getKeyword();

	@Nullable
	protected String getHelpText(GuildSettings settings)
	{
		return settings.translate("command."+getKeyword()+".help");
	}

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

	public void showUsage(TextChannel channel, GuildSettings settings)
	{
		String usage = getUsage(settings);
		if(usage==null)
		{
			return;
		}

		String prefix = settings.getPrefix();

		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(prefix+getKeyword());

		usage = usage.replace("$p", prefix);
		usage = usage.replace("$c", prefix+getKeyword());
		usage = usage.replace("$C", getKeyword());

		String helpText = getHelpText(settings);
		if(helpText!=null)
		{
			usage = usage.replace("$h", helpText);
		}

		eb.setDescription(usage);

		eb.setColor(ChocoBot.COLOR_COOKIE);
		channel.sendMessage(eb.build()).queue();
	}

	@Nullable
	protected String getUsage(GuildSettings settings)
	{
		return settings.translate("command."+getKeyword()+".usage");
	}

	@Nullable
	public static Command getCommand(String keyword)
	{
		return commands.getOrDefault(keyword.toLowerCase(), null);
	}
}
