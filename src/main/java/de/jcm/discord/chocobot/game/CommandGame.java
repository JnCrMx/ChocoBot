package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandGame extends Command
{
	private final Class<? extends Game> gameClass;
	private String name;

	public CommandGame(Class<? extends Game> gameClass)
	{
		this.gameClass = gameClass;

		try
		{
			this.name = gameClass.getConstructor(Member.class, TextChannel.class).newInstance(null, null).getName();
		}
		catch (ReflectiveOperationException var3)
		{
			var3.printStackTrace();
		}

	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		try
		{
			Game game = this.gameClass.getConstructor(Member.class, TextChannel.class).newInstance(message.getMember(), channel);
			game.start(args.length >= 1 && args[0].equals("noconfirm"));
			return true;
		}
		catch (ReflectiveOperationException var5)
		{
			var5.printStackTrace();
			return false;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return this.name.toLowerCase();
	}

	@NotNull
	public String getHelpText(GuildSettings settings)
	{
		return settings.translate("command.game.help", this.name);
	}

	@Override
	protected @Nullable String getUsage(GuildSettings settings)
	{
		return settings.translate("command.game.usage");
	}
}
