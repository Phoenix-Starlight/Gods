package com.dogonfire.gods.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dogonfire.gods.managers.GodManager;

public class CommandAccept extends GodsCommand
{

	protected CommandAccept()
	{
		super("accept");
		this.permission = "gods.accept";
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
		
		GodManager.instance().believerAccept(((Player) sender).getUniqueId());
	}
}
