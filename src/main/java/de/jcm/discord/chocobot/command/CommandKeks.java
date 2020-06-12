package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.stream.Stream;

public class CommandKeks extends Command
{
	public CommandKeks()
	{
	}

	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		HttpClient client = HttpClient.newBuilder().build();

		try
		{
			HttpRequest request = HttpRequest.newBuilder(new URI("https://www.ecosia.org/images?q=american+cookie")).GET().build();
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			String page = response.body();
			Scanner scanner = new Scanner(page);
			Stream<MatchResult> resultStream = scanner.findAll("data-src=\"(.*)\"");
			MatchResult[] results = resultStream.toArray(MatchResult[]::new);
			Random rand = new Random();
			MatchResult result = results[rand.nextInt(results.length)];
			String url = result.group(1);
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Keks");
			builder.setImage(url);
			builder.setColor(ChocoBot.COLOR_COOKIE);
			channel.sendMessage(builder.build()).queue();
			return true;
		}
		catch (IOException | InterruptedException | URISyntaxException var15)
		{
			var15.printStackTrace();
			return false;
		}
	}

	@NotNull
	public String getKeyword()
	{
		return "keks";
	}

	public String getHelpText()
	{
		return "Zeige einen Keks an.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return null;
	}
}
