package com.dogonfire.gods.tasks;

import java.util.Random;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.LanguageManager;

public class GiveHolyArtifactTask implements Runnable
{
	private Gods				plugin;
	private Player				player	= null;
	private String				godName	= null;
	private GodManager.GodType	godType	= null;
	private boolean				speak	= false;

	public GiveHolyArtifactTask(Gods instance, String god, GodManager.GodType godType, Player p, boolean godspeak)
	{
		this.plugin = instance;
		this.player = p;
		this.godName = god;
		this.godType = godType;
		this.speak = godspeak;
	}

	private boolean giveItem()
	{
		Vector dir = this.player.getLocation().getDirection();

		dir.setY(0);

		Location spawnLocation = this.player.getLocation().toVector().add(dir.multiply(4)).toLocation(this.player.getWorld());

		spawnLocation.setY(spawnLocation.getY() + 2.0D);
		if (spawnLocation.getBlock().getType() != Material.AIR)
		{
			this.plugin.logDebug("Could not giveItem(): Not air infront of " + this.player.getName());
			return false;
		}
		try
		{
			this.plugin.logDebug("Creating holy artifact for " + this.player.getName());

			Item artifact = HolyArtifactManager.instance().createHolyArtifact(this.player.getName(), this.godType, this.godName, spawnLocation);

			spawnLocation.getWorld().playEffect(spawnLocation, Effect.MOBSPAWNER_FLAMES, 25);
		}
		catch (Exception ex)
		{
			this.plugin.log("Could not giveItem(): " + ex.getMessage());
			return false;
		}
		return true;
	}

	public void run()
	{
		if (giveItem())
		{
			Random random = new Random();
			if (this.speak)
			{
				LanguageManager.instance().setPlayerName(this.player.getName());
				GodManager.instance().GodSay(this.godName, this.player, LanguageManager.LANGUAGESTRING.GodToBelieverHolyArtifactBlessing, 2 + random.nextInt(10));
			}
		}
	}
}
