package com.dogonfire.gods.managers;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class WhitelistManager
{
	private static WhitelistManager instance;

	public static WhitelistManager instance()
	{
		if (instance == null)
			instance = new WhitelistManager();
		return instance;
	}

	private FileConfiguration	whiteList		= null;

	private File				whiteListFile	= null;
	private FileConfiguration	blackList		= null;
	private File				blackListFile	= null;

	private WhitelistManager()
	{
	}

	public float getMinGodPower(String godName)
	{
		int power = this.whiteList.getInt(godName + ".MinPower");

		return power;
	}

	public boolean isBlacklistedGod(String godName)
	{
		String name = this.blackList.getString(godName);

		return name != null;
	}

	public boolean isWhitelistedGod(String godName)
	{
		String name = this.whiteList.getString(godName);

		return name != null;
	}

	public void load()
	{
		if (GodsConfiguration.instance().isUseWhitelist())
		{
			if (this.whiteListFile == null)
			{
				this.whiteListFile = new File(Gods.instance().getDataFolder(), "whitelist.yml");
			}

			this.whiteList = YamlConfiguration.loadConfiguration(this.whiteListFile);

			Gods.instance().log("Loaded " + this.whiteList.getKeys(false).size() + " whitelisted Gods.");

			if (this.whiteList.getKeys(false).size() == 0)
			{
				this.whiteList.set("TheExampleGodName.MinPower", Integer.valueOf(0));

				save();
			}

			GodsConfiguration.instance().setUseBlacklist(false);

			Gods.instance().log("Using whitelist");
		}

		if (GodsConfiguration.instance().isUseBlacklist())
		{
			if (this.blackListFile == null)
			{
				this.blackListFile = new File(Gods.instance().getDataFolder(), "blacklist.yml");
			}
			this.blackList = YamlConfiguration.loadConfiguration(this.blackListFile);

			Gods.instance().log("Loaded " + this.blackList.getKeys(false).size() + " blacklisted Gods.");
			if (this.blackList.getKeys(false).size() == 0)
			{
				this.blackList.set("TheExampleGodName", "");

				save();
			}
			GodsConfiguration.instance().setUseWhitelist(false);

			Gods.instance().log("Using blacklist");
		}
	}

	public void save()
	{
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					whiteList.save(whiteListFile);
				}
				catch (IOException ex) {
					Gods.instance().log("Could not save whitelist to " + whiteListFile + ": " + ex.getMessage());
				}
				try {
					blackList.save(blackListFile);
				}
				catch (IOException ex) {
					Gods.instance().log("Could not save blacklist to " + blackListFile + ": " + ex.getMessage());
				}
			}
		}.runTaskAsynchronously(Gods.instance());
	}
}