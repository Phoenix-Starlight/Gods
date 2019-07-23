package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.QuestManager;

public class CommandHunt extends GodsCommand {

	protected CommandHunt() {
		super("hunt");
		this.permission = "gods.hunt";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false) {
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		if (godName == null) {
			sender.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}
		Location pilgrimageLocation = QuestManager.get().getQuestLocation(godName);
		if (player == null || player.isFlying()) {
			sender.sendMessage(ChatColor.RED + "No flying allowed.");
			return;
		}
		if (pilgrimageLocation == null) {
			sender.sendMessage(ChatColor.RED + "There is no quest target to hunt for, Mr. fancy pants!");
			return;
		}
		if (!pilgrimageLocation.getWorld().getName().equals(player.getWorld().getName())) {
			Gods.get().logDebug("PilgrimageQuest for '" + player.getDisplayName() + "' is wrong world");
			return;
		}
		Vector vector = pilgrimageLocation.toVector().subtract(player.getLocation().toVector());
		LanguageManager.get().setAmount((int) vector.length());
		Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, (int) vector.length(), "", 20);

	}

}
