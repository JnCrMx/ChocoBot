package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Guild;

import javax.ws.rs.PathParam;
import java.util.Objects;

public class GuildParam
{
	@PathParam("guild")
	private long guildId;

	public long getGuildId()
	{
		return guildId;
	}

	public boolean checkAccess(ApiUser user)
	{
		return Objects.requireNonNull(toGuild())
		              .isMember(Objects.requireNonNull(user.toUser()));
	}

	public Guild toGuild()
	{
		return ChocoBot.jda.getGuildById(guildId);
	}
}
