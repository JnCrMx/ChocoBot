package de.jcm.discord.chocobot.command.warn;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.regex.Pattern;

public class CommandWarn extends Command
{
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		Member warnerMember = message.getMember();

		assert warnerMember != null;

		if(!settings.isOperator(warnerMember))
		{
			channel.sendMessage(ChocoBot.errorMessage("Du darfst keine Leute verwarnen!")).queue();
			return false;
		}
		else if (args.length < 2)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du musst einen Verwarnten und einen Grund angeben!")).queue();
			return false;
		}
		else
		{
			Pattern pattern = Pattern.compile("<@(|!)([0-9]*)>");
			if (!message.getMentionedUsers().isEmpty() && pattern.matcher(args[0]).matches())
			{
				User warned = message.getMentionedUsers().get(0);
				User warner = message.getAuthor();
				String[] reasonArray = new String[args.length - 1];
				System.arraycopy(args, 1, reasonArray, 0, reasonArray.length);
				String reason = String.join(" ", reasonArray);
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Verwarnung");
				builder.setColor(ChocoBot.COLOR_WARN);
				builder.addField("Verwarnter", warned.getAsTag(), false);
				builder.addField("Verwarner", warner.getAsTag(), false);
				builder.addField("Grund", reason, false);

				TextChannel warnTextChannel = settings.getWarningChannel();
				warnTextChannel.sendMessage(builder.build()).queue((s) -> {
					try
					{
						try(Connection connection = ChocoBot.getDatabase();
						    PreparedStatement insertWarning = connection.prepareStatement("INSERT INTO warnings(uid, guild, reason, time, warner, message) VALUES(?, ?, ?, ?, ?, ?)",
						                                                                  Statement.RETURN_GENERATED_KEYS))
						{
							insertWarning.setLong(1, warned.getIdLong());
							insertWarning.setLong(2, guild.getIdLong());
							insertWarning.setString(3, reason);
							insertWarning.setLong(4, System.currentTimeMillis());
							insertWarning.setLong(5, warner.getIdLong());
							insertWarning.setLong(6, s.getIdLong());
							insertWarning.executeUpdate();

							try(ResultSet keys = insertWarning.getGeneratedKeys())
							{
								if(keys.next())
								{
									builder.setAuthor("[" + keys.getInt(1) + "]");
								}
							}
						}
						if (warnTextChannel.getIdLong() != channel.getIdLong())
						{
							channel.sendMessage(builder.build()).queue();
						}
						s.editMessage(builder.build()).queue();

						if (warned.getIdLong() == ChocoBot.jda.getSelfUser().getIdLong())
						{
							channel.sendMessage("Es tut mir leid! :cry:").queue();
						}
					}
					catch (SQLException var9)
					{
						var9.printStackTrace();
					}

				});
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Du musst einen Verwarnten angeben!")).queue();
				return false;
			}

			return true;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "warn";
	}

	public String getHelpText()
	{
		return "Verwarne einen Nutzer.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c <Nutzer> <Grund> (nur Operatoren) : %h";
	}
}
