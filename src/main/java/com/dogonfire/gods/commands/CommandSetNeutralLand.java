package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.HolyLandManager;

public class CommandSetNeutralLand extends GodsCommand {

	public CommandSetNeutralLand() {
		super("setsafe");
		this.permission = "gods.setsafe";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!GodsConfiguration.get().isHolyLandEnabled()) {
			sender.sendMessage(ChatColor.RED + "Holy Land is not enabled on this server");
			return;
		}
		if (!hasPermission(sender)) {
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false) {
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		if (HolyLandManager.get().isNeutralLandLocation(player.getLocation())) {
			HolyLandManager.get().clearNeutralLand(player.getLocation());
			sender.sendMessage(ChatColor.AQUA + "You set cleared the neutral land in this location.");
		} else {
			HolyLandManager.get().setNeutralLand(player.getLocation());
			sender.sendMessage(ChatColor.AQUA + "You set neutral land in this location.");
		}
	}
}
