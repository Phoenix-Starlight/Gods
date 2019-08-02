package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SpawnHostileMobsTask implements Runnable
{
	private Gods		plugin			= null;
	private EntityType	mobType;
	private int			numberOfMobs	= 0;
	private Player		player;
	private String		godName;

	public SpawnHostileMobsTask(Gods instance, String god, Player p, EntityType entityType, int n)
	{
		this.plugin = instance;
		this.numberOfMobs = n;
		this.player = p;
		this.godName = new String(god);
		this.mobType = entityType;
	}

	public void run()
	{
		for (int i = 0; i < this.numberOfMobs; i++)
		{
			Creature spawnedMob = (Creature) this.player.getWorld().spawnEntity(this.player.getLocation(), this.mobType);
			spawnedMob.setTarget(this.player);
		}
	}
}
