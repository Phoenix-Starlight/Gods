package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class BoostKnowledgeTask implements Runnable
{
	private Gods	plugin;
	private Player	player;
	private long	amount;

	public BoostKnowledgeTask(Gods instance, Player player, long amount)
	{
		this.plugin = instance;
		this.amount = amount;
		this.player = player;
	}

	public void run()
	{
		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		this.player.setExp(this.player.getExp() + (float) this.amount);
	}
}
