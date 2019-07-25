package com.dogonfire.gods.managers;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;

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

	private RitualManager()
	{
	}

	public boolean handleAltarPray(Location altarLocation, Player player, String godName)
	{
		return false;
	}	
}