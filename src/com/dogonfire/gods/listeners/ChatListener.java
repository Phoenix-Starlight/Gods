package com.dogonfire.gods.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.ChatManager;
import com.dogonfire.gods.managers.GodManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatListener implements Listener
{
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		Player player = event.getPlayer();

		String godName = BelieverManager.instance().getGodForBeliever(player.getUniqueId());
		if (GodsConfiguration.instance().isChatFormattingEnabled())
		{
			event.setFormat(ChatManager.instance().formatChat(event.getPlayer(), godName, event.getFormat()));
		}

		if (godName == null)
		{
			return;
		}
		
		if (BelieverManager.instance().getReligionChat(player.getUniqueId()))
		{
			event.setCancelled(true);
			
			for (Player otherPlayer : Gods.instance().getServer().getOnlinePlayers())
			{
				String otherGod = BelieverManager.instance().getGodForBeliever(otherPlayer.getUniqueId());
				if ((otherGod != null) && (otherGod.equals(godName)))
				{
					//AtomicBoolean isPriest = new AtomicBoolean(false);
					//Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(
					//		Gods.instance(), () -> {
					//			isPriest.set(GodManager.instance().isPriest(player.getUniqueId()));
					//		});
					//if (isPriest.get())
					//{
					//	otherPlayer.sendMessage(ChatColor.YELLOW + "[" + godName + "Chat] " + player.getName() + ": " + ChatColor.WHITE + event.getMessage());
					//}
					//else
					//{
						otherPlayer.sendMessage(ChatColor.YELLOW + "[" + godName + "Chat] " + ChatColor.RED + player.getName() + ChatColor.YELLOW + ": " + ChatColor.WHITE + event.getMessage());
					//}
				}
			}
			Gods.instance().log(player.getName() + "(GODCHAT): " + event.getMessage());
		}
	}
}