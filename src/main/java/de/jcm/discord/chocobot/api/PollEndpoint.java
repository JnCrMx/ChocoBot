package de.jcm.discord.chocobot.api;

import de.jcm.discord.chocobot.ChocoBot;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/polls")
public class PollEndpoint
{
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<Poll> all(@Context ContainerRequestContext request)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement pollStatement = connection.prepareStatement("SELECT * FROM polls ORDER BY message DESC");
		    PreparedStatement answerStatement = connection.prepareStatement("SELECT * FROM poll_answers WHERE poll = ?"))
		{
			ResultSet r1 = pollStatement.executeQuery();

			ArrayList<Poll> polls = new ArrayList<>();
			while(r1.next())
			{
				Poll poll = new Poll();
				poll.id = r1.getLong("message");
				poll.question = r1.getString("question");

				int totalVotes = 0;

				answerStatement.setLong(1, poll.id);
				ResultSet r2 = answerStatement.executeQuery();

				poll.answers = new ArrayList<>();
				while(r2.next())
				{
					Answer answer = new Answer();
					answer.answer = r2.getString("answer");
					answer.votes = r2.getInt("votes");

					poll.answers.add(answer);
					totalVotes += answer.votes;
				}
				poll.totalVotes = totalVotes;

				polls.add(poll);
			}

			return polls;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return null;
	}

	private static class Poll
	{
		public long id;
		public String question;
		public int totalVotes;
		public List<Answer> answers;
	}

	private static class Answer
	{
		public String answer;
		public int votes;
	}
}
