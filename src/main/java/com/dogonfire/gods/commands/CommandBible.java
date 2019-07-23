package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;

public class CommandBible extends GodsCommand {

	protected CommandBible() {
		super("bible");
		this.permission = "gods.bible";
		this.description = "Produces the Holy Book for your religion";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!GodsConfiguration.get().isBiblesEnabled()) {
			sender.sendMessage(ChatColor.RED + "Bibles are not enabled on this server");
			return;
		}
		if (!hasPermission(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
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

		if (!GodManager.get().isPriest(player.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "Only your priest can produce the Holy Book");
			return;
		}
		if (!HolyBookManager.get().giveBible(godName, player.getName())) {
			sender.sendMessage(ChatColor.RED + "Could not produce a Holy Book for " + godName);
			return;
		}
		sender.sendMessage(ChatColor.AQUA + "You produced a copy of " + ChatColor.GOLD + HolyBookManager.get().getBibleTitle(godName) + ChatColor.AQUA + "!");
	}
}
