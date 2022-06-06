package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CommandAlias extends Command
{
	private static final Pattern RANGE_SPEC = Pattern.compile("\\{(?<start>\\d+)-(?<end>\\d+|n)}");
	private static final Pattern LIST_SPEC = Pattern.compile("\\{(?<numbers>\\d*(?:,\\d+)*)}");

	private final String keyword;
	private final Command command;
	private final String[] argumentSpec;

	private CommandAlias(String keyword, Command command, String arguments)
	{
		this.keyword = keyword;
		this.command = command;
		this.argumentSpec = arguments.split(" ");
	}

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		List<String> newArgs = new ArrayList<>();
		for(String s : argumentSpec)
		{
			Matcher m;
			if((m=LIST_SPEC.matcher(s)).matches())
			{
				String list = m.group("numbers");
				Stream.of(list.split(","))
				      .mapToInt(Integer::parseInt)
				      .mapToObj(i->args[i])
				      .forEach(newArgs::add);
			}
			else if((m=RANGE_SPEC.matcher(s)).matches())
			{
				int start = Integer.parseInt(m.group("start"));
				int end = m.group("end").equals("n") ? (args.length-1) : Integer.parseInt(m.group("end"));
				IntStream.rangeClosed(start, end)
				         .mapToObj(i->args[i])
				         .forEach(newArgs::add);
			}
			else
			{
				newArgs.add(s);
			}
		}

		String[] res;
		if(command.multipleArguments())
		{
			res = newArgs.toArray(String[]::new);
		}
		else
		{
			res = new String[] {String.join(" ", newArgs)};
		}

		return command.execute(message, channel, guild, settings, res);
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return keyword;
	}

	static CommandAlias forGuild(Guild guild, String keyword)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT command, arguments FROM command_aliases WHERE guild = ? AND keyword = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, keyword);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				String commandName = resultSet.getString("command");
				String arguments = resultSet.getString("arguments");

				Command command = CommandListener.findCommand(commandName, guild, false);
				if(command == null)
					return null;

				return new CommandAlias(keyword, command, arguments);
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return null;
	}
}
