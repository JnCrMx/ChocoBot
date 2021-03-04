package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;

public class CommandStats extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(settings.translate("command.stats.title"));
		builder.setTimestamp(LocalDateTime.now());
		builder.setColor(ChocoBot.COLOR_COOKIE);
		builder.setAuthor(guild.getName());
		builder.setThumbnail(guild.getIconUrl());

		guild.loadMembers().onSuccess(members->{
			builder.addField(settings.translate("command.stats.entry.members"), String.valueOf(members.size()), true);
			builder.addField(settings.translate("command.stats.entry.humans"), String.valueOf(members.stream().filter(m->!m.getUser().isBot()).count()), true);
			builder.addField(settings.translate("command.stats.entry.bots"), String.valueOf(members.stream().filter(m->m.getUser().isBot()).count()), true);

			builder.addField(settings.translate("command.stats.entry.owner"), Objects.requireNonNull(guild.getOwner()).getUser().getAsTag(), true);
			builder.addField(settings.translate("command.stats.entry.creation"), DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(guild.getTimeCreated()), true);

			builder.addBlankField(true);

			builder.addField(settings.translate("command.stats.entry.boost_level"), String.valueOf(guild.getBoostTier().getKey()), true);
			builder.addField(settings.translate("command.stats.entry.boosts"), String.valueOf(guild.getBoostCount()), true);

			builder.addBlankField(true);

			builder.addField(settings.translate("command.stats.entry.emojis.normal"), guild.getEmotes().stream().filter(e->!e.isAnimated()).count()+"/"+guild.getMaxEmotes(), true);
			builder.addField(settings.translate("command.stats.entry.emojis.animated"), guild.getEmotes().stream().filter(Emote::isAnimated).count()+"/"+guild.getMaxEmotes(), true);

			builder.addBlankField(true);

			channel.sendMessage(builder.build()).queue();
		});

		return true;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "stats";
	}
}
