package com.dogonfire.gods.tasks;

import java.util.Random;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.HolyPowerManager;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

public class LoveTask implements Runnable
{
	private Gods	plugin;
	private Player	player1;
	private Player	player2;
	private int		cycle	= 3;

	public LoveTask(Gods instance, Player player1, Player player2)
	{
		this.plugin = instance;
		this.player1 = player1;
		this.player2 = player2;
	}

	public LoveTask(Gods instance, Player player1, Player player2, int cycle)
	{
		this.plugin = instance;
		this.player1 = player1;
		this.player2 = player2;
		this.cycle = cycle;
	}

	public void run()
	{
		if ((!this.player1.isOnline()) || (!this.player2.isOnline()))
		{
			return;
		}

		Random random = new Random();
		boolean firework = (random.nextInt(100) == 0);

		{
			Wolf wolf = player1.getWorld().spawn(this.player1.getLocation(), Wolf.class);
			wolf.playEffect(EntityEffect.WOLF_HEARTS);		
			wolf.remove();

			if (firework)
			{
				HolyPowerManager.get().shootFirework(player1, 16);
			}
		}

		{
			Wolf wolf = player2.getWorld().spawn(this.player2.getLocation(), Wolf.class);
			wolf.playEffect(EntityEffect.WOLF_HEARTS);		
			wolf.remove();

			if (firework)
			{
				HolyPowerManager.get().shootFirework(player2, 16);
			}
		}	

		int newcycle = this.cycle - 1;
		if (newcycle > 0)
		{
			this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new LoveTask(this.plugin, this.player1, this.player2, newcycle), 40L);
		}
	}

	/*
	 * private void onLove()
	 * 
	 * { if (this.plugin.getGodManager().getDivineForceForGod(godName) ==
	 * GodManager.GodType.LOVE) { powerBefore =
	 * this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());
	 * this.plugin.getBelieverManager().increasePrayer(killer.getUniqueId(),
	 * killerGodName, 2);
	 * this.plugin.getBelieverManager().increasePrayerPower(killer
	 * .getUniqueId(), 2); powerAfter =
	 * this.plugin.getBelieverManager().getBelieverPower(player.getUniqueId());
	 * 
	 * this.plugin.sendInfo(killer.getUniqueId(),
	 * LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen,
	 * ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20); } }
	 */
}