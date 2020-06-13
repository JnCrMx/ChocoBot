package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandShop extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if(args.length == 0)
		{
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_COINS);
			builder.setTitle("Rollen-Shop");
			builder.setDescription("Folgende Rollen sind verfügbar:");
			builder.setFooter("Nutze \""+settings.getPrefix()+"shop buy <alias>\" um eine Rolle zu kaufen.");

			try(Connection connection = ChocoBot.getDatabase();
			    PreparedStatement statement = connection.prepareStatement("SELECT role, alias, description, cost FROM shop_roles WHERE guild = ?"))
			{
				statement.setLong(1, guild.getIdLong());

				ResultSet resultSet = statement.executeQuery();
				while(resultSet.next())
				{
					long roleId = resultSet.getLong("role");
					String alias = resultSet.getString("alias");
					String description = resultSet.getString("description");
					int cost = resultSet.getInt("cost");

					Role role = guild.getRoleById(roleId);
					if(role == null)
						continue;

					builder.addField(alias+" ("+role.getName()+")",
					                 description+" : "+cost+" Coins",
					                 false);
				}
			}
			catch(SQLException throwables)
			{
				throwables.printStackTrace();
			}

			channel.sendMessage(builder.build()).queue();
			return true;
		}
		else if(args.length == 1 && args[0].equals("inventory"))
		{
			return inventory(message.getAuthor(), guild, settings, channel);
		}
		else if(args.length == 2)
		{
			switch(args[0])
			{
				case "buy":
					return buy(message.getAuthor(), guild, settings, channel, args[1]);
				case "activate":
					return activate(message.getAuthor(), guild, settings, channel, args[1]);
				case "deactivate":
					return deactivate(message.getAuthor(), guild, settings, channel, args[1]);
			}
		}
		return false;
	}

	private boolean inventory(User user, Guild guild, GuildSettings settings, TextChannel channel)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT r.role, r.alias, r.description FROM shop_roles r, shop_inventory i WHERE " +
				                                                              "r.guild = ? AND " +
				                                                              "r.guild = i.guild AND "+
				                                                              "r.role = i.role AND "+
				                                                              "i.user = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setLong(2, user.getIdLong());

			ResultSet resultSet = statement.executeQuery();

			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_COINS);
			builder.setTitle("Rollen Shop");
			builder.setDescription("Du besitzt folgende Rollen:");
			builder.setFooter("Nutze \""+settings.getPrefix()+"shop activate <alias>\" um eine diesen Rollen zu aktivieren.");

			while(resultSet.next())
			{
				long roleId = resultSet.getLong("role");
				String alias = resultSet.getString("alias");
				String description = resultSet.getString("description");

				Role role = guild.getRoleById(roleId);
				if(role == null)
					continue;

				builder.addField(alias+" ("+role.getName()+")",
				                 description,
				                 false);
			}

			channel.sendMessage(builder.build()).queue();
			return true;
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}
		return false;
	}

	private boolean buy(User user, Guild guild, GuildSettings settings, TextChannel channel, String alias)
	{
		long roleId = 0;
		int cost = Integer.MAX_VALUE;

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT role, cost FROM shop_roles WHERE guild = ? AND alias = ?");
		    PreparedStatement testStatement = connection.prepareStatement("SELECT 1 FROM shop_inventory WHERE role = ? AND user = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setString(2, alias);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				roleId = resultSet.getLong("role");
				cost = resultSet.getInt("cost");

				testStatement.setLong(1, roleId);
				testStatement.setLong(2, user.getIdLong());
				if(testStatement.executeQuery().next())
				{
					channel.sendMessage(ChocoBot.errorMessage("Du besitzt diese Rolle bereits!")).queue();
					return false;
				}
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Ich kann diese Rolle nicht finden!")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		if(DatabaseUtils.getCoins(user.getIdLong(), guild.getIdLong()) < cost)
		{
			channel.sendMessage(ChocoBot.errorMessage("Du hast nicht genug Coins um diese Rolle zu erwerben!")).queue();
			return false;
		}

		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement buyStatement = connection.prepareStatement("INSERT INTO shop_inventory (role, user, guild) VALUES (?, ?, ?)"))
		{
			buyStatement.setLong(1, roleId);
			buyStatement.setLong(2, user.getIdLong());
			buyStatement.setLong(3, guild.getIdLong());

			if(buyStatement.executeUpdate() == 0)
			{
				channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
			return false;
		}

		DatabaseUtils.changeCoins(user.getIdLong(), guild.getIdLong(), -cost);

		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_COINS);
		builder.setTitle("Rollen Shop");
		builder.setDescription("Du hast Rolle \""+alias+"\" erfolgreich erworben!");
		builder.setFooter("Nutze \""+settings.getPrefix()+"shop activate "+alias+"\" um sie zu aktivieren.");
		channel.sendMessage(builder.build()).queue();

		return true;
	}

	private boolean activate(User user, Guild guild, GuildSettings settings, TextChannel channel, String alias)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT r.role FROM shop_roles r, shop_inventory i WHERE " +
				                                                              "r.guild = ? AND " +
				                                                              "r.guild = i.guild AND "+
				                                                              "r.role = i.role AND "+
				                                                              "i.user = ? AND "+
				                                                              "r.alias = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setLong(2, user.getIdLong());
			statement.setString(3, alias);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				long roleId = resultSet.getLong("role");

				Member member = guild.getMember(user);
				assert member != null;

				Role role = guild.getRoleById(roleId);
				if(role == null)
				{
					channel.sendMessage(ChocoBot.errorMessage("Die Rolle konnte nicht gefunden werden!")).queue();
					return false;
				}

				if(member.getRoles().contains(role))
				{
					channel.sendMessage(ChocoBot.errorMessage("Du scheinst diese Rolle bereits aktiviert zu haben.")).queue();
					return false;
				}
				try
				{
					guild.addRoleToMember(member, role).reason("Rollen Shop").queue(s -> {
						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COINS);
						builder.setTitle("Rollen Shop");
						builder.setDescription("Die Rolle \"" + alias + "\" wurde erfolgreich aktiviert!");
						builder.setFooter("Nutze \"" + settings.getPrefix() + "shop deactivate " + alias + "\" um sie wieder zu deaktivieren.");
						channel.sendMessage(builder.build()).queue();
					}, e -> {
						e.printStackTrace();
						channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
				}
				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Du scheinst diese Rolle nicht zu besitzen.")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		return false;
	}

	private boolean deactivate(User user, Guild guild, GuildSettings settings, TextChannel channel, String alias)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT r.role FROM shop_roles r, shop_inventory i WHERE " +
				                                                              "r.guild = ? AND " +
				                                                              "r.guild = i.guild AND "+
				                                                              "r.role = i.role AND "+
				                                                              "i.user = ? AND "+
				                                                              "r.alias = ?"))
		{
			statement.setLong(1, guild.getIdLong());
			statement.setLong(2, user.getIdLong());
			statement.setString(3, alias);

			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next())
			{
				long roleId = resultSet.getLong("role");

				Member member = guild.getMember(user);
				assert member != null;

				Role role = guild.getRoleById(roleId);
				if(role == null)
				{
					channel.sendMessage(ChocoBot.errorMessage("Die Rolle konnte nicht gefunden werden!")).queue();
					return false;
				}

				if(!member.getRoles().contains(role))
				{
					channel.sendMessage(ChocoBot.errorMessage("Du scheinst diese Rolle nicht aktiviert zu haben.")).queue();
					return false;
				}
				try
				{
					guild.removeRoleFromMember(member, role).reason("Rollen Shop").queue(s -> {
						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COINS);
						builder.setTitle("Rollen Shop");
						builder.setDescription("Die Rolle \"" + alias + "\" wurde erfolgreich deaktiviert!");
						builder.setFooter("Nutze \"" + settings.getPrefix() + "shop activate " + alias + "\" um sie wieder zu aktivieren.");
						channel.sendMessage(builder.build()).queue();
					}, e -> {
						e.printStackTrace();
						channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					channel.sendMessage(ChocoBot.errorMessage("Es gab einen Fehler.")).queue();
				}
				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.errorMessage("Du scheinst diese Rolle nicht zu besitzen.")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		return false;
	}

	@Override
	protected @NotNull String getKeyword()
	{
		return "shop";
	}

	@Override
	protected @Nullable String getHelpText()
	{
		return "Shop, in dem du Rollen (z.B. Farbrollen) für Coins kaufen kannst.";
	}

	@Override
	protected @Nullable String getUsage()
	{
		return  "%c : Zeige alle verfügbaren Rollen an.\n" +
				"%c inventory : Zeige alle Rollen an, die du gekauft hast.\n" +
				"%c buy <alias> : Kaufe eine Rolle.\n" +
				"%c activate <alias> : Aktiviere eine gekaufte Rolle.\n" +
				"%c deactivate <alias> : Deaktiviere eine gekaufte Rolle.";
	}
}
