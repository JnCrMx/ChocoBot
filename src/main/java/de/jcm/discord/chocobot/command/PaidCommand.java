package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.Nullable;

public abstract class PaidCommand extends Command
{
    @Override
    public final boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
    {
        if(DatabaseUtils.getCoins(message.getAuthor().getIdLong(), guild.getIdLong())>=getCost())
        {
            DatabaseUtils.changeCoins(message.getAuthor().getIdLong(), guild.getIdLong(), -getCost());

            return executePaid(message, channel, guild, args);
        }
        else
        {
            channel.sendMessage(
                    ChocoBot.errorMessage(
                            "Du hast nicht genug Coins, um diesen Befehl auszuführen!\n"+
                                    "Die Kosten betragen "+getCost()+" Coins."))
                   .queue();
            return false;
        }
    }

    protected abstract boolean executePaid(Message message, TextChannel channel, Guild guild, String... args);
    protected abstract int getCost();
    @Nullable
    protected abstract String getPaidHelpText();

    @Override
    protected final @Nullable String getHelpText()
    {
        if(getPaidHelpText()==null)
            return null;

        return getPaidHelpText()+" (Kosten: "+getCost()+" Coins)";
    }
}
