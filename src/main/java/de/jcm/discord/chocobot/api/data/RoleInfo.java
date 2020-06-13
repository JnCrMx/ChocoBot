package de.jcm.discord.chocobot.api.data;

import net.dv8tion.jda.api.entities.Role;

public class RoleInfo
{
	public String id;
	public String name;
	public int color;

	public static RoleInfo fromRole(Role role)
	{
		RoleInfo info = new RoleInfo();

		info.id = role.getId();
		info.name = role.getName();
		info.color = role.getColorRaw();

		return info;
	}
}
