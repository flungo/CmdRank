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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
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

    private ConfigAccessor playersCA = null;
    private ConfigAccessor messagesCA = null;
    public static Metrics metrics = null;
    public static Permission permission = null;
    public static Economy economy = null;
    private static HashMap<String, String> colorSubs = new HashMap<>();

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "{0} is now disabled", getDescription().getName());
    }

    @Override
    public void onEnable() {
        setupMetrics();
        setupPermissions();
        setupEconomy();
        CommandExecutor ce = new Commands(this);
        getCommand("rankup").setExecutor(ce);
        for (String alias : getConfig().getStringList("aliases.rankup")) {
            getCommand(alias).setExecutor(ce);
        }
        getCommand("rankcheck").setExecutor(ce);
        for (String alias : getConfig().getStringList("aliases.rankcheck")) {
            getCommand(alias).setExecutor(ce);
        }
        getCommand("cmdrank").setExecutor(ce);
        setupConfig();
        getLogger().log(Level.INFO, "{0} version {1} is enabled.", new Object[]{getDescription().getName(), getDescription().getVersion()});
        setupColourSubs();
    }

    private void setupMetrics() {
        try {
            metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
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

    private void setupColourSubs() {
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

    public void reload() {
        reloadConfig();
        setupConfig();
    }

    public void setupConfig() {
        getConfig().options().header(getDescription().getName() + " Config File");
        // If there aren't any ranks defined, consturct a default rankup for refference
        if (!getConfig().contains("ranks")) {
            ConfigurationSection defaultRank = getConfig().createSection("ranks.default");
            defaultRank.set("description", "Rank you up from default to member");
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
            defaultRank.set("global-cooldown", 0);
            defaultRank.set("reranks", 0);
        }
        getConfig().options().copyDefaults(true);
        saveConfig();
        playersCA = new ConfigAccessor(this, "player.yml");
        messagesCA = new ConfigAccessor(this, "messages.yml");
        messagesCA.getConfig().options().copyHeader(true);
        messagesCA.saveConfig();
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

    public FileConfiguration getMessageConfig() {
        if (messagesCA == null) {
            throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
        }
        return messagesCA.getConfig();
    }

    public void reloadMessageConfig() {
        if (messagesCA == null) {
            throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
        }
        messagesCA.reloadConfig();
    }

    public void saveMessageConfig() {
        if (messagesCA == null) {
            throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
        }
        messagesCA.saveConfig();
    }

    public void saveMessagePlayerConfig() {
        if (messagesCA == null) {
            throw new IllegalStateException("Plugin has not initialised the Player Config Accessor");
        }
        messagesCA.saveDefaultConfig();
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
        List<String> messages = new ArrayList<>(matches.size() * 2 + 2);
        int line = 0;
        Map<String, String> globalSubs = new HashMap<>(1);
        globalSubs.put("player", p.getName());
        globalSubs.put("globalcoodown", formatTime(getCooldown(p)));
        for (String header_line : getMessageConfig().getStringList("rankcheck.header")) {
            messages.add(formatString(header_line, globalSubs));
        }
        if (!getConfig().getBoolean("hide-messages.rankcheck.global-cooldown") && getCooldown(p) > 0) {
            messages.add(formatString(getMessageConfig().getString("rankcheck.global-cooldown"), globalSubs));
        }
        boolean matchshow = false;
        for (String group : matches) {
            //Check if enabled
            if (getConfig().getBoolean("hide-messages.rankcheck.matches.disabled")
                    && isRankDisabled(p, group)) {
                continue;
            }
            //Check if the is reranking and if they are allowed to
            if (getConfig().getBoolean("hide-messages.rankcheck.matches.rerank")
                    && !checkRerank(p, group)) {
                continue;
            }
            //Check if the player is subject to cooldown time
            if (getConfig().getBoolean("hide-messages.rankcheck.matches.cooldown")
                    && checkCooldown(p, group)) {
                continue;
            }
            //Check if requirements are met
            if (getConfig().getBoolean("hide-messages.rankcheck.matches.requirements")
                    && !checkRequirements(p, group)) {
                continue;
            }
            Map<String, String> rankSubs = getRankSubs(p, group);
            rankSubs.putAll(globalSubs);
            messages.add(null);
            messages.add(formatString(getMessageConfig().getString("rankcheck.rank.header"), rankSubs));
            if (!getConfig().getBoolean("hide-messages.rankcheck.description")
                    && getConfig().contains("ranks." + group + ".description")) {
                messages.add(formatString(getMessageConfig().getString("rankcheck.rank.description"), rankSubs));
            }
            if (!getConfig().getBoolean("hide-messages.rankcheck.requirements")) {
                messages.add(formatString(getMessageConfig().getString("rankcheck.rank.requirements"), rankSubs));
            }
            if (!getConfig().getBoolean("hide-messages.rankcheck.cooldown")
                    && getCooldown(p, group) > 0) {
                messages.add(formatString(getMessageConfig().getString("rankcheck.rank.cooldown"), rankSubs));
            }
            if (!getConfig().getBoolean("hide-messages.rankcheck.reranks")
                    && getConfig().getInt("ranks." + group + ".reranks", getDefaultReranks()) > 1) {
                messages.add(formatString(getMessageConfig().getString("rankcheck.rank.reranks"), rankSubs));
            }
            if (!getConfig().getBoolean("hide-messages.rankcheck.disabled")
                    && isRankDisabled(group)) {
                messages.add(formatString(getMessageConfig().getString("rankcheck.rank.disabled"), rankSubs));
            }
            for (String additional_line : getMessageConfig().getStringList("rankcheck.rank.additional")) {
                messages.add(formatString(additional_line, rankSubs));
            }
            matchshow = true;
        }
        if (!matchshow) {
            messages.add(null);
            messages.add(formatString(getMessageConfig().getString("rankcheck.norankups"), globalSubs));
        }
        messages.add(null);
        for (String footer_line : getMessageConfig().getStringList("rankcheck.footer")) {
            messages.add(formatString(footer_line, globalSubs));
        }
        p.sendMessage(messages.toArray(new String[messages.size()]));
    }

    public boolean rankup(Player p) {
        boolean rankedup = false;
        List<String> matches = getMatches(p);
        if (matches.isEmpty()) {
            if (!getConfig().getBoolean("hide-messages.rankup.not-available")) {
                p.sendMessage(formatString(getMessageConfig().getString("rankup.not-available"), null));
            }
            return false;
        }
        for (String group : matches) {
            Map<String, String> rankSubs = getRankSubs(p, group);
            String rankNode = "ranks." + group;
            //Check if enabled
            if (isRankDisabled(p, group)) {
                if (!getConfig().getBoolean("hide-messages.rankup.disabled")) {
                    p.sendMessage(formatString(getMessageConfig().getString("rankup.rank.disabled"), rankSubs));
                }
                continue;
            }
            //Check if the player is reranking and if they are allowed to
            if (!checkRerank(p, group)) {
                if (!getConfig().getBoolean("hide-messages.rankup.rerank")) {
                    p.sendMessage(formatString(getMessageConfig().getString("rankup.rank.rerank"), rankSubs));
                }
                continue;
            }
            //Check if the player is subject to cooldown time
            if (checkCooldown(p, group)) {
                if (!getConfig().getBoolean("hide-messages.rankup.cooldown")) {
                    p.sendMessage(formatString(getMessageConfig().getString("rankup.rank.cooldown"), rankSubs));
                }
                continue;
            }
            //Check if requirements are met
            if (!checkRequirements(p, group)) {
                if (!getConfig().getBoolean("hide-messages.rankup.requirements")) {
                    p.sendMessage(formatString(getMessageConfig().getString("rankup.rank.requirements"), rankSubs));
                }
                continue;
            }
            //Use requirements where appropriate
            useRequirments(p, group);
            //Execute commands
            List<String> commands = getConfig().getStringList(rankNode + ".commands");
            for (String command : commands) {
                getServer().dispatchCommand(Bukkit.getConsoleSender(), formatString(command, rankSubs));
            }
            //Announce
            if (!getConfig().getBoolean("hide-messages.rankup.announcement")) {
                String announcement = getConfig().getString(rankNode + ".announcement",
                        getMessageConfig().getString("rankup.rank.announcement"));
                getServer().broadcastMessage(formatString(announcement, rankSubs));
            }
            //Set last rankup for player in config
            long lastrankup = System.currentTimeMillis();
            getPlayerConfig().set(p.getName() + ".lastrankup", lastrankup);
            //Add this rankup to the player's rankup history
            String playerRankNode = p.getName() + ".ranks." + group;
            getPlayerConfig().set(playerRankNode + ".lastrankup", lastrankup);
            //Increment number of rankups for rank
            String playerRankRankupsNode = playerRankNode + ".rankups";
            int playerRankRankups = getPlayerConfig().getInt(playerRankRankupsNode, 0);
            getPlayerConfig().set(playerRankRankupsNode, ++playerRankRankups);
            //Save changes to players.yml
            savePlayerConfig();
            //Player has been ranked up, set the flag
            rankedup = true;
        }
        if (rankedup) {
            if (!getConfig().getBoolean("hide-messages.rankup.success")) {
                p.sendMessage(formatString(getMessageConfig().getString("rankup.success"), null));
            }
        } else {
            if (!getConfig().getBoolean("hide-messages.rankup.failure")) {
                p.sendMessage(formatString(getMessageConfig().getString("rankup.failure"), null));
            }
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
        String requirmentsString = "";
        // Get terms
        Map<String, String> terms = new HashMap<>();
        terms.put("money", getMessageConfig().getString("requirements.terms.money"));
        terms.put("exp", getMessageConfig().getString("requirements.terms.exp"));
        terms.put("health", getMessageConfig().getString("requirements.terms.health"));
        terms.put("hunger", getMessageConfig().getString("requirements.terms.hunger"));
        terms.put("and", getMessageConfig().getString("requirements.terms.and"));
        terms.put("none", getMessageConfig().getString("requirements.terms.none"));
        if (getMessageConfig().getBoolean("requirements.overide")) {
            requirmentsString = getMessageConfig().getString("requirements.string");
        } else {
            if (economy != null && getConfig().getDouble(reqNode + ".money", 0) > 0) {
                requirements.add(economy.format(getConfig().getDouble(reqNode + ".money")) + " {money}");
            }
            if (getConfig().getInt(reqNode + ".exp", 0) > 0) {
                requirements.add(getConfig().getInt(reqNode + ".exp") + " {exp}");
            }
            if (getConfig().getInt(reqNode + ".health", 0) > 0) {
                int health = getConfig().getInt(reqNode + ".health");
                float hearts = health / 2;
                requirements.add(health + " hit points (" + hearts + " {health})");
            }
            if (getConfig().getInt(reqNode + ".hunger", 0) > 0) {
                int hunger = getConfig().getInt(reqNode + ".hunger");
                requirements.add(hunger + " {hunger}");
            }
            if (requirements.isEmpty()) {
                return "{none}";
            }
            for (int i = 0; i < requirements.size(); i++) {
                requirmentsString += requirements.get(i);
                if (i != (requirements.size() - 1)) {
                    if (i == (requirements.size() - 2)) {
                        requirmentsString += " {and} ";
                    } else {
                        requirmentsString += ", ";
                    }
                }
            }
        }
        return formatString(requirmentsString, terms);
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

    private long getCooldown(Player p) {
        if (permission.has(p, "cmdrank.bypass.cooldown")
                || getConfig().getLong("cooldown", 0L) <= 0) {
            return 0;
        }
        long cooldown = getPlayerConfig().getLong(p.getName() + ".lastrankup", 0L)
                - System.currentTimeMillis() + getConfig().getLong("cooldown", 0L) * 1000L;
        if (cooldown < 0) {
            return 0;
        } else {
            return cooldown;
        }
    }

    private long getCooldown(Player p, String group) {
        if (permission.has(p, "cmdrank.bypass.cooldown")
                || (getConfig().getLong("ranks." + group + ".cooldown", 0L) <= 0
                && getConfig().getLong("ranks." + group + ".global-cooldown", 0L) <= 0)) {
            return 0;
        }
        long localCooldown = getPlayerConfig().getLong(p.getName() + ".ranks." + group + ".lastrankup", 0L)
                - System.currentTimeMillis() + getConfig().getLong("ranks." + group + ".cooldown", 0L) * 1000L;
        long globalCooldown = getPlayerConfig().getLong(p.getName() + ".lastrankup", 0L)
                - System.currentTimeMillis() + getConfig().getLong("ranks." + group + ".global-cooldown", 0L) * 1000L;
        long cooldown;
        if (localCooldown > globalCooldown) {
            cooldown = localCooldown;
        } else {
            cooldown = globalCooldown;
        }
        if (cooldown < 0) {
            return 0;
        } else {
            return cooldown;
        }
    }

    private boolean checkCooldown(Player p, String group) {
        if (permission.has(p, "cmdrank.bypass.cooldown")) {
            return false;
        }
        if (getCooldown(p) > 0 || getCooldown(p, group) > 0) {
            return true;
        }
        return false;
    }

    private boolean isRankDisabled(String group) {
        return getConfig().getInt("ranks." + group + ".reranks", getDefaultReranks()) == 0;
    }

    private boolean isRankDisabled(Player p, String group) {
        if (isRankDisabled(group)) {
            if (permission.has(p, "cmdrank.bypass.disabled")) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private int getDefaultReranks() {
        if (getConfig().getBoolean("rerank", true)) {
            return -1;
        } else {
            return 1;
        }
    }

    private Integer getRemainingReranks(Player p, String group) {
        if (getConfig().getInt("ranks." + group + ".reranks", getDefaultReranks()) >= 0) {
            if (!getConfig().getBoolean("rerank", true)
                    || getPlayerConfig().getInt(p.getName() + ".ranks." + group + ".rankups", 0) > 0) {
                int remaining = getConfig().getInt("ranks." + group + ".reranks", getDefaultReranks())
                        - getPlayerConfig().getInt(p.getName() + ".ranks." + group + ".rankups", 0);
                if (remaining < 0) {
                    return 0;
                } else {
                    return remaining;
                }
            }
        }
        return -1;
    }

    private boolean checkRerank(Player p, String group) {
        if (isRankDisabled(group)) {
            return isRankDisabled(p, group);
        }
        if (permission.has(p, "cmdrank.bypass.reranks")) {
            return true;
        }
        if (getRemainingReranks(p, group) == 0) {
            return false;
        }
        return true;
    }

    public Map<String, String> getRankSubs(Player p, String group) {
        Map<String, String> rankSubs = new HashMap<>();
        rankSubs.put("group", group);
        rankSubs.put("rank", group);
        rankSubs.put("player", p.getName());
        rankSubs.put("user", p.getName());
        rankSubs.put("description", getConfig().getString("ranks." + group + ".description"));
        rankSubs.put("requirements", getRequirements(group));
        rankSubs.put("cooldown", formatTime(getCooldown(p, group)));
        rankSubs.put("rankups", getRemainingReranks(p, group).toString());
        return rankSubs;
    }

    public String formatTime(long time) {
        Map<String, String> times = new HashMap<>();
        Long s = new Long(TimeUnit.MILLISECONDS.toSeconds(time));
        Long m = new Long(TimeUnit.MILLISECONDS.toMinutes(time));
        Long h = new Long(TimeUnit.MILLISECONDS.toHours(time));
        Long d = new Long(TimeUnit.MILLISECONDS.toDays(time));
        times.put("s", s.toString());
        times.put("m", m.toString());
        times.put("h", h.toString());
        times.put("d", d.toString());
        if (d > 0) {
            return formatString(getMessageConfig().getString("time.days"), times);
        }
        if (h > 0) {
            return formatString(getMessageConfig().getString("time.hours"), times);
        }
        if (m > 0) {
            return formatString(getMessageConfig().getString("time.minutes"), times);
        }
        return formatString(getMessageConfig().getString("time.seconds"), times);
    }

    public static String formatString(String message, Map<String, String> subs) {
        if (subs != null) {
            for (Entry<String, String> sub : subs.entrySet()) {
                message = message.replace("{" + sub.getKey() + "}", sub.getValue());
            }
        }
        for (Entry<String, String> sub : colorSubs.entrySet()) {
            message = message.replace(sub.getKey(), sub.getValue());
        }
        return message;
    }
}
