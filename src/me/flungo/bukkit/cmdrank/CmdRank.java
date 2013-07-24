/*
 * Copyright (C) 2013 Fabrizio Lungo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.flungo.bukkit.cmdrank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Fabrizio Lungo <fab@lungo.co.uk>
 */
public class CmdRank extends JavaPlugin {

	private PluginDescriptionFile pdf;
	private PluginManager pm;
	private ConfigAccessor playersCA = null;
	public static Permission permission = null;
	public static Economy economy = null;
	private static HashMap<String, String> colorSubs = new HashMap<>();

	@Override
	public void onDisable() {
		getLogger().log(Level.INFO, "{0} is now disabled", pdf.getName());
	}

	@Override
	public void onEnable() {
		pdf = this.getDescription();
		setupPermissions();
		setupEconomy();
		pm = getServer().getPluginManager();
		CommandExecutor ce = new Commands(this);
		getCommand("rankup").setExecutor(ce);
		getCommand("rankcheck").setExecutor(ce);
		getCommand("cmdrank").setExecutor(ce);
		setupConfig();
		getLogger().log(Level.INFO, "{0} version {1} is enabled.", new Object[]{pdf.getName(), pdf.getVersion()});
		colorSubs.put("&0", ChatColor.BLACK.toString());
		colorSubs.put("&1", ChatColor.DARK_BLUE.toString());
		colorSubs.put("&2", ChatColor.DARK_GREEN.toString());
		colorSubs.put("&3", ChatColor.DARK_AQUA.toString());
		colorSubs.put("&4", ChatColor.DARK_RED.toString());
		colorSubs.put("&5", ChatColor.DARK_PURPLE.toString());
		colorSubs.put("&6", ChatColor.GOLD.toString());
		colorSubs.put("&7", ChatColor.GRAY.toString());
		colorSubs.put("&8", ChatColor.DARK_GRAY.toString());
		colorSubs.put("&9", ChatColor.BLUE.toString());
		colorSubs.put("&a", ChatColor.GREEN.toString());
		colorSubs.put("&b", ChatColor.AQUA.toString());
		colorSubs.put("&c", ChatColor.RED.toString());
		colorSubs.put("&d", ChatColor.LIGHT_PURPLE.toString());
		colorSubs.put("&e", ChatColor.YELLOW.toString());
		colorSubs.put("&f", ChatColor.WHITE.toString());
		colorSubs.put("&k", ChatColor.MAGIC.toString());
		colorSubs.put("&l", ChatColor.BOLD.toString());
		colorSubs.put("&m", ChatColor.STRIKETHROUGH.toString());
		colorSubs.put("&n", ChatColor.UNDERLINE.toString());
		colorSubs.put("&o", ChatColor.ITALIC.toString());
		colorSubs.put("&r", ChatColor.RESET.toString());
	}

	private boolean setupPermissions() {
		getLogger().log(Level.INFO, "Attempting to configure Vault permissions");
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	private boolean setupEconomy() {
		getLogger().log(Level.INFO, "Attempting to configure Vault economy");
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	public void reload() {
		reloadConfig();
		setupConfig();
	}

	public void setupConfig() {
		getConfig().options().copyDefaults(true);
		getConfig().options().header(pdf.getName() + " Config File");
		if (!getConfig().contains("ranks")) {
			ConfigurationSection defaultRank = getConfig().createSection("ranks.default");
			defaultRank.set("requirements.money", 500);
			defaultRank.set("requirements.exp", 0);
			defaultRank.set("requirements.health", 0);
			defaultRank.set("requirements.hunger", 0);
			List<String> commands = new ArrayList<>(2);
			commands.add("pex user {player} group add member");
			commands.add("pex user {player} group remove {rank}");
			defaultRank.set("commands", commands);
			defaultRank.set("announcement", "{player} has ranked up from default to member");
			defaultRank.set("cooldown", 0);
			defaultRank.set("reranks", 0);
		}
		saveConfig();
		playersCA = new ConfigAccessor(this, "player.yml");
	}

	public FileConfiguration getPlayerConfig() {
		if (playersCA == null) {
			throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
		}
		return playersCA.getConfig();
	}

	public void reloadPlayerConfig() {
		if (playersCA == null) {
			throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
		}
		playersCA.reloadConfig();
	}

	public void savePlayerConfig() {
		if (playersCA == null) {
			throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
		}
		playersCA.saveConfig();
	}

	public void saveDefaultPlayerConfig() {
		if (playersCA == null) {
			throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
		}
		playersCA.saveDefaultConfig();
	}

	protected Permission getPermissions() {
		return permission;
	}

	public List<String> getMatches(Player p) {
		String[] groups = permission.getPlayerGroups(p);
		List<String> matches = new ArrayList<>(4);
		for (String group : groups) {
			if (getConfig().contains("ranks." + group)) {
				matches.add(group);
			}
		}
		return matches;
	}

	public void showMatches(Player p) {
		List<String> matches = getMatches(p);
		if (matches.isEmpty()) {
			p.sendMessage(ChatColor.RED + "Your current rank does not allow you to rankup.");
		} else {
			String[] messages = new String[matches.size() * 2];
			int line = 0;
			for (String group : matches) {
				messages[line++] = ChatColor.RED + "Rankup from " + group + ":";
				messages[line++] = ChatColor.DARK_GREEN + "Requirements: " + ChatColor.GOLD + getRequirements(group);
			}
			p.sendMessage(messages);
		}
	}

	public boolean rankup(Player p) {
		boolean rankedup = false;
		List<String> matches = getMatches(p);
		if (matches.isEmpty()) {
			p.sendMessage(ChatColor.RED + "Your current rank does not allow you to rankup.");
			return false;
		}
		for (String group : matches) {
			String rankNode = "ranks." + group;
			//Check if enabled
			if (getConfig().getInt("ranks." + group + ".reranks", 1) < 1 && !permission.has(p, "cmdrank.bypass.disabled")) {
				p.sendMessage(ChatColor.RED + "Rankup is currently disabled from " + ChatColor.DARK_PURPLE + group);
				continue;
			}
			//Check if the is reranking and if they are allowed to
			if (!checkRerank(p, group)) {
				p.sendMessage(ChatColor.RED + "You cannot rankup from " + ChatColor.DARK_PURPLE + group + ChatColor.RED + " again");
				continue;
			}
			//Check if the player is subject to cooldown time
			if (checkCooldown(p, group)) {
				p.sendMessage(ChatColor.RED + "You must wait before you can rankup from " + ChatColor.DARK_PURPLE + group);
				continue;
			}
			//Check if requirements are met
			if (!checkRequirements(p, group)) {
				p.sendMessage(ChatColor.RED + "You must meet the requirments to rankup: " + ChatColor.GOLD + getRequirements(group));
				continue;
			}
			//Use requirements where appropriate
			useRequirments(p, group);
			//Setup substitutions
			HashMap<String, String> subs = new HashMap<>();
			subs.put("player", p.getName());
			subs.put("rank", group);
			//Execute commands
			List<String> commands = getConfig().getStringList(rankNode + ".commands");
			for (String command : commands) {
				getServer().dispatchCommand(Bukkit.getConsoleSender(), formatString(command, subs));
			}
			//Announce
			String announcement = getConfig().getString(rankNode + ".announcement", "{user} has ranked up");
			getServer().broadcastMessage(ChatColor.AQUA + formatString(announcement, subs));
			//Set last rankup for player in config
			long lastrankup = System.currentTimeMillis();
			getPlayerConfig().set(p.getName() + ".lastrankup", lastrankup);
			//Add this rankup to the player's rankup history
			String playerRankNode = p.getName() + ".ranks." + group;
			getPlayerConfig().set(playerRankNode + ".lastrankup", lastrankup);
			String playerRankRankupsNode = playerRankNode + ".rankups";
			int playerRankRankups = getPlayerConfig().getInt(playerRankRankupsNode, 0);
			getPlayerConfig().set(playerRankRankupsNode, ++playerRankRankups);
			//Save changes to players.yml
			savePlayerConfig();
			//Player has been ranked up, set the flag
			rankedup = true;
		}
		return rankedup;
	}

	public boolean checkRequirements(Player p, String group) {
		String reqNode = "ranks." + group + ".requirements";
		if (economy != null && getConfig().getDouble(reqNode + ".money", 0) != 0) {
			if (!economy.has(p.getName(), getConfig().getDouble(reqNode + ".money"))) {
				return false;
			}
		}
		if (getConfig().getInt(reqNode + ".exp", 0) != 0) {
			if (p.getExp() < getConfig().getInt(reqNode + ".exp")) {
				return false;
			}
		}
		if (getConfig().getInt(reqNode + ".health", 0) != 0) {
			if (p.getHealth() < getConfig().getInt(reqNode + ".health")) {
				return false;
			}
		}
		if (getConfig().getInt(reqNode + ".hunger", 0) != 0) {
			if (p.getFoodLevel() < getConfig().getInt(reqNode + ".hunger")) {
				return false;
			}
		}
		return true;
	}

	public String getRequirements(String group) {
		List<String> requirements = new ArrayList<>(4); //Set array list size to number of requirements in program
		String reqNode = "ranks." + group + ".requirements";
		if (economy != null && getConfig().getDouble(reqNode + ".money", 0) > 0) {
			requirements.add(economy.format(getConfig().getDouble(reqNode + ".money")) + " money");
		}
		if (getConfig().getInt(reqNode + ".exp", 0) > 0) {
			requirements.add(getConfig().getInt(reqNode + ".exp") + " xp points");
		}
		if (getConfig().getInt(reqNode + ".health", 0) > 0) {
			int health = getConfig().getInt(reqNode + ".health");
			float hearts = health / 2;
			requirements.add(health + " hit points (" + hearts + " hearts)");
		}
		if (getConfig().getInt(reqNode + ".hunger", 0) > 0) {
			int hunger = getConfig().getInt(reqNode + ".hunger");
			requirements.add(hunger + " hunger");
		}
		if (requirements.isEmpty()) {
			return "No requirements!";
		}
		String requirmentsString = "";
		for (int i = 0; i < requirements.size(); i++) {
			requirmentsString += requirements.get(i);
			if (i != (requirements.size() - 1)) {
				if (i == (requirements.size() - 2)) {
					requirmentsString += " and ";
				} else {
					requirmentsString += ", ";
				}
			}
		}
		return requirmentsString;
	}

	public void useRequirments(Player p, String group) {
		if (!checkRequirements(p, group)) {
			throw new IllegalStateException("Player must meet requirements first");
		}
		String reqNode = "ranks." + group + ".requirements";
		if (economy != null && getConfig().getBoolean("use-requirements.money", true) && getConfig().getDouble(reqNode + ".money", 0) > 0) {
			economy.withdrawPlayer(p.getName(), getConfig().getDouble(reqNode + ".money"));
		}
		if (getConfig().getBoolean("use-requirements.exp", true) && getConfig().getInt(reqNode + ".exp", 0) > 0) {
			p.setExp(p.getExp() - getConfig().getInt(reqNode + ".exp"));
		}
		if (getConfig().getBoolean("use-requirements.health", false) && getConfig().getInt(reqNode + ".health", 0) > 0) {
			p.setHealth(p.getHealth() - getConfig().getInt(reqNode + ".health"));
		}
		if (getConfig().getBoolean("use-requirements.hunger", false) && getConfig().getInt(reqNode + ".hunger", 0) > 0) {
			p.setFoodLevel(p.getFoodLevel() - getConfig().getInt(reqNode + ".hunger"));
		}
	}

	private boolean checkCooldown(Player p, String group) {
		if (permission.has(p, "cmdrank.bypass.cooldown")) {
			return false;
		}
		long cooldown = getConfig().getLong("cooldown", 0L);
		if (cooldown > 0
				&& getPlayerConfig().getLong(p.getName() + ".lastrankup", 0L)
				> (System.currentTimeMillis() - cooldown * 1000L)) {
			return true;
		}
		long rankCooldown = getConfig().getLong("ranks." + group + ".cooldown", 0L);
		if (rankCooldown > 0
				&& getPlayerConfig().getLong(p.getName() + ".ranks." + group + ".lastrankup", 0L)
				> (System.currentTimeMillis() - rankCooldown * 1000L)) {
			return true;
		}
		return false;
	}

	private boolean checkRerank(Player p, String group) {
		if (getConfig().getInt("ranks." + group + ".reranks", 1) < 1) {
			if (permission.has(p, "cmdrank.bypass.disabled")) {
				return true;
			} else {
				return false;
			}
		}
		if (permission.has(p, "cmdrank.bypass.reranks")) {
			return true;
		}
		if (!getConfig().getBoolean("rerank", true)
				&& getPlayerConfig().getInt(p.getName() + ".ranks." + group + ".rankups", 0) >= 1) {
			return false;
		}
		if (getPlayerConfig().getInt(p.getName() + ".ranks." + group + ".rankups", 0)
				>= getConfig().getInt("ranks." + group + ".reranks", 1)) {
			return false;
		}
		return true;
	}

	public static String formatString(String message, Map<String, String> subs) {
		for (Entry<String, String> sub : subs.entrySet()) {
			message = message.replace("{" + sub.getKey() + "}", sub.getValue());
		}
		for (Entry<String, String> sub : colorSubs.entrySet()) {
			message = message.replace(sub.getKey(), sub.getValue());
		}
		return message;
	}
}
