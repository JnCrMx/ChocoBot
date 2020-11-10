package de.jcm.discord.chocobot.api.data;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.function.Function;

public class UserData
{
	public static final Function<User, UserData> FROM_USER = user -> {
		UserData data = new UserData();
		data.userId = user.getId();
		data.tag = user.getAsTag();
		data.name = user.getName();
		data.avatarUrl = user.getAvatarUrl();

		return data;
	};

	public static final Function<Member, UserData> FROM_MEMBER = member -> {
		UserData data = FROM_USER.apply(member.getUser());

		data.nickname = member.getNickname();
		data.role = member.getRoles().get(0).getName();
		data.roleColor = member.getRoles().get(0).getColorRaw();
		data.timeJoined = member.getTimeJoined().toEpochSecond();

		return data;
	};

	public String userId;
	public String tag;
	public String name;
	public String nickname;
	public String avatarUrl;
	public int coins;
	public OnlineStatus onlineStatus;
	public long timeJoined;
	public String role;
	public int roleColor;
	public boolean operator;

	public String getTag()
	{
		return tag;
	}

	public String getName()
	{
		return name;
	}

	public String getAvatarUrl()
	{
		return avatarUrl;
	}
}
