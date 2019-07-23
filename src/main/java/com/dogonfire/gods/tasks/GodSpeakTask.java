package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class GodSpeakTask implements Runnable
{
	private Gods							plugin;
	private UUID							playerId			= null;
	private String							godName				= null;
	private LanguageManager.LANGUAGESTRING	message				= null;
	private int								amount				= 0;
	private String							playerNameString	= null;
	private String							typeString			= null;

	public GodSpeakTask(Gods instance, String gname, UUID playerId, String player, String type, int a)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.godName = new String(gname);
		this.message = null;
	}

	public GodSpeakTask(Gods instance, String gname, UUID playerId, String player, String type, int a, LANGUAGESTRING m)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.godName = new String(gname);
		this.message = m;

		this.playerNameString = new String(player);
		this.amount = a;
		if (type != null)
		{
			this.typeString = new String(type);
		}
		else
		{
			type = "";
		}
	}

	private void godWeakSpeak(String godName, Player player)
	{
		String message = "";
		Random r = new Random();
		switch (r.nextInt(15))
		{
			case 0:
				message = "...";
				break;
			case 1:
				message = "* Whisper *";
				break;
			case 2:
				message = "......";
				break;
			case 3:
				message = "Believe...";
				break;
			case 4:
				message = "Faith...";
				break;
			case 5:
				message = "Pray...";
				break;
			case 6:
				message = "Church...";
				break;
			case 7:
				message = "Altar...";
				break;
			case 8:
				message = "Power...";
				break;
			case 9:
				message = "...Believers...";
				break;
			case 10:
				message = "...rewards";
				break;
			case 11:
				message = "...Blessings...";
				break;
			case 12:
				message = "...Rewards...";
				break;
			case 13:
				message = "Fate...";
				break;
			case 14:
				message = "Destiny...";
		}
		player.sendMessage(ChatColor.GOLD + "<" + godName + ">: " + ChatColor.WHITE + message);
	}

	public void run()
	{
		Player player = this.plugin.getServer().getPlayer(this.playerId);
		if (player == null)
		{
			return;
		}
		
		LanguageManager.get().setAmount(this.amount);
		
		try
		{
			LanguageManager.get().setType(this.typeString);
		}
		catch (Exception ex)
		{
			this.plugin.logDebug(ex.getStackTrace().toString());
		}
		
		LanguageManager.get().setPlayerName(this.playerNameString);
		
		if (this.message != null)
		{
			player.sendMessage(ChatColor.GOLD + "<" + this.godName + ">: " + ChatColor.WHITE +

			ChatColor.BOLD + LanguageManager.get().getLanguageString(this.godName, this.message));
		}
		else
		{
			String questionMessage = ChatColor.AQUA + "Use " + ChatColor.WHITE + "/gods yes" + ChatColor.AQUA + " or " + ChatColor.WHITE + "/gods no" + ChatColor.AQUA + " to answer your god.";

			player.sendMessage(ChatColor.GOLD + "[" + "Gods" + "]: " + questionMessage);
		}
	}
}