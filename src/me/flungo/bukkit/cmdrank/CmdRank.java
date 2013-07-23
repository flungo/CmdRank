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
		getCommand("cmdrank").setExecutor(ce);
		getConfig().options().copyDefaults(true);
		saveConfig();
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
	}

	protected Permission getPermissions() {
		return permission;
	}

	public boolean rankup(Player p) {
		String[] groups = permission.getPlayerGroups(p);
		boolean rankedup = false, matched = false;
		for (String group : groups) {
			String rankNode = "ranks." + group;
			if (getConfig().contains(rankNode)) {
				matched = true;
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
				//Player has been ranked up, set the flag
				rankedup = true;
			}
		}
		if (!matched) {
			p.sendMessage(ChatColor.RED + "Your current rank does not allow you to rankup.");
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
