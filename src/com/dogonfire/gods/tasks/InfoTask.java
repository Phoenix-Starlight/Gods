package com.dogonfire.gods.tasks;

import java.util.UUID;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class InfoTask implements Runnable
{
	private Gods							plugin;
	private UUID							playerId	= null;
	private String							name1		= null;
	private String							name2		= null;
	private LANGUAGESTRING	message		= null;
	private int								amount		= 0;
	private ChatColor						color;

	public InfoTask(Gods instance, ChatColor color, UUID playerId, LANGUAGESTRING m, int amount, String name1)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.message = m;
		this.name1 = name1;
		this.amount = amount;
		this.color = color;
	}

	public InfoTask(Gods instance, ChatColor color, UUID playerId, LANGUAGESTRING m, String name1, String name2)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.name1 = name1;
		this.name2 = name2;
		this.message = m;
		this.color = color;
	}

	public InfoTask(Gods instance, ChatColor color, UUID playerId, LANGUAGESTRING m, String name, int amount1, int amount2)
	{
		this.plugin = instance;
		this.playerId = playerId;
		this.name1 = name;
		this.name2 = String.valueOf(amount1);
		this.message = m;
		this.amount = amount2;
		this.color = color;
	}

	public void run()
	{
		Player player = this.plugin.getServer().getPlayer(this.playerId);

		if (player == null)
		{
			return;
		}

		LanguageManager.instance().setPlayerName(this.name1);

		try
		{
			LanguageManager.instance().setType(this.name2);
		}
		catch (Exception ex)
		{
			this.plugin.logDebug(ex.getStackTrace().toString());
		}

		LanguageManager.instance().setAmount(this.amount);

		String questionMessage = LanguageManager.instance().getInfoString(this.message, this.color);

		player.sendMessage(

		this.color + questionMessage);
	}
}