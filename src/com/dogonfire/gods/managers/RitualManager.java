package com.dogonfire.gods.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.GodManager.GodType;

public class RitualManager
{
	private static RitualManager instance;

	public static RitualManager get()
	{
		if (instance == null)
			instance = new RitualManager();
		return instance;
	}

	private Random							random			= new Random();
	private Map<Integer, String>			droppedItems	= new HashMap<Integer, String>();
	private Map<Material, List<GodType>>	altarBlockTypes	= new HashMap<Material, List<GodType>>();

	private RitualManager()
	{
	}

	public void addDroppedItem(int entityID, String playerName)
	{
		this.droppedItems.put(Integer.valueOf(entityID), playerName);
	}

	public void clearDroppedItems()
	{
		Gods.instance().logDebug("Cleared " + this.droppedItems.size() + " dropped items");
		this.droppedItems.clear();
	}

	

	
}