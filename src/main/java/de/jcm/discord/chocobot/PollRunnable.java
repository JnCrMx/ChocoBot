package de.jcm.discord.chocobot;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PollRunnable implements Runnable
{
	private static final Pattern emojiPattern = Pattern.compile("(<[:A-Za-z0-9_+]+>|:[A-Za-z1-9_+]+:)");

	@Override
	public void run()
	{
		TextChannel channel = (TextChannel) ChocoBot.jda.getGuildChannelById(ChannelType.TEXT, ChocoBot.pollChannel);
		channel.getIterableHistory().forEachRemaining(message -> {
			String[] parts = message.getContentRaw().split("\n");
			Optional<String> answerString = Stream.of(parts)
			                              .map(String::strip)
			                              .filter(s->s.split("\\|").length>1)
			                              .findAny();
			if(answerString.isEmpty())
				return true;
			Answer[] answers = Stream.of(answerString.get().split("\\|"))
			                         .map(EmojiParser::parseToAliases)
			                         .map(String::strip)
			                         .filter(emojiPattern.asPredicate())
			                         .map(Answer::new).
					                         toArray(Answer[]::new);
			ArrayList<ImmutablePair<Answer, Integer>> pairs = message
					.getReactions().stream()
					.flatMap((Function<MessageReaction, Stream<ImmutablePair<Answer, Integer>>>)
							         r -> Stream.of(answers)
							                    .filter(a -> a.test(r.getReactionEmote()))
							                    .map(a -> new ImmutablePair<>(a, r.getCount())))
					.collect(Collectors.toCollection(ArrayList::new));

			Stream.of(answers)
			      .filter(a->pairs.stream()
			                       .map(Pair::getKey)
			                       .noneMatch(o->o.equals(a)))
			      .map(a->new ImmutablePair<>(a, 0))
			      .forEach(pairs::add);

			String question = message.getContentRaw().replace(answerString.get(), "");
			question = question.replaceAll("<@.?\\d*>", "");
			question = question.strip();

			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement insertQuestion = connection.prepareStatement(
			    		"REPLACE INTO polls (message, question) VALUES (?, ?)");
			    PreparedStatement insertAnswer = connection.prepareStatement(
			    		"REPLACE INTO poll_answers (answer, poll, votes) VALUES (?, ?, ?)"))
			{
				insertQuestion.setLong(1, message.getIdLong());
				insertQuestion.setString(2, question);
				insertQuestion.execute();

				for(ImmutablePair<Answer, Integer> pair : pairs)
				{
					insertAnswer.setString(1, pair.getKey().answer);
					insertAnswer.setLong(2, message.getIdLong());
					insertAnswer.setInt(3, pair.getValue());
					insertAnswer.execute();
				}
			}
			catch(SQLException throwables)
			{
				throwables.printStackTrace();
			}

			return true;
		});
	}

	private class Answer implements Predicate<MessageReaction.ReactionEmote>
	{
		private String emojiAlias;
		private String answer;

		public Answer(String answer)
		{
			Matcher matcher = emojiPattern.matcher(answer);
			if(matcher.find())
			{
				emojiAlias = matcher.group();
				String str = answer.substring(matcher.end()).trim();

				if(str.startsWith("- "))
					str = str.replaceFirst("- ", "");
				if(str.startsWith("für "))
					str = str.replaceFirst("für ", "");

				this.answer = str.trim();
			}
		}

		@Override
		public boolean test(MessageReaction.ReactionEmote emote)
		{
			if(emote.isEmoji())
			{
				return emojiAlias.equals(EmojiParser.parseToAliases(emote.getEmoji()));
			}
			else if(emote.isEmote())
			{
				return emojiAlias.equals(emote.getEmote().getAsMention());
			}
			return false;
		}

		@Override
		public String toString()
		{
			return emojiAlias + " " + answer;
		}
	}
}
