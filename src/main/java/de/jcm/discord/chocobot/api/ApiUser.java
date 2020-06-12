package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;
import net.dv8tion.jda.api.entities.User;

public class ApiUser
{
	private long userId;

	public ApiUser(long userId)
	{
		this.userId = userId;
	}

	public long getUserId()
	{
		return userId;
	}

	public void setUserId(long userId)
	{
		this.userId = userId;
	}

	public User toUser()
	{
		return ChocoBot.jda.getUserById(userId);
	}
}
