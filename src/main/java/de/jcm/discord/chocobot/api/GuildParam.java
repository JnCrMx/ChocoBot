package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

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
		Guild guild = toGuild();
		User user1 = user.toUser();

		if(guild == null || user1 == null )
			return false;
		return guild.isMember(user1);
	}

	public Guild toGuild()
	{
		return ChocoBot.jda.getGuildById(guildId);
	}
}
