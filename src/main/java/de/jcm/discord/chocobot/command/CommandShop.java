package de.jcm.discord.chocobot.command;

import de.jcm.discord.chocobot.ChocoBot;
import de.jcm.discord.chocobot.DatabaseUtils;
import de.jcm.discord.chocobot.GuildSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class CommandShop extends Command
{
	@Override
	public boolean execute(Message message, TextChannel channel, Guild guild, GuildSettings settings, String... args)
	{
		if(args.length == 0)
		{
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(ChocoBot.COLOR_COINS);
			builder.setTitle(settings.translate("command.shop.list.title"));
			builder.setDescription(settings.translate("command.shop.list.description"));
			builder.setFooter(settings.translate("command.shop.list.footer", settings.getPrefix()));

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

					builder.addField(settings.translate("command.shop.list.entry.key", alias, role.getName()),
					                 settings.translate("command.shop.list.entry.value", description, cost),
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
		else if(args.length == 1 && (args[0].equals("inventory") || args[0].equals("i")))
		{
			return inventory(message.getAuthor(), guild, settings, channel);
		}
		else if(args.length == 2)
		{
			String subcommand = args[0].toLowerCase(Locale.ROOT);
			switch(subcommand)
			{
				case "b":
				case "buy":
					return buy(message.getAuthor(), guild, settings, channel, args[1]);
				case "a":
				case "activate":
					return activate(message.getAuthor(), guild, settings, channel, args[1]);
				case "d":
				case "deactivate":
					return deactivate(message.getAuthor(), guild, settings, channel, args[1]);
			}
		}
		return false;
	}

	private boolean inventory(User user, Guild guild, GuildSettings settings, TextChannel channel)
	{
		try(Connection connection = ChocoBot.getDatabase();
		    PreparedStatement statement = connection.prepareStatement("SELECT r.role, r.alias, r.description, r.cost FROM shop_roles r, shop_inventory i WHERE " +
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
			builder.setTitle(settings.translate("command.shop.inventory.title"));
			builder.setDescription(settings.translate("command.shop.inventory.description"));
			builder.setFooter(settings.translate("command.shop.inventory.footer", settings.getPrefix()));

			while(resultSet.next())
			{
				long roleId = resultSet.getLong("role");
				String alias = resultSet.getString("alias");
				String description = resultSet.getString("description");
				int cost = resultSet.getInt("cost");

				Role role = guild.getRoleById(roleId);
				if(role == null)
					continue;

				builder.addField(settings.translate("command.shop.inventory.entry.key", alias, role.getName()),
				                 settings.translate("command.shop.inventory.entry.value", description, cost),
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
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.buy.dup")).queue();
					return false;
				}
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.buy.noent")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
		}

		if(DatabaseUtils.getCoins(user.getIdLong(), guild.getIdLong()) < cost)
		{
			channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.buy.not_enough")).queue();
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
				channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
				return false;
			}
		}
		catch(SQLException throwables)
		{
			throwables.printStackTrace();
			channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
			return false;
		}

		DatabaseUtils.changeCoins(user.getIdLong(), guild.getIdLong(), -cost);

		EmbedBuilder builder = new EmbedBuilder();
		builder.setColor(ChocoBot.COLOR_COINS);
		builder.setTitle("Rollenshop");
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

				Member member = guild.retrieveMember(user).complete();
				assert member != null;

				Role role = guild.getRoleById(roleId);
				if(role == null)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.activate.noent")).queue();
					return false;
				}

				if(member.getRoles().contains(role))
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.activate.dup")).queue();
					return false;
				}
				try
				{
					guild.addRoleToMember(member, role).reason(settings.translate("command.shop.activate.reason")).queue(s -> {
						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COINS);
						builder.setTitle(settings.translate("command.shop.activate.title"));
						builder.setDescription(settings.translate("command.shop.activate.description", alias));
						builder.setFooter(settings.translate("command.shop.activate.footer", settings.getPrefix(), alias));
						channel.sendMessage(builder.build()).queue();
					}, e -> {
						e.printStackTrace();
						channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
				}
				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.activate.not_owned")).queue();
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

				Member member = guild.retrieveMember(user).complete();
				assert member != null;

				Role role = guild.getRoleById(roleId);
				if(role == null)
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.deactivate.noent")).queue();
					return false;
				}

				if(!member.getRoles().contains(role))
				{
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.deactivate.dup")).queue();
					return false;
				}
				try
				{
					guild.removeRoleFromMember(member, role).reason(settings.translate("command.shop.deactivate.reason")).queue(s -> {
						EmbedBuilder builder = new EmbedBuilder();
						builder.setColor(ChocoBot.COLOR_COINS);
						builder.setTitle(settings.translate("command.shop.deactivate.title"));
						builder.setDescription(settings.translate("command.shop.deactivate.description", alias));
						builder.setFooter(settings.translate("command.shop.deactivate.footer", settings.getPrefix(), alias));
						channel.sendMessage(builder.build()).queue();
					}, e -> {
						e.printStackTrace();
						channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.internal")).queue();
				}
				return true;
			}
			else
			{
				channel.sendMessage(ChocoBot.translateError(settings, "command.shop.error.deactivate.not_owned")).queue();
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
}
