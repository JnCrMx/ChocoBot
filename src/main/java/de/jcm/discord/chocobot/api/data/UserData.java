package de.jcm.discord.chocobot.api.data;

import net.dv8tion.jda.api.OnlineStatus;

public class UserData
{
	public String userId;
	public String tag;
	public String nickname;
	public String avatarUrl;
	public int coins;
	public OnlineStatus onlineStatus;
	public long timeJoined;
	public String role;
	public int roleColor;
	public boolean operator;
}
