package com.dogonfire.gods.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.GodManager.GodMood;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandInfo extends GodsCommand {

	protected CommandInfo() {
		super("info");
		this.permission = "gods.info";
		this.description = "Show info about your/a God";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args) {
		if (!hasPermission(sender)) {
			sender.sendMessage(stringNoPermission);
			return;
		}
		String godName = null;

		if (args.length == 2) {
			godName = GodManager.get().formatGodName(args[1]);
		}

		if (godName == null) {
			if (sender instanceof Player)
				godName = BelieverManager.get().getGodForBeliever(((Player) sender).getUniqueId());
			if (godName == null) {
				sender.sendMessage(ChatColor.RED + "You do not believe in any God.");
				return;
			}
		}

		if (!GodManager.get().godExist(godName)) {
			sender.sendMessage(ChatColor.RED + "There is no God with such name.");
			return;
		}

		List<UUID> priests = GodManager.get().getPriestsForGod(godName);

		if (priests == null) {
			priests = new ArrayList<UUID>();
		}

		sender.sendMessage(ChatColor.YELLOW + "--------- " + godName + " " + GodManager.get().getColorForGod(godName) + GodManager.get().getTitleForGod(godName) + ChatColor.YELLOW + " ---------");

		sender.sendMessage("" + ChatColor.DARK_PURPLE + ChatColor.ITALIC + GodManager.get().getGodDescription(godName));

		ChatColor moodColor = ChatColor.AQUA;
		GodMood godMood = GodManager.get().getMoodForGod(godName);

		switch (godMood) {
			case EXALTED:
				moodColor = ChatColor.GOLD;
				break;
			case PLEASED:
				moodColor = ChatColor.DARK_GREEN;
				break;
			case NEUTRAL:
				moodColor = ChatColor.WHITE;
				break;
			case DISPLEASED:
				moodColor = ChatColor.GRAY;
				break;
			case ANGRY:
				moodColor = ChatColor.DARK_RED;
				break;
		}

		sender.sendMessage(moodColor + godName + " is " + LanguageManager.get().getGodMoodName(godMood));

		Material neededItem = GodManager.get().getSacrificeItemTypeForGod(godName);
		if (neededItem != null) {
			sender.sendMessage(ChatColor.GOLD + godName + ChatColor.AQUA + " wants more " + ChatColor.WHITE + LanguageManager.get().getItemTypeName(neededItem));
		}

		if (priests.size() == 0) {
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + "None");
		} else if (priests.size() == 1) {
			sender.sendMessage(ChatColor.AQUA + "Priest: " + ChatColor.YELLOW + Gods.get().getServer().getOfflinePlayer(priests.get(0)).getName());
		} else {
			sender.sendMessage(ChatColor.AQUA + "Priests: ");
			for (UUID priest : priests) {
				sender.sendMessage(ChatColor.YELLOW + " - " + Gods.get().getServer().getOfflinePlayer(priest).getName());
			}
		}

		sender.sendMessage(ChatColor.AQUA + "Believers: " + ChatColor.YELLOW + BelieverManager.get().getBelieversForGod(godName).size());
		sender.sendMessage(ChatColor.AQUA + "Exact power: " + ChatColor.YELLOW + GodManager.get().getGodPower(godName));
		if (GodsConfiguration.get().isCommandmentsEnabled()) {
			sender.sendMessage(ChatColor.AQUA + "Holy food: " + ChatColor.YELLOW + LanguageManager.get().getItemTypeName(GodManager.get().getHolyFoodTypeForGod(godName)));
			sender.sendMessage(ChatColor.AQUA + "Unholy food: " + ChatColor.YELLOW + LanguageManager.get().getItemTypeName(GodManager.get().getUnholyFoodTypeForGod(godName)));

			sender.sendMessage(ChatColor.AQUA + "Holy creature: " + ChatColor.YELLOW + LanguageManager.get().getMobTypeName(GodManager.get().getHolyMobTypeForGod(godName)));
			sender.sendMessage(ChatColor.AQUA + "Unholy creature: " + ChatColor.YELLOW + LanguageManager.get().getMobTypeName(GodManager.get().getUnholyMobTypeForGod(godName)));
		}

		List<String> allyRelations = GodManager.get().getAllianceRelations(godName);
		Object warRelations = GodManager.get().getWarRelations(godName);

		if ((((List<?>) warRelations).size() > 0) || (allyRelations.size() > 0)) {
			sender.sendMessage(ChatColor.AQUA + "Religious relations: ");
			for (String ally : GodManager.get().getAllianceRelations(godName)) {
				sender.sendMessage(ChatColor.GREEN + " Alliance with " + ChatColor.GOLD + ally);
			}
			List<String> enemies = GodManager.get().getWarRelations(godName);
			for (String enemy : enemies) {
				sender.sendMessage(ChatColor.RED + " War with " + ChatColor.GOLD + enemy);
			}
		}

	}

}
