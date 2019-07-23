package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;

public class CommandReload extends GodsCommand {

	protected CommandReload() {
		super("reload");
		this.permission = "gods.reload";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			sender.sendMessage(stringNoPermission);
			return;
		}
		GodsConfiguration.get().loadSettings();
		GodManager.get().load();
		QuestManager.get().load();
		BelieverManager.get().load();
		WhitelistManager.get().load();
		sender.sendMessage(ChatColor.YELLOW + Gods.get().getDescription().getFullName() + ": " + ChatColor.WHITE + "Reloaded configuration.");
		Gods.get().log(sender.getName() + " /gods reload");
	}
}
