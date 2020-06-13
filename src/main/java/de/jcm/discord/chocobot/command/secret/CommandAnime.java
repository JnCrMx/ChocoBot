package de.jcm.discord.chocobot.command.secret;

import de.jcm.discord.chocobot.GuildSettings;
import de.jcm.discord.chocobot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tensorflow.lite.Interpreter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class CommandAnime extends Command
{
	private static int toRGB(float[] color)
	{
		double[] c = IntStream.range(0, color.length)
		                      .mapToDouble(i->color[i])
		                      .map(d->d * 0.5 + 0.5)
		                      .toArray();
		return new Color(
				(float)c[0],
				(float)c[1],
				(float)c[2]).getRGB();
	}

	private static long stringToSeed(String s)
	{
		if (s == null)
		{
			return 0;
		}
		long hash = 0;
		for (char c : s.toCharArray())
		{
			hash = 31L*hash + c;
		}
		return hash;
	}

	private final Interpreter interpreter;
	private final Random random;

	private final int[] inputShape;
	private final int[] outputShape;

	public CommandAnime()
	{
		System.load(new File("./libtensorflowlite_jni.so").getAbsolutePath());

		interpreter = new Interpreter(new File("model.tflite"));
		random = new Random();

		inputShape = interpreter.getInputTensor(0).shape();
		outputShape = interpreter.getOutputTensor(0).shape();
	}

	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		Random r = random;
		if(!message.getMentionedUsers().isEmpty())
		{
			r = new Random(message.getMentionedUsers().get(0).getIdLong());
		}
		else if(args.length == 1)
		{
			try
			{
				r = new Random(Long.parseLong(args[0]));
			}
			catch(NumberFormatException e)
			{
				r = new Random(stringToSeed(args[0]));
			}
		}

		float[][] input = new float
				[inputShape[0]]
				[inputShape[1]];
		float[][][][] output = new float
				[outputShape[0]]
				[outputShape[1]]
				[outputShape[2]]
				[outputShape[3]];

		for(int i = 0; i < inputShape[0]; i++)
		{
			for(int j = 0; j < inputShape[1]; j++)
			{
				input[i][j] = (float) r.nextGaussian();
			}
		}

		interpreter.resizeInput(0, inputShape);
		interpreter.run(input, output);

		BufferedImage image = new BufferedImage(
				outputShape[1],
				outputShape[2],				BufferedImage.TYPE_INT_RGB);

		for(int x = 0; x < outputShape[1]; x++)
		{
			for(int y = 0; y < outputShape[2]; y++)
			{
				image.setRGB(x, y, toRGB(output[0][y][x]));
			}
		}

		try(ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			ImageIO.write(image, "png", out);

			channel.sendFile(out.toByteArray(), "anime.png").queue();

			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "anime";
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return null;
	}

	@Override
	protected @Nullable String getUsage()
	{
		return null;
	}

	@Override
	public boolean usableEverywhere()
	{
		return true;
	}
}
