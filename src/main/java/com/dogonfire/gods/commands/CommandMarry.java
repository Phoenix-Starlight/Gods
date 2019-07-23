package com.dogonfire.gods.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.MarriageManager;

public class CommandMarry extends GodsCommand
{

	protected CommandMarry()
	{
		super("marry");
		this.permission = "gods.marry";
		this.parameters = "<name>";
		this.description = "Ask another player to marry you";
	}

	@Override
	public void onCommand(CommandSender sender, String command, String... args)
	{
		if (!GodsConfiguration.get().isMarriageEnabled())
		{
			sender.sendMessage(ChatColor.RED + "Marrige is not enabled on this server");
		}
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
		String thisGodName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		if (thisGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not believe in a God");
			return;
		}

		if (args.length < 2)
		{
			player.sendMessage(ChatColor.RED + "Marry who!?");
			return;
		}

		String otherPlayerName = args[1];
		if (player.getName().equalsIgnoreCase(otherPlayerName))
		{
			player.sendMessage(ChatColor.RED + "Marry yourself!? Think again...");
			return;
		}

		Player otherPlayer = Gods.get().getServer().getPlayer(otherPlayerName);
		if (otherPlayer == null)
		{
			player.sendMessage(ChatColor.RED + "There is no player with the name '" + ChatColor.WHITE + otherPlayerName + ChatColor.RED + " online.");
			return;
		}

		String otherGodName = BelieverManager.get().getGodForBeliever(otherPlayer.getUniqueId());

		if (otherGodName == null)
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " does not believe in a God");
			return;
		}

		if (!thisGodName.equals(otherGodName))
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " does not believe in " + ChatColor.GOLD + thisGodName);
			return;
		}

		String partnerName = MarriageManager.get().getPartnerName(otherPlayer.getUniqueId());
		if (partnerName != null)
		{
			player.sendMessage(ChatColor.WHITE + otherPlayerName + ChatColor.RED + " is already married to " + ChatColor.WHITE + partnerName + "!");
			return;
		}
		partnerName = MarriageManager.get().getPartnerName(otherPlayer.getUniqueId());
		if (partnerName != null)
		{
			player.sendMessage(ChatColor.RED + "You are already married to " + ChatColor.WHITE + partnerName + "!");
			return;
		}

		MarriageManager.get().proposeMarriage(player.getUniqueId(), otherPlayer.getUniqueId());

		LanguageManager.get().setPlayerName(player.getName());
		GodManager.get().GodSayWithQuestion(thisGodName, otherPlayer, LanguageManager.LANGUAGESTRING.GodToBelieverMarriageProposal, 1);

		player.sendMessage(ChatColor.AQUA + "You proposed " + ChatColor.WHITE + otherPlayerName + ChatColor.AQUA + " to marry you in the name of " + ChatColor.GOLD + thisGodName + "!");

	}
}
