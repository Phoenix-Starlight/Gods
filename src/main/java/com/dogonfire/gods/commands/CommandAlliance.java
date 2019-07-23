package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandAlliance extends GodsCommand {

	protected CommandAlliance() {
		super("ally");
		this.permission = "gods.priest.alliance";
		this.parameters = "<god>";
		this.description = "Toggle alliance with another religion";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission for that");
			return;
		}
		if (sender instanceof Player == false) {
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		if (!GodManager.get().isPriest(player.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "Only priests can declare religous wars");
			return;
		}
		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		String allyGodName = GodManager.get().formatGodName(args[1]);
		if (!GodManager.get().godExist(args[1])) {
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		List<?> wars = GodManager.get().getWarRelations(godName);
		if (wars.contains(allyGodName)) {
			player.sendMessage(ChatColor.RED + "You are in WAR with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
			return;
		}
		if (GodManager.get().toggleAllianceRelationForGod(godName, allyGodName)) {
			LanguageManager.get().setPlayerName(godName);
			GodManager.get().godSayToBelievers(allyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAlliance, 10);
			LanguageManager.get().setPlayerName(allyGodName);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAlliance, 10);
		} else {
			LanguageManager.get().setPlayerName(godName);
			GodManager.get().godSayToBelievers(allyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversAllianceCancelled, 10);

			LanguageManager.get().setPlayerName(allyGodName);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversAllianceCancelled, 10);
		}
	}
}
