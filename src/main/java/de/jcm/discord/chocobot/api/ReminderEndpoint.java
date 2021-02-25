package de.jcm.discord.chocobot.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.api.data.ChannelInfo;
import de.jcm.discord.chocobot.api.data.UserData;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.glassfish.jersey.media.multipart.FormDataParam;

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

@Path("/reminders")
public class ReminderEndpoint
{
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Reminder> fetch(@QueryParam("active") @DefaultValue("false") boolean active,
	                            @QueryParam("type") @DefaultValue("remindee") String type,
	                            @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");

		try(Connection connection = ChocoBot.getDatabase())
		{
			String sql = "SELECT * FROM reminders WHERE ";
			if(active)
			{
				sql+="done = 0 AND ";
			}

			switch(type)
			{
				case "remindee":
					sql+="uid = ?";
					break;
				case "reminder":
					sql+="issuer = ?";
					break;
				case "all":
					sql+="(uid = ? OR issuer = ?)";
					break;
				default:
					throw new IllegalArgumentException("type invalid. allowed types are: remindee, reminder, all");
			}

			try(PreparedStatement statement = connection.prepareStatement(sql))
			{
				statement.setLong(1, user.getUserId());
				if(type.equals("all"))
				{
					statement.setLong(2, user.getUserId());
				}

				ResultSet resultSet = statement.executeQuery();

				ArrayList<Reminder> reminders = new ArrayList<>();
				while(resultSet.next())
				{
					Reminder reminder = new Reminder();
					reminder.id = resultSet.getInt("id");
					reminder.remindee = ChocoBot.provideUser(resultSet.getLong("uid"), UserData::getTag, "?");
					reminder.message = resultSet.getString("message");
					reminder.time = resultSet.getLong("time");
					reminder.reminder = ChocoBot.provideUser(resultSet.getLong("issuer"), UserData::getTag, "?");;
					reminder.done = resultSet.getBoolean("done");

					GuildChannel channel = ChocoBot.jda.getGuildChannelById(resultSet.getLong("channel"));
					if(channel != null)
					{
						reminder.channel = ChannelInfo.fromChannel(channel);
					}

					reminders.add(reminder);
				}

				return reminders;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		return null;
	}

	@POST
	@Path("/cancel")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public boolean cancel(@FormDataParam("id") int id,
	                      @Context ContainerRequestContext request)
	{
		ApiUser user = (ApiUser) request.getProperty("user");
		try(Connection connection = ChocoBot.getDatabase();
			PreparedStatement statement = connection.prepareStatement(
					"DELETE FROM reminders WHERE id = ? AND (uid = ? OR issuer = ?) AND done = 0"))
		{
			statement.setInt(1, id);
			statement.setLong(2, user.getUserId());
			statement.setLong(3, user.getUserId());
			return statement.executeUpdate() == 1;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return false;
	}

	private static class Reminder
	{
		public int id;
		public String remindee;
		public String message;
		public long time;
		public String reminder;
		public boolean done;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public ChannelInfo channel;
	}
}
