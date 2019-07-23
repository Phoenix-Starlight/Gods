package com.dogonfire.gods.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class CommandToggleWar extends GodsCommand
{

	protected CommandToggleWar()
	{
		super("war");
		this.permission = "gods.priest.war";
		this.parameters = "<god>";
		this.description = "Toggle war with another religion";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!hasPermission(sender))
		{
			sender.sendMessage(stringNoPermission);
			return;
		}
		if (sender instanceof Player == false)
		{
			sender.sendMessage(stringPlayerOnly);
			return;
		}
		Player player = (Player) sender;
		if (!GodManager.get().isPriest(player.getUniqueId()))
		{
			player.sendMessage(stringPreistOnly);
			return;
		}
		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		String enemyGodName = GodManager.get().formatGodName(args[1]);
		if (!GodManager.get().godExist(args[1]))
		{
			player.sendMessage(ChatColor.RED + "There is no God with the name " + ChatColor.GOLD + args[1]);
			return;
		}
		List<String> alliances = GodManager.get().getAllianceRelations(godName);
		if (alliances.contains(enemyGodName))
		{
			player.sendMessage(ChatColor.RED + "You are ALLIED with " + ChatColor.GOLD + args[1] + ChatColor.RED + "!");
			return;
		}
		if (GodManager.get().toggleWarRelationForGod(godName, enemyGodName))
		{
			LanguageManager.get().setPlayerName(godName);
			GodManager.get().godSayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);

			LanguageManager.get().setPlayerName(enemyGodName);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWar, 10);
		}
		else
		{
			LanguageManager.get().setPlayerName(godName);
			GodManager.get().godSayToBelievers(enemyGodName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);

			LanguageManager.get().setPlayerName(enemyGodName);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversWarCancelled, 10);
		}

	}
}
