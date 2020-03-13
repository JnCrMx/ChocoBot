package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class CommandWarn extends Command
{
	private PreparedStatement insertWarning;

	public CommandWarn()
	{
		try
		{
			this.insertWarning = ChocoBot.database.prepareStatement("INSERT INTO warnings(uid, reason, time, warner, message) VALUES(?, ?, ?, ?, ?)");
		}
		catch (SQLException var2)
		{
			var2.printStackTrace();
		}

	}

	public boolean execute(Message message, TextChannel channel, String... args)
	{
		Member warnerMember = message.getMember();

		assert warnerMember != null;

		if (warnerMember.getRoles().stream().noneMatch((r) ->
				ChocoBot.operatorRoles.contains(r.getId())))
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
				GuildChannel warnChannel = ChocoBot.jda.getGuildChannelById(ChocoBot.warningChannel);

				assert warnChannel != null;

				if (warnChannel.getType() == ChannelType.TEXT)
				{
					TextChannel warnTextChannel = (TextChannel) warnChannel;
					warnTextChannel.sendMessage(builder.build()).queue((s) ->
					{
						try
						{
							this.insertWarning.setLong(1, warned.getIdLong());
							this.insertWarning.setString(2, reason);
							this.insertWarning.setLong(3, System.currentTimeMillis());
							this.insertWarning.setLong(4, warner.getIdLong());
							this.insertWarning.setLong(5, s.getIdLong());
							this.insertWarning.execute();
							if (warnTextChannel.getIdLong() != channel.getIdLong())
							{
								channel.sendMessage(builder.build()).queue();
							}

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

				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Du musst einen Verwarnten angeben!")).queue();
				return false;
			}
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
