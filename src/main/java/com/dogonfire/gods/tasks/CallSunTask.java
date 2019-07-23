package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import java.util.Random;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CallSunTask implements Runnable
{
	private Gods	plugin;
	private long	stopTime;
	private Player	player;
	private Random	random	= new Random();

	public CallSunTask(Gods instance, Player player, long stopTime)
	{
		this.plugin = instance;
		this.stopTime = stopTime;
		this.player = player;
	}

	public void run()
	{
		long time = this.player.getWorld().getFullTime() % 24000L;
		if ((time > 13000L) || (time < 1000L))
		{
			this.player.playSound(this.player.getLocation(), Sound.BLOCK_STONE_STEP, 1.0F, 0.1F);
			this.player.getWorld().setFullTime(this.player.getWorld().getFullTime() + 1000L);
			this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new CallSunTask(this.plugin, this.player, this.stopTime), 20L);
		}
	}
}