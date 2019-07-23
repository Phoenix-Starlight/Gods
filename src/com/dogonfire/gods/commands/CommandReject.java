package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.GodManager;

public class CommandReject extends GodsCommand {

	protected CommandReject() {
		super("reject");
		this.permission = "gods.reject";
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
		GodManager.instance().believerReject(player.getUniqueId());
	}

}
