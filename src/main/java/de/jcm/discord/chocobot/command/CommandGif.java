package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

public class CommandGif extends PaidCommand
{
	private final WebTarget searchTarget;
	private final Random random = new Random();

	public CommandGif()
	{
		this.searchTarget = ChocoBot.client.target("https://api.tenor.com/v1/search");
	}

	public boolean executePaid(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		String q = String.join(" ", args);
		Response response = this.searchTarget.queryParam("q", q).request(new String[]{"application/json"}).get();
		HashMap<?, ?> map = (HashMap<?,?>) response.readEntity(HashMap.class);
		ArrayList<?> results = (ArrayList<?>) map.get("results");
		if (results.size() == 0)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.gif.error.noent")).queue();
			return false;
		}
		else
		{
			LinkedHashMap<?, ?> result = (LinkedHashMap<?,?>) results.get(this.random.nextInt(results.size()));
			String url = (String) result.get("url");
			channel.sendMessage(url).queue();
			return true;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "gif";
	}

	@Override
	protected int getCost()
	{
		return 10;
	}
}
