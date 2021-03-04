package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandArch extends Command
{
    public CommandArch()
    {
    }

    public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
    {
        channel.sendMessage(settings.translate("command.secret.arch.message")).queue();
        return true;
    }

    @NotNull
    public String getKeyword()
    {
        return "arch";
    }

    @Nullable
    public String getHelpText(GuildSettings settings)
    {
        return null;
    }

    @Override
    protected @Nullable String getUsage(GuildSettings settings)
    {
        return null;
    }

    @Override
    public boolean usableEverywhere()
    {
        return true;
    }
}
