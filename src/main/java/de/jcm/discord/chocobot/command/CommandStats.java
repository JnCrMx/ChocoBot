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

		builder.setTitle("Server-Statistik");
		builder.setTimestamp(LocalDateTime.now());
		builder.setColor(ChocoBot.COLOR_COOKIE);
		builder.setAuthor(guild.getName());
		builder.setThumbnail(guild.getIconUrl());

		guild.loadMembers().onSuccess(members->{
			builder.addField("Mitglieder", String.valueOf(members.size()), true);
			builder.addField("Menschen", String.valueOf(members.stream().filter(m->!m.getUser().isBot()).count()), true);
			builder.addField("Bots", String.valueOf(members.stream().filter(m->m.getUser().isBot()).count()), true);

			builder.addField("Owner", Objects.requireNonNull(guild.getOwner()).getUser().getAsTag(), true);
			builder.addField("Erstellungszeit", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(guild.getTimeCreated()), true);

			builder.addBlankField(true);

			builder.addField("Boost-Level", String.valueOf(guild.getBoostTier().getKey()), true);
			builder.addField("Boosts", String.valueOf(guild.getBoostCount()), true);

			builder.addBlankField(true);

			builder.addField("normale Emojis", guild.getEmotes().stream().filter(e->!e.isAnimated()).count()+"/"+guild.getMaxEmotes(), true);
			builder.addField("animierte Emojis", guild.getEmotes().stream().filter(Emote::isAnimated).count()+"/"+guild.getMaxEmotes(), true);

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

	@Override
	protected @Nullable String getHelpText()
	{
		return "Zeigt Statistiken über den Server an.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return "%c : %h";
	}
}
