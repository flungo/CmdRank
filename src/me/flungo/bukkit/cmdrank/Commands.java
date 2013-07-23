/*
 * Copyright (C) 2013 Fabrizio Lungo <fab@lungo.co.uk>
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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Fabrizio Lungo <fab@lungo.co.uk>
 */
class Commands implements CommandExecutor {

	private CmdRank plugin;

	public Commands(CmdRank plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
		switch (cmnd.getName()) {
			case "rankup":
				if (cs instanceof Player) {
					Player p = (Player) cs;
					if (!CmdRank.permission.has(p, "cmdrank.rankup")) {
						p.sendMessage(ChatColor.RED + "You do not have permission to rankup");
					}
					plugin.rankup(p);
				} else {
					cs.sendMessage(ChatColor.RED + "This can only be executed in game.");
				}
				return true;
			case "rankcheck":
				if (cs instanceof Player) {
					Player p = (Player) cs;
					if (!CmdRank.permission.has(p, "cmdrank.rankup")) {
						p.sendMessage(ChatColor.RED + "You do not have permission to rankup");
					}
					plugin.showMatches(p);
				} else {
					cs.sendMessage(ChatColor.RED + "This can only be executed in game.");
				}
				return true;
			case "cmdrank":
				if (cs instanceof Player && !CmdRank.permission.has((Player) cs, "cmdrank.admin")) {
					cs.sendMessage(ChatColor.RED + "You do not have permission to do that");
					return true;
				}
				if (strings.length < 1) {
					return false;
				}
				switch (strings[0]) {
					case "reload":
						plugin.reload();
						cs.sendMessage(ChatColor.GREEN + "CmdRank Reloaded.");
						return true;
				}
		}
		return false;
	}
}
