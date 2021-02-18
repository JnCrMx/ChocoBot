package de.jcm.discord.chocobot;

import de.jcm.discord.chocobot.game.BlockGame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Random;

public class BlockTest
{
	private static final int TRIES = 1_000_000;
	private static final int TRY_MAGNITUDE = (int) Math.ceil(Math.log10(TRIES));

	@Test
	void testProbabilities()
	{
		Random random = new Random();
		BlockGame.Prize[] prizes = BlockGame.PRIZES;
		int[] stats = new int[prizes.length];

		for(int q = 0; q < TRIES; q++)
		{
			double rnd = random.nextDouble();
			double sum = 0.0;

			for(int i = 0, prizesLength = prizes.length; i < prizesLength; i++)
			{
				BlockGame.Prize prize = prizes[i];
				if(rnd >= sum && rnd <= sum + prize.getProbability())
				{
					stats[i]++;
					break;
				}
				sum += prize.getProbability();
			}
		}

		for(int i=0; i<stats.length; i++)
		{
			double probability = ((double)stats[i]) / TRIES;
			double diff = Math.abs(probability - prizes[i].getProbability());
			double diffRelative = diff / prizes[i].getProbability();

			System.out.printf(Locale.ROOT,
			                  "Prize %d (%7d Coins): " +
					                  "desired probability: %8.5f%%, " +
					                  "real probability: %8.5f%%, " +
					                  "per %"+TRY_MAGNITUDE+"d: %"+TRY_MAGNITUDE+"d, " +
					                  "diff: %.5f%% (%.2f%%)\n",
			                  i, prizes[i].getCoins(),
			                  prizes[i].getProbability()*100, probability*100,
			                  TRIES, stats[i], diff*100, diffRelative*100);
			Assertions.assertEquals(prizes[i].getProbability(), probability, 0.01,
			                        "probability difference too big");
		}
	}
}
