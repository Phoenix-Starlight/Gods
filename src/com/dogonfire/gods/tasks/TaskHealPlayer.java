package com.dogonfire.gods.tasks;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;

public class TaskHealPlayer extends Task
{
	private Player							player	= null;
	private String							godName	= null;
	private LanguageManager.LANGUAGESTRING	languageString;

	public TaskHealPlayer(String god, Player p, LanguageManager.LANGUAGESTRING speak)
	{
		this.player = p;
		this.godName = god;
		this.languageString = speak;
	}

	private boolean healPlayer()
	{
		this.player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 10, 1));

		this.player.getLocation().getWorld().playEffect(this.player.getLocation(), Effect.MOBSPAWNER_FLAMES, 4);
		return true;
	}

	@Override
	public void run()
	{
		Random random = new Random();
		if (healPlayer())
		{
			LanguageManager.instance().setPlayerName(this.player.getName());
			GodManager.instance().GodSay(this.godName, this.player, this.languageString, 2 + random.nextInt(10));
			getPlugin().log(this.godName + " healed " + this.player.getName());
		}
	}
}
