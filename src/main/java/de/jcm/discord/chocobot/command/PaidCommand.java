package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.Nullable;

import static de.jcm.discord.chocobot.ChocoBot.sendTempMessage;

public abstract class PaidCommand extends Command
{
    @Override
    public final boolean execute(Message message, TextChannel channel, String... args)
    {
        if(DatabaseUtils.getCoins(message.getAuthor().getIdLong())>=getCost())
        {
            DatabaseUtils.changeCoins(message.getAuthor().getIdLong(), -getCost());

            return executePaid(message, channel, args);
        }
        else
        {
            sendTempMessage(channel,
                    ChocoBot.errorMessage(
                            "Du hast nicht genug Coins, um diesen Befehl auszuführen!\n"+
                                    "Die Kosten betragen "+getCost()+" Coins."));
            return false;
        }
    }

    protected abstract boolean executePaid(Message message, TextChannel channel, String... args);
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
