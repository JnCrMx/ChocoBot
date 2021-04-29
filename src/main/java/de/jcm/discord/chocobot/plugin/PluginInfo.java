package de.jcm.discord.chocobot.plugin;

import com.fasterxml.jackson.annotation.JsonAlias;

public class PluginInfo
{
	@JsonAlias("main")
	public String mainClass;
	public String name;

	public boolean isValid()
	{
		return mainClass != null && !mainClass.isBlank();
	}
}
