package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.LanguageManager.LANGUAGESTRING;

import java.util.Random;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HealPlayerTask implements Runnable
{
	private Gods			plugin;
	private Player			player	= null;
	private String			godName	= null;
	private LANGUAGESTRING	languageString;

	public HealPlayerTask(Gods instance, String god, Player p, LANGUAGESTRING speak)
	{
		this.plugin = instance;
		this.player = p;
		this.godName = new String(god);
		this.languageString = speak;
	}

	private boolean healPlayer()
	{
		this.player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 10, 1));

		this.player.getLocation().getWorld().playEffect(this.player.getLocation(), Effect.MOBSPAWNER_FLAMES, 4);
		return true;
	}

	public void run()
	{
		Random random = new Random();
		if (healPlayer())
		{
			LanguageManager.instance().setPlayerName(this.player.getName());
			GodManager.instance().GodSay(this.godName, this.player, this.languageString, 2 + random.nextInt(10));

			this.plugin.log(this.godName + " healed " + this.player.getName());
		}
	}
}
