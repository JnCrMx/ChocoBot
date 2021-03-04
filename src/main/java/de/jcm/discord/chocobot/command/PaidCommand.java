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

            return executePaid(message, channel, guild, settings, args);
        }
        else
        {
            channel.sendMessage(ChocoBot.translateError(settings, "command.pair.error.not_enough", getCost())).queue();
            return false;
        }
    }

    protected abstract boolean executePaid(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args);
    protected abstract int getCost();

    @Nullable
    protected String getPaidHelpText(GuildSettings settings)
    {
        return super.getHelpText(settings);
    }

    @Override
    protected final @Nullable String getHelpText(GuildSettings settings)
    {
        String text = getPaidHelpText(settings);
        if(text==null)
            return null;

        return text+" "+settings.translate("command.paid.help_cost", getCost());
    }
}
