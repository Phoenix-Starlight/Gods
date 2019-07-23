package com.dogonfire.gods;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.dogonfire.gods.commands.GodsCommandExecuter;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.listeners.BlockListener;
import com.dogonfire.gods.listeners.ChatListener;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;
import com.dogonfire.gods.tasks.TaskInfo;

public class Gods extends JavaPlugin
{
	private static Gods pluginInstance;
	public boolean vaultEnabled	= false;

	public static Gods get()
	{
		return pluginInstance;
	}

	public boolean isBlacklistedGod(String godName)
	{
		if (GodsConfiguration.get().isUseBlacklist())
		{
			return WhitelistManager.get().isBlacklistedGod(godName);
		}
		return false;
	}

	public boolean isEnabledInWorld(World world)
	{
		return GodsConfiguration.get().getWorlds().contains(world.getName());
	}

	public boolean isWhitelistedGod(String godName)
	{
		if (GodsConfiguration.get().isUseWhitelist())
		{
			return WhitelistManager.get().isWhitelistedGod(godName);
		}
		return true;
	}
	
	public void log(String message)
	{
		this.getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (GodsConfiguration.get().isDebug())
		{
			this.getLogger().info("[Debug] " + message);
		}
	}

	@Override
	public void onDisable()
	{
		reloadSettings();

		GodManager.get().save();
		QuestManager.get().save();
		BelieverManager.get().save();
		
		if ((GodsConfiguration.get().isUseBlacklist()) || (GodsConfiguration.get().isUseWhitelist()))
			WhitelistManager.get().save();
		if (GodsConfiguration.get().isHolyLandEnabled())
			HolyLandManager.get().save();
		if (GodsConfiguration.get().isBiblesEnabled())
			HolyBookManager.get().save();
		
		pluginInstance = null;
	}

	@Override
	public void onEnable()
	{
		pluginInstance = this;
		
		getCommand("gods").setExecutor(GodsCommandExecuter.get());
		
		GodsConfiguration.get().loadSettings();
		GodsConfiguration.get().saveSettings();
		
		PluginManager pm = getServer().getPluginManager();

		// Check for Vault
		if (pm.getPlugin("Vault") != null)
		{
			this.vaultEnabled = true;

			log("Vault detected.");
		}
		else
		{
			log("Vault not found.");
		}
		// Check for PlaceholderAPI
		if (pm.getPlugin("PlaceholderAPI") != null)
		{
			log("PlaceholderAPI found.");
			new GodsPlaceholderExpansion(Gods.get()).register();
		}
		else
		{
			logDebug("PlaceholderAPI not found.");
		}
		
		PermissionsManager.get().load();
		LanguageManager.get().load();
		GodManager.get().load();
		QuestManager.get().load();
		BelieverManager.get().load();
		WhitelistManager.get().load();
		
		getServer().getPluginManager().registerEvents(HolyLandManager.get(), this);
		getServer().getPluginManager().registerEvents(new BlockListener(), this);
		getServer().getPluginManager().registerEvents(new ChatListener(), this);

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				GodManager.get().update();
			}
		}.runTaskTimer(this, 20L, 200L);

	}

	public void reloadSettings()
	{
		reloadConfig();

		GodsConfiguration.get().loadSettings();

		WhitelistManager.get().load();
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, int amount, String name, int delay)
	{
		Player player = getServer().getPlayer(playerId);

		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, amount, name), delay);
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}
		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, name, amount1, amount2), delay);
	}

	public void sendInfo(UUID playerId, LanguageManager.LANGUAGESTRING message, ChatColor color, String name1, String name2, int delay)
	{
		Player player = getServer().getPlayer(playerId);
		if (player == null)
		{
			logDebug("sendInfo can not find online player with id " + playerId);
			return;
		}

		getServer().getScheduler().runTaskLater(this, new TaskInfo(color, playerId, message, name1, name2), delay);
	}
}