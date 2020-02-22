package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandArch extends Command
{
    public CommandArch()
    {
    }

    public boolean execute(Message message, TextChannel channel, String... args)
    {
        channel.sendMessage("I use arch btw").queue();
        return true;
    }

    @NotNull
    public String getKeyword()
    {
        return "arch";
    }

    @Nullable
    public String getHelpText()
    {
        return null;
    }
}
