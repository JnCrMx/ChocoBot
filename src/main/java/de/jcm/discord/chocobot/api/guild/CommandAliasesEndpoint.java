package de.jcm.discord.chocobot.api.guild;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.api.ApiUser;
import de.jcm.discord.chocobot.api.GuildParam;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/{guild}/guild/settings/command_aliases")
public class CommandAliasesEndpoint
{
	public static class CommandAlias
	{
		public String keyword;
		public String command;
		public String arguments;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<CommandAlias> getAliases(@BeanParam GuildParam guildParam,
	                                             @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		if(!guildParam.checkAccess(user))
			throw new ForbiddenException();

		Guild guild = guildParam.toGuild();
		GuildSettings settings = DatabaseUtils.getSettings(guild);

		Member member = guild.retrieveMemberById(user.getUserId()).onErrorMap(t->null).complete();
		if(member == null
				|| (settings == null && !member.isOwner())
				|| (settings != null && !settings.isOperator(member)))
			throw new ForbiddenException();

		List<CommandAlias> aliases = new ArrayList<>();
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT keyword, command, arguments FROM command_aliases WHERE guild = ?"))
		{
			statement.setLong(1, guild.getIdLong());

			ResultSet resultSet = statement.executeQuery();
			while(resultSet.next())
			{
				CommandAlias alias = new CommandAlias();
				alias.keyword = resultSet.getString("keyword");
				alias.command = resultSet.getString("command");
				alias.arguments = resultSet.getString("arguments");

				aliases.add(alias);
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		return aliases;
	}
}
