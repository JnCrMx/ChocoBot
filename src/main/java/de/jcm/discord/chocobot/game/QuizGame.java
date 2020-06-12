package de.jcm.discord.chocobot.game;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class QuizGame extends Game
{
	private static List<QuizGame.QuizQuestion> quizQuestions;
	private static final String[] EMOJIS_ANSWER = new String[]{"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	private static final int WINNER_REWARD = 50;
	private Message quizMessage;
	private int rightAnswer;
	private Map<Member, Integer> answers;
	private Map<Member, Long> answerTimes;

	public QuizGame(Member sponsor, TextChannel gameChannel)
	{
		super(sponsor, gameChannel);
	}

	private static void loadQuestions()
	{
		quizQuestions = new ArrayList<>();
		Scanner scanner = new Scanner(ChocoBot.class.getResourceAsStream("/quiz.txt"));
		String lastQuestion = null;
		ArrayList<String> answers = new ArrayList<>();

		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			if (!line.isBlank())
			{
				if (line.startsWith("\t"))
				{
					answers.add(line.substring(1));
				}
				else
				{
					if (lastQuestion != null)
					{
						quizQuestions.add(new QuizGame.QuizQuestion(lastQuestion, answers.toArray(String[]::new)));
					}

					lastQuestion = line;
					answers.clear();
				}
			}
		}

		if (lastQuestion != null)
		{
			quizQuestions.add(new QuizGame.QuizQuestion(lastQuestion, answers.toArray(String[]::new)));
		}

		scanner.close();
	}

	public static void prepare()
	{
		loadQuestions();
	}

	protected void play()
	{
		Random random = new Random();
		QuizGame.QuizQuestion question = quizQuestions.get(random.nextInt(quizQuestions.size()));
		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_GAME);
		builder.setTitle("Quiz");
		builder.setDescription(question.question);
		List<String> answerList = new ArrayList<>(List.of(question.answers));
		Collections.shuffle(answerList);

		for (int i = 0; i < answerList.size(); ++i)
		{
			builder.addField(EMOJIS_ANSWER[i], answerList.get(i), true);
			if (answerList.get(i).equals(question.answers[0]))
			{
				this.rightAnswer = i;
			}
		}

		this.answers = new HashMap<>();
		this.answerTimes = new HashMap<>();
		this.gameChannel.sendMessage(builder.build()).queue((m) ->
		{
			this.quizMessage = m;

			for (int i = 0; i < question.answers.length; ++i)
			{
				this.quizMessage.addReaction(EMOJIS_ANSWER[i]).queue();
			}

			this.state = GameState.RUNNING;
			this.quizMessage.delete().queueAfter(10L, TimeUnit.SECONDS, (v2) ->
			{
				this.state = GameState.FINISHED;
				EmbedBuilder builder1 = new EmbedBuilder();
				builder1.setColor(ChocoBot.COLOR_GAME);
				builder1.setTitle("Gewinner");
				builder1.setDescription("Die folgenden Teilnehmer haben richtig geantwortet:");

				Member[] rightAnswers = answers.entrySet().stream().filter(e->e.getValue()==rightAnswer)
						.sorted(Comparator.comparingLong(e -> answerTimes.get(e.getKey())))
						.flatMap(e->Stream.of(e.getKey())).toArray(Member[]::new);

				for(int i=0; i<rightAnswers.length; i++)
				{
					Member member = rightAnswers[i];

					int reward = WINNER_REWARD;
					reward += (players.size()-i-1)*10;

					if(member.getIdLong() == sponsor.getIdLong())
					{
						reward += getSponsorCost();
						builder1.addField(member.getEffectiveName(), "+" + (reward-getSponsorCost()) + " Coins",
								false);
					}
					else
					{
						builder1.addField(member.getEffectiveName(), "+" + reward + " Coins", false);
					}
					DatabaseUtils.changeCoins(member.getIdLong(), guild.getIdLong(), reward);
				}

				if (builder1.getFields().isEmpty())
				{
					builder1.setDescription("Niemand hat richtig geantwortet \ud83d\ude2d");
				}

				if(Stream.of(rightAnswers).noneMatch(e->e.getIdLong()==sponsor.getIdLong()))
				{
					builder1.addField(sponsor.getEffectiveName(), "-" + getSponsorCost() + " Coins",
							false);
				}

				builder1.setFooter(question.question + " " + question.answers[0]);
				this.gameChannel.sendMessage(builder1.build()).queue();
				this.end();
			});
		});
	}

	public String getName()
	{
		return "Quiz";
	}

	public int getSponsorCost()
	{
		return 100;
	}

	public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event)
	{
		super.onMessageReactionAdd(event);
		if (!event.getUser().isBot())
		{
			if (this.state == GameState.RUNNING && event.getMessageIdLong() == this.quizMessage.getIdLong() && this.players.contains(event.getMember()) && event.getReactionEmote().isEmoji() && List.of(EMOJIS_ANSWER).contains(event.getReactionEmote().getEmoji()))
			{
				this.answers.put(event.getMember(), Arrays.binarySearch(EMOJIS_ANSWER, event.getReactionEmote().getEmoji()));
				this.answerTimes.put(event.getMember(), System.currentTimeMillis());
			}
		}
	}

	static class QuizQuestion
	{
		final String question;
		final String[] answers;

		QuizQuestion(String question, String[] answers)
		{
			this.question = question;
			this.answers = answers;
		}
	}
}
