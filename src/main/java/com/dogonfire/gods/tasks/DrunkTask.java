package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.HolyPowerManager;

import java.util.Random;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DrunkTask implements Runnable
{
	private Gods plugin;
	private Player player;
	private long amount;
	private Random random = new Random();

	public DrunkTask(Gods instance, Player player, long amount)
	{
		this.plugin = instance;
		this.amount = amount;
		this.player = player;
	}

	public void run()
	{
		this.player.playSound(this.player.getLocation(), Sound.AMBIENT_CAVE, 1.0F, 0.1F);

		Entity[] entities = HolyPowerManager.get().getNearbyLivingEntities(this.player.getLocation(), 20.0D);
		int n = 0;
		for (Entity entity : entities)
		{
			if (this.player.getEntityId() != entity.getEntityId())
			{
				LivingEntity targetEntity = (LivingEntity) entity;
				targetEntity.setHealth(targetEntity.getMaxHealth());
				targetEntity.getWorld().playEffect(targetEntity.getLocation(), Effect.ENDER_SIGNAL, 0);
				n++;
			}
		}
	}
}
