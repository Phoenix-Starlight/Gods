package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpawnGuideMobTask implements Runnable
{
	private Gods		plugin		= null;
	private EntityType	mobType;
	private Location	targetLocation;
	private Player		player;
	private Creature	spawnedMob	= null;
	private int			runs		= 0;
	private int			taskId		= 0;

	public SpawnGuideMobTask(Gods instance, Player p, Location location, EntityType entityType)
	{
		this.targetLocation = location;
		this.player = p;
		this.plugin = instance;
		this.mobType = entityType;
	}

	public void run()
	{
		if (this.runs == 0)
		{
			Vector dir = this.targetLocation.subtract(this.player.getLocation().toVector()).toVector();

			dir.normalize();
			dir.multiply(17);

			Location location = this.player.getLocation().add(new Location(this.player.getWorld(), dir.getBlockX(), 0.0D, dir.getBlockZ()));

			Entity entity = this.player.getWorld().spawnEntity(location, this.mobType);
			if (entity != null)
			{
				this.spawnedMob = ((Creature) entity);
				this.spawnedMob.setTarget(this.player);

				this.taskId = this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this, 40L);
			}
		}
		else if (this.runs < 2)
		{
			this.taskId = this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this, 40L);
			this.spawnedMob.setVelocity(new Vector(0.0F, 0.5F, 0.0F));
		}
		else
		{
			this.spawnedMob.remove();
		}
		this.runs += 1;
	}
}