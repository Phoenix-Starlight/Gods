package com.dogonfire.gods.managers;


import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.dogonfire.gods.Gods;

import net.milkbowl.vault.permission.Permission;

public class PermissionsManager
{
	private static PermissionsManager instance;

	public static PermissionsManager instance()
	{
		if (instance == null)
			instance = new PermissionsManager();
		return instance;
	}

	private String				pluginName			= "null";
	private Permission 			vaultPermission;
	
	private PermissionsManager()
	{
		RegisteredServiceProvider<Permission> permissionProvider = Gods.instance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		
		if(permissionProvider==null)
		{
			Gods.instance().log(ChatColor.RED + "Could not detect Vault plugin.");
			return;
		}
		
		vaultPermission = permissionProvider.getProvider();
	}

	public void load()
	{
		// Nothing to see here
	}

	public String getPermissionPluginName()
	{
		return pluginName;
	}

	public boolean hasPermission(Player player, String node)
	{
		return vaultPermission.has(player, node);
	}

	public String getGroup(String playerName)
	{
		return vaultPermission.getPrimaryGroup(Gods.instance().getServer().getPlayer(playerName));
	}

	public void setGroup(String playerName, String groupName)
	{
		Player player = Gods.instance().getServer().getPlayer(playerName);
		vaultPermission.playerAddGroup(player, groupName);
	}
}