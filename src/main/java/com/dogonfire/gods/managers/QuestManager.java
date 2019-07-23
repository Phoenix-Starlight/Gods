package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class QuestManager
{
	static enum QUESTTYPE {
		NONE, FIREWORKPARTY, HUMANSACRIFICE, MOBSACRIFICE, ITEMSACRIFICE, GIVEROSE, KILLBOSS, HARVEST, CONVERT, GETHOLYARTIFACT, DELIVERITEM, SLAY, BUILDALTARS, BUILDTOWER, HOLYFEAST, COLLECTBIBLES, BURNBIBLES, GIVEITEMS, PILGRIMAGE, HOLYWAR, CLAIMHOLYLAND, PVPREVENGE, SPREADLOVE, SHOWHOLYARTIFACT, CRAFTITEMS, SLAYDRAGON, CRUSADE;

		@Override
		public String toString()
		{
			String output = name().toString();
			output = output.charAt(0) + output.substring(1).toLowerCase();

			return output;
		}
	}

	private static QuestManager instance;

	public static QuestManager get()
	{
		if (instance == null)
			instance = new QuestManager();
		
		return instance;
	}

	private FileConfiguration			questsConfig		= null;
	private File						questsConfigFile	= null;

	private Random						random				= new Random();

	private HashMap<Material, Integer>	rewardValues		= new HashMap<Material, Integer>();

	private QuestManager()
	{
	}

	private boolean addQuestPlayerProgressForGod(String godName, UUID playerId)
	{
		boolean complete = false;

		List<String> players = this.questsConfig.getStringList(godName + ".Players");
		if (players.contains(playerId.toString()))
		{
			return false;
		}

		players.add(playerId.toString());

		int questAmount = this.questsConfig.getInt(godName + ".Amount");

		complete = players.size() >= questAmount;

		this.questsConfig.set(godName + ".Players", players);
		this.questsConfig.set(godName + ".Progress", Integer.valueOf(players.size()));

		save();

		return complete;
	}

	private boolean addQuestProgressForGod(String godName)
	{
		boolean complete = false;

		int questAmount = this.questsConfig.getInt(godName + ".Amount");
		int questProgress = this.questsConfig.getInt(godName + ".Progress");

		questProgress++;

		complete = questProgress >= questAmount;

		this.questsConfig.set(godName + ".Progress", Integer.valueOf(questProgress));

		save();

		return complete;
	}

	void clearQuestTarget(Location location)
	{
		int hash = hashVector(location);
		this.questsConfig.set("QuestTargets." + hash, null);

		this.save();
	}

	Location generateAncientCaveTemple(ItemStack item, String godName, String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;

		Block target = null;
		Inventory contents = null;

		List<Material> defaultspawnblocks = new ArrayList<Material>();

		defaultspawnblocks.add(Material.STONE);
		defaultspawnblocks.add(Material.SMOOTH_STONE);
		defaultspawnblocks.add(Material.MOSSY_COBBLESTONE);
		defaultspawnblocks.add(Material.OBSIDIAN);

		World world = Gods.get().getServer().getWorld(worldName);
		int maxLevel;
		int x;
		int z;
		int y;
		do
		{
			run++;

			int minLevel = 4;
			maxLevel = 50;
			int maxLight = 4;
			int minLight = 0;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist));
			do
			{
				y = this.random.nextInt(maxLevel);
			}
			while (

			y < minLevel);
			target = world.getBlockAt(x, y, z);
			if ((target.getType() == Material.AIR) && (world.getBlockAt(x, y + 1, z).getType() == Material.AIR) && (target.getLightLevel() <= maxLight) && (target.getLightLevel() >= minLight))
			{
				target = world.getBlockAt(x, y - 1, z);
				if ((defaultspawnblocks.contains(target.getType())) && (world.getHighestBlockAt(target.getLocation()).getType() != Material.WATER))
				{
					target.setType(Material.GLOWSTONE);

					target = world.getBlockAt(x, y, z);
					target.setType(Material.CHEST);

					Chest tb = (Chest) target.getState();
					contents = tb.getInventory();
				}
			}
		}
		while ((contents == null) && (run < 100));

		if (run >= 100)
		{
			Gods.get().log("Ancient cave holy artifact chest generation FAILED in " + worldName);
			return null;
		}

		contents.addItem(new ItemStack[] { item });

		for (ItemStack rewardItem : getRewardsForQuestCompletion(200 + this.random.nextInt(200)))
		{
			contents.addItem(new ItemStack[] { rewardItem });
		}

		for (int oy = y - 7; oy < y + 7; oy++)
		{
			for (int ox = x - 20; ox < x + 20; ox++)
			{
				for (int oz = z - 20; oz < z + 20; oz++)
				{
					Block stoneTarget = world.getBlockAt(ox, oy, oz);
					if ((this.random.nextInt(4) == 0) && (stoneTarget.getType() == Material.AIR) && (world.getBlockAt(ox, oy - 1, oz).getType() == Material.STONE))
					{
						stoneTarget.setType(Material.CHEST);
						world.getBlockAt(ox, oy - 1, oz).setType(Material.GLOWSTONE);
					}
					else if ((stoneTarget.getType() == Material.STONE) && (this.random.nextInt(4) == 0))
					{
						stoneTarget.setType(Material.SMOOTH_STONE);
					}
				}
			}
		}
		Gods.get().logDebug("Ancient cave holy artifact chest generated in " + run + " runs");

		return target.getLocation();
	}

	private boolean generateGiveItemsQuest(String godName, Material material)
	{
		String questTargetType = material.name();
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.GIVEITEMS;
		questAmount = 1 + (Gods.get().getServer().getOnlinePlayers().size() - BelieverManager.get().getOnlineBelieversForGod(godName).size()) / (2 + this.random.nextInt(3));
		questMaxDuration = (1 + this.random.nextInt(5)) * questAmount;

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Make " + questAmount + " non-believers read the Holy Book");

		return true;
	}

	private String generateHolyMountainName()
	{
		return "Holy mountain of " + generateHolyName();
	}

	private String generateHolyName()
	{
		int length = 2 + this.random.nextInt(6);
		String templeName = "";

		int l = 0;
		boolean wasVocal = false;
		while (l < length)
		{
			if (wasVocal)
			{
				switch (this.random.nextInt(20))
				{
				case 0:
					templeName = templeName + "b";
					wasVocal = false;
					break;
				case 1:
					templeName = templeName + "f";
					wasVocal = false;
					break;
				case 2:
					templeName = templeName + "g";
					wasVocal = false;
					break;
				case 3:
					templeName = templeName + "h";
					wasVocal = false;
					break;
				case 4:
					templeName = templeName + "j";
					wasVocal = false;
					break;
				case 5:
					templeName = templeName + "k";
					wasVocal = false;
					break;
				case 6:
					templeName = templeName + "l";
					wasVocal = false;
					break;
				case 7:
					templeName = templeName + "ll";
					wasVocal = false;
					break;
				case 8:
					templeName = templeName + "v";
					wasVocal = false;
					break;
				case 9:
					templeName = templeName + "r";
					wasVocal = false;
					break;
				case 10:
					templeName = templeName + "rr";
					wasVocal = false;
					break;
				case 11:
					templeName = templeName + "kk";
					wasVocal = false;
					break;
				case 12:
					templeName = templeName + "p";
					wasVocal = false;
					break;
				case 13:
					templeName = templeName + "t";
					wasVocal = false;
					break;
				case 14:
					templeName = templeName + "s";
					wasVocal = false;
					break;
				case 15:
					templeName = templeName + "x";
					wasVocal = false;
					break;
				case 16:
					templeName = templeName + "d";
					wasVocal = false;
					break;
				case 17:
					templeName = templeName + "n";
					wasVocal = false;
					break;
				case 18:
					templeName = templeName + "m";
					wasVocal = false;
					break;
				case 19:
					templeName = templeName + "nn";
					wasVocal = false;
				}
			}
			else
			{
				switch (this.random.nextInt(6))
				{
				case 0:
					templeName = templeName + "a";
					wasVocal = true;
					break;
				case 1:
					templeName = templeName + "o";
					wasVocal = true;
					break;
				case 2:
					templeName = templeName + "i";
					wasVocal = true;
					break;
				case 3:
					templeName = templeName + "u";
					wasVocal = true;
					break;
				case 4:
					templeName = templeName + "y";
					wasVocal = true;
					break;
				case 5:
					templeName = templeName + "e";
					wasVocal = true;
				}
			}
			l++;
		}
		return templeName.substring(0, 1).toUpperCase() + templeName.substring(1).toLowerCase();
	}

	private boolean generateHolyWarQuest(String godName)
	{
		String questTargetType = "NONE";
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		Set<UUID> thisGodOnlineBelievers = BelieverManager.get().getOnlineBelieversForGod(godName);

		QUESTTYPE questType = QUESTTYPE.HOLYWAR;
		questAmount = 3;
		questMaxDuration = (5 + this.random.nextInt(5)) * questAmount;

		List<String> enemyGods = GodManager.get().getEnemyGodsForGod(godName);
		if (enemyGods.size() == 0)
		{
			return false;
		}
		String otherGodName = enemyGods.get(this.random.nextInt(enemyGods.size()));

		Set<UUID> otherGodOnlineBelievers = BelieverManager.get().getOnlineBelieversForGod(otherGodName);
		if ((thisGodOnlineBelievers.size() < questAmount) || (otherGodOnlineBelievers.size() < questAmount))
		{
			return false;
		}
		Location believerPosition = Gods.get().getServer().getPlayer((String) thisGodOnlineBelievers.toArray()[this.random.nextInt(thisGodOnlineBelievers.size())]).getLocation();

		Location targetLocation = getLocationForHolywarQuest(believerPosition.getWorld().getName(), 500, 3000, believerPosition);
		if (targetLocation == null)
		{
			return false;
		}
		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Location.World", targetLocation.getWorld().getName());
		this.questsConfig.set(godName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		this.questsConfig.set(godName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		this.questsConfig.set(godName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", otherGodName);
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		this.questsConfig.set(otherGodName + ".Type", questType.toString());
		this.questsConfig.set(otherGodName + ".Location.World", targetLocation.getWorld().getName());
		this.questsConfig.set(otherGodName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		this.questsConfig.set(otherGodName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		this.questsConfig.set(otherGodName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		this.questsConfig.set(otherGodName + ".Amount", godName);
		this.questsConfig.set(otherGodName + ".TargetType", questTargetType);
		this.questsConfig.set(otherGodName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(otherGodName + ".Progress", null);
		this.questsConfig.set(otherGodName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Kill " + questAmount + " non-believers!");

		return true;
	}

	private boolean generatePilgrimageQuest(String godName)
	{
		String questTargetType = generateHolyMountainName();
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.PILGRIMAGE;
		questAmount = 1;
		questMaxDuration = 15 + this.random.nextInt(10);

		Set<UUID> thisGodOnlineBelievers = BelieverManager.get().getOnlineBelieversForGod(godName);
		if (thisGodOnlineBelievers.size() < questAmount)
		{
			Gods.get().logDebug("thisGodOnlineBelievers.size() < questAmount");
			return false;
		}

		UUID randomId = (UUID) thisGodOnlineBelievers.toArray()[this.random.nextInt(thisGodOnlineBelievers.size())];

		Location believerPosition = Gods.get().getServer().getPlayer(randomId).getLocation();

		Location targetLocation = getLocationForPilgrimageQuest(believerPosition.getWorld().getName(), 500, 2000, believerPosition);

		if (targetLocation == null)
		{
			Gods.get().logDebug("targetLocation == nul");
			return false;
		}

		this.setQuestTarget(godName, targetLocation);

		// Setup the chest
		setupPilgrimageQuest(targetLocation, godName, GodManager.get().getDivineForceForGod(godName));

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".Location.World", targetLocation.getWorld().getName());
		this.questsConfig.set(godName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		this.questsConfig.set(godName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		this.questsConfig.set(godName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Pilgrimage to " + questTargetType + " in " + targetLocation.getWorld().getName() + " !");

		return true;
	}

	public boolean generateQuest(String godName)
	{
		boolean newQuest = false;
		int t = 0;

		if (hasQuest(godName))
		{
			return false;
		}

		do
		{
			switch (this.random.nextInt(6))
			{
			case 0:
				if (GodsConfiguration.get().isPilgrimageQuestsEnabled())
				{
					newQuest = generatePilgrimageQuest(godName);
				}
				break;
			case 1:
				if (GodsConfiguration.get().isSlayQuestsEnabled())
				{
					newQuest = generateSlayQuest(godName);
				}
				break;
			case 2:
				if ((GodsConfiguration.get().isSacrificeQuestsEnabled()) && (GodsConfiguration.get().isSacrificesEnabled()))
				{
					newQuest = generateSacrificeQuest(godName);
				}
				break;
			case 3:
				if (GodsConfiguration.get().isHolywarQuestsEnabled())
				{
					newQuest = generateHolyWarQuest(godName);
				}
				break;
			case 4:
				if (GodsConfiguration.get().isGiveItemsQuestsEnabled())
				{
					GodManager.GodType godType = GodManager.get().getDivineForceForGod(godName);
					switch (godType)
					{
					case EVIL:
						newQuest = generateGiveItemsQuest(godName, Material.POPPY);
						break;
					case SUN:
						newQuest = generateGiveItemsQuest(godName, Material.STONE_AXE);
						break;
					case SEA:
						newQuest = generateGiveItemsQuest(godName, Material.CAKE);
						break;
					default:
						break;
					}
				}
				break;
			case 5:
				if (GodsConfiguration.get().isSlayDragonQuestsEnabled())
				{
					newQuest = generateSlayDragonQuest(godName);
				}
				break;
			}

			t++;
		}
		while ((!newQuest) && (t < 10));

		if (newQuest)
		{
			godSayNewQuest(godName);
			GodManager.get().setTimeSinceLastQuest(godName);
		}
		else
		{
			Gods.get().logDebug("Could not generate any quest");
		}

		return newQuest;
	}

	private boolean generateSacrificeQuest(String godName)
	{
		String questTargetType = "NONE";
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.ITEMSACRIFICE;
		questAmount = 1 + this.random.nextInt(1 + BelieverManager.get().getOnlineBelieversForGod(godName).size());
		questMaxDuration = (1 + this.random.nextInt(5)) * questAmount;

		switch (this.random.nextInt(10))
		{
		case 0:
			questTargetType = Material.GOLDEN_SWORD.name();
			break;
		case 1:
			questTargetType = Material.GOLDEN_PICKAXE.name();
			break;
		case 2:
			questTargetType = Material.GOLDEN_SHOVEL.name();
			break;
		case 3:
			questTargetType = Material.GOLDEN_AXE.name();
			break;
		case 4:
			questTargetType = Material.FISHING_ROD.name();
			break;
		case 5:
			questTargetType = Material.ANVIL.name();
			break;
		case 6:
			questTargetType = Material.OAK_BOAT.name();
			break;
		case 7:
			questTargetType = Material.BOOK.name();
			break;
		case 8:
			questTargetType = Material.BOW.name();
		case 9:
			questTargetType = Material.APPLE.name();
		}

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Sacrifice " + questAmount + " " + questTargetType);

		return true;
	}

	private boolean generateSlayDragonQuest(String godName)
	{
		String questTargetType = generateHolyMountainName();
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.SLAYDRAGON;
		questAmount = 1;
		questMaxDuration = 15 + this.random.nextInt(10);

		Set<UUID> thisGodOnlineBelievers = BelieverManager.get().getOnlineBelieversForGod(godName);
		if (thisGodOnlineBelievers.size() < questAmount)
		{
			return false;
		}
		Location believerPosition = Gods.get().getServer().getPlayer((String) thisGodOnlineBelievers.toArray()[this.random.nextInt(thisGodOnlineBelievers.size())]).getLocation();

		Location targetLocation = getLocationForSlayDragonQuest(believerPosition.getWorld().getName(), 500, 2000, believerPosition);
		if (targetLocation == null)
		{
			return false;
		}
		hashVector(targetLocation);

		this.setQuestTarget(godName, targetLocation);

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".TargetType", questTargetType);
		this.questsConfig.set(godName + ".Location.World", targetLocation.getWorld().getName());
		this.questsConfig.set(godName + ".Location.X", Integer.valueOf(targetLocation.getBlockX()));
		this.questsConfig.set(godName + ".Location.Y", Integer.valueOf(targetLocation.getBlockY()));
		this.questsConfig.set(godName + ".Location.Z", Integer.valueOf(targetLocation.getBlockZ()));
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Slay dragon quest to somewhere!");

		return true;
	}

	private boolean generateSlayQuest(String godName)
	{
		int questAmount = 0;
		int questMaxDuration = 0;
		Date thisDate = new Date();
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		QUESTTYPE questType = QUESTTYPE.SLAY;
		questAmount = 1 + this.random.nextInt(3) + BelieverManager.get().getOnlineBelieversForGod(godName).size();
		questMaxDuration = (5 + this.random.nextInt(5)) * questAmount;

		EntityType holyCreature = GodManager.get().getHolyMobTypeForGod(godName);
		EntityType targetType = EntityType.UNKNOWN;
		do
		{
			switch (this.random.nextInt(10))
			{
			case 0:
				targetType = EntityType.SHEEP;
				break;
			case 1:
				targetType = EntityType.SPIDER;
				break;
			case 2:
				targetType = EntityType.CHICKEN;
				break;
			case 3:
				targetType = EntityType.COW;
				break;
			case 4:
				targetType = EntityType.PIG;
				break;
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				targetType = GodManager.get().getUnholyMobTypeForGod(godName);
			}
		}
		while (targetType == holyCreature);

		this.questsConfig.set(godName + ".Type", questType.toString());
		this.questsConfig.set(godName + ".Amount", Integer.valueOf(questAmount));
		this.questsConfig.set(godName + ".TargetType", targetType.name());
		this.questsConfig.set(godName + ".MaxDuration", Integer.valueOf(questMaxDuration));
		this.questsConfig.set(godName + ".Progress", null);
		this.questsConfig.set(godName + ".CreatedTime", formatter.format(thisDate));

		save();

		Gods.get().log(godName + " issued a quest: Kill " + questAmount + " " + targetType.name());

		return true;
	}

	public Date getGlobalQuestCreatedTime()
	{
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		String startTime = this.questsConfig.getString("Global.CreatedTime");
		Date startTimeDate = null;
		try
		{
			startTimeDate = formatter.parse(startTime);
		}
		catch (Exception ex)
		{
			Gods.get().log("Invalid global quest created date. Reset.");
			startTimeDate = new Date();
			startTimeDate.setTime(0L);
		}
		return startTimeDate;
	}

	public Location getGlobalQuestLocation()
	{
		int x = 0;
		int y;
		int z;
		String worldName;

		try
		{
			x = this.questsConfig.getInt("Global.Location.X");
			y = this.questsConfig.getInt("Global.Location.Y");
			z = this.questsConfig.getInt("Global.Location.Z");
			worldName = this.questsConfig.getString("Global.Location.World");
		}
		catch (Exception ex)
		{
			return null;
		}

		return new Location(Gods.get().getServer().getWorld(worldName), x, y, z);
	}

	public long getGlobalQuestLockedTime()
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lockedTimeDate = null;
		long time = new Date().getTime();
		try
		{
			String lockedTime = this.questsConfig.getString("Global.LockedTime");
			lockedTimeDate = formatter.parse(lockedTime);
		}
		catch (Exception ex)
		{
			return -1L;
		}
		return time - lockedTimeDate.getTime();
	}

	public int getGlobalQuestMaxDuration()
	{
		int value = this.questsConfig.getInt("Global.MaxDuration");

		return value;
	}

	public Material getGlobalQuestTargetItemType()
	{
		Material type = null;
		try
		{
			type = Material.getMaterial(this.questsConfig.getString("Global.ItemType").toUpperCase());
		}
		catch (Exception ex)
		{
			Gods.get().logDebug("Could not recognize quest type '" + this.questsConfig.getString("Global.ItemType") + "': " + ex.getMessage());
			type = Material.AIR;
		}
		return type;
	}

	public String getGlobalQuestTargetType()
	{
		String value = this.questsConfig.getString("Global.TargetType");

		return value;
	}

	public QUESTTYPE getGlobalQuestType()
	{
		QUESTTYPE type = null;
		try
		{
			type = QUESTTYPE.valueOf(this.questsConfig.getString("Global.Type").toUpperCase());
		}
		catch (Exception ex)
		{
			Gods.get().logDebug("Could not recognize quest type '" + this.questsConfig.getString("Global.Type") + "': " + ex.getMessage());
			type = QUESTTYPE.NONE;
		}
		return type;
	}

	Location getLocationForHolywarQuest(String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;
		boolean placed = false;
		Block target = null;

		List<Material> defaultspawnblocks = new ArrayList<Material>();
		defaultspawnblocks.add(Material.GRASS);

		minDist = 1;
		maxDist = 2000;
		do
		{
			run++;

			int minLevel = 60;
			int maxLevel = 80;
			int x;
			int z;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist));
			int y;
			do
			{
				y = this.random.nextInt(maxLevel);
			}
			while (

			y < minLevel);
			World world = Gods.get().getServer().getWorld(worldName);
			target = world.getBlockAt(x, y, z);
			if ((target.getType() == Material.AIR) && (target.getRelative(BlockFace.NORTH).getType() == Material.AIR) && (target.getRelative(BlockFace.SOUTH).getType() == Material.AIR) && (target.getRelative(BlockFace.WEST).getType() == Material.AIR)
					&& (target.getRelative(BlockFace.EAST).getType() == Material.AIR))
			{
				target = world.getBlockAt(x, y - 1, z);
				if (defaultspawnblocks.contains(target.getType()))
				{
					target.setType(Material.CACTUS);

					target = world.getBlockAt(x, y, z);

					placed = true;
				}
			}
		}
		while ((!placed) && (run < 100));
		if (run >= 100)
		{
			Gods.get().log("Holy battle ground generation FAILED");
			return null;
		}
		return target.getLocation();
	}

	Location getLocationForLostHolyLand(String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;
		boolean placed = false;
		Block target = null;

		List<Material> defaultspawnblocks = new ArrayList<Material>();
		defaultspawnblocks.add(Material.GRASS);

		minDist = 1;
		maxDist = 2000;
		do
		{
			run++;

			int minLevel = 60;
			int maxLevel = 80;
			int x;
			int z;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist));
			int y;
			do
			{
				y = this.random.nextInt(maxLevel);
			}
			while (

			y < minLevel);
			World world = Gods.get().getServer().getWorld(worldName);
			target = world.getBlockAt(x, y, z);
			if ((target.getType() == Material.AIR) && (target.getRelative(BlockFace.NORTH).getType() == Material.AIR) && (target.getRelative(BlockFace.SOUTH).getType() == Material.AIR) && (target.getRelative(BlockFace.WEST).getType() == Material.AIR)
					&& (target.getRelative(BlockFace.EAST).getType() == Material.AIR))
			{
				target = world.getBlockAt(x, y - 1, z);
				if (defaultspawnblocks.contains(target.getType()))
				{
					setDominationColor(target.getRelative(BlockFace.DOWN), ChatColor.WHITE);

					target = world.getBlockAt(x, y, z);

					placed = true;
				}
			}
		}
		while ((!placed) && (run < 100));

		if (run >= 100)
		{
			Gods.get().log("Lost Holy Land claim generation FAILED");
			return null;
		}

		return target.getLocation();
	}

	public Location getLocationForPilgrimageQuest(String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;

		Block target = null;
		boolean canBuild = true;

		List<Material> defaultspawnblocks = new ArrayList<Material>();
		defaultspawnblocks.add(Material.GRASS);

		List<Biome> biomeTypes = new ArrayList<Biome>();
		biomeTypes.add(Biome.MOUNTAINS);
		biomeTypes.add(Biome.SNOWY_MOUNTAINS);
		biomeTypes.add(Biome.DESERT_HILLS);
		biomeTypes.add(Biome.WOODED_HILLS);
		biomeTypes.add(Biome.JUNGLE_HILLS);
		biomeTypes.add(Biome.TAIGA_HILLS);

		World world = Gods.get().getServer().getWorld(worldName);

		do
		{
			run++;

			int minLevel = 80;
			int x;
			int z;
			int y;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
				y = world.getHighestBlockYAt(x, z) - 1;

				if (getWorldGuard() != null)
				{
					Location location = new Location(world, x, y, z);
					com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
					
					RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
					RegionQuery query = container.createQuery();
					
					canBuild = query.getApplicableRegions(loc).size() == 0;
				}
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist) || (y < minLevel) || !canBuild);

			target = world.getBlockAt(x, y, z);

			if ((biomeTypes.contains(target.getBiome())) && (world.getBlockAt(x, y + 1, z).getType() == Material.AIR))
			{
				// target = world.getBlockAt(x, y - 1, z);

				if ((target.getBiome() == Biome.MOUNTAINS) && (defaultspawnblocks.contains(target.getType())))
				{
					int flatBlocks = 0;
					if ((defaultspawnblocks.contains(target.getRelative(BlockFace.NORTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
					{
						flatBlocks++;
					}
					if ((defaultspawnblocks.contains(target.getRelative(BlockFace.SOUTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
					{
						flatBlocks++;
					}
					if ((defaultspawnblocks.contains(target.getRelative(BlockFace.EAST).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
					{
						flatBlocks++;
					}
					if ((defaultspawnblocks.contains(target.getRelative(BlockFace.SOUTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
					{
						flatBlocks++;
					}

					if (flatBlocks < 4)
					{
						target = null;
					}
				}
			}
		}
		while (target == null && run < 1000);

		if (run >= 1000)
		{
			Gods.get().log("Pilgrimage Holy mountain generation FAILED in " + worldName);
			return null;
		}

		Gods.get().logDebug("Pilgrimage to Holy mountain in " + run + " runs: " + target.getLocation().getBlockX() + "," + target.getLocation().getBlockY() + "," + target.getLocation().getBlockZ());

		return target.getRelative(BlockFace.UP).getLocation();
	}

	public Location getLocationForSlayDragonQuest(String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;

		Block target = null;

		List<Material> defaultspawnblocks = new ArrayList<Material>();
		defaultspawnblocks.add(Material.GRASS);

		List<Biome> biomeTypes = new ArrayList<Biome>();
		biomeTypes.add(Biome.MOUNTAINS);
		biomeTypes.add(Biome.WOODED_MOUNTAINS);
		biomeTypes.add(Biome.GRAVELLY_MOUNTAINS);
		biomeTypes.add(Biome.MODIFIED_GRAVELLY_MOUNTAINS);

		World world = Gods.get().getServer().getWorld(worldName);
		do
		{
			run++;

			int minLevel = 80;
			int x;
			int z;
			int y;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
				y = world.getHighestBlockYAt(x, z);

				if (getWorldGuard() == null)
				{
					// getWorldGuard().getRegionManager(world).getApplicableRegions(new
					// Location(world, x, y, z)).size();
				}
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist) || (y < minLevel));
			target = world.getBlockAt(x, y, z);
			if ((biomeTypes.contains(target.getBiome())) && (world.getBlockAt(x, y + 1, z).getType() == Material.AIR))
			{
				int flatBlocks = 0;
				if ((defaultspawnblocks.contains(target.getRelative(BlockFace.NORTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
				{
					flatBlocks++;
				}
				if ((defaultspawnblocks.contains(target.getRelative(BlockFace.SOUTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
				{
					flatBlocks++;
				}
				if ((defaultspawnblocks.contains(target.getRelative(BlockFace.EAST).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
				{
					flatBlocks++;
				}
				if ((defaultspawnblocks.contains(target.getRelative(BlockFace.SOUTH).getType())) && (target.getRelative(BlockFace.NORTH).getRelative(BlockFace.UP).getType() == Material.AIR))
				{
					flatBlocks++;
				}
				if (flatBlocks < 4)
				{
					target = null;
				}
			}
		}
		while ((target == null) && (run < 1000));
		if (run >= 1000)
		{
			Gods.get().log("Pilgrimage Holy mountain generation FAILED in " + worldName);
			return null;
		}
		Gods.get().logDebug("Pilgrimage to Holy mountain in " + run + " runs: " + target.getLocation().getBlockX() + "," + target.getLocation().getBlockY() + "," + target.getLocation().getBlockZ());

		return target.getRelative(BlockFace.UP).getLocation();
	}

	Location getPositionForAncientTemple(String worldName, int minDist, int maxDist, Location center)
	{
		int run = 0;
		Block target = null;
		List<Material> defaultspawnblocks = new ArrayList<Material>();
		defaultspawnblocks.add(Material.END_PORTAL_FRAME);
		do
		{
			run++;

			int minLevel = 4;
			int maxLevel = 50;
			int x;
			int z;
			do
			{
				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
			}
			while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist));
			int y;
			do
			{
				y = this.random.nextInt(maxLevel);
			}
			while (

			y < minLevel);
			World world = Gods.get().getServer().getWorld(worldName);
			target = world.getBlockAt(x, y, z);
			if (target.getType() == Material.AIR)
			{
				target = world.getBlockAt(x, y - 1, z);
				if (defaultspawnblocks.contains(target.getType()))
				{
					target = world.getBlockAt(x, y, z);
				}
			}
		}
		while ((target != null) && (run < 1000));
		if (run >= 1000)
		{
			Gods.get().log("Ancient temple chest generation FAILED");
			return null;
		}
		Gods.get().logDebug("Ancient temple chest generated in " + run + " runs");

		return target.getLocation();
	}

	public int getQuestAmountForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".Amount");

		return value;
	}

	public Date getQuestCreatedTimeForGod(String godName)
	{
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		String startTime = this.questsConfig.getString(godName + ".CreatedTime");
		Date startTimeDate = null;
		try
		{
			startTimeDate = formatter.parse(startTime);
		}
		catch (Exception ex)
		{
			Gods.get().log("Invalid quest created date for " + godName + ". Reset.");
			startTimeDate = new Date();
			startTimeDate.setTime(0L);
		}
		return startTimeDate;
	}

	public Location getQuestLocation(String godName)
	{
		int x = 0;
		int y = 0;
		int z = 0;
		String worldName;

		try
		{
			worldName = this.questsConfig.getString(godName + ".Location.World");

			if (worldName == null)
			{
				return null;
			}

			x = this.questsConfig.getInt(godName + ".Location.X");
			y = this.questsConfig.getInt(godName + ".Location.Y");
			z = this.questsConfig.getInt(godName + ".Location.Z");
		}
		catch (Exception ex)
		{
			return null;
		}

		return new Location(Gods.get().getServer().getWorld(worldName), x, y, z);
	}

	public int getQuestMaxDurationForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".MaxDuration");

		return value;
	}

	public int getQuestProgressForGod(String godName)
	{
		int value = this.questsConfig.getInt(godName + ".Progress");

		return value;
	}

	String getQuestTargetGod(Location location)
	{
		int hash = hashVector(location);
		String godName;

		try
		{
			godName = this.questsConfig.getString("QuestTargets." + hash + ".God");
		}
		catch (Exception ex)
		{
			return null;
		}

		return godName;
	}

	String getQuestTargetLockedGodName(Location location)
	{
		String playerName = null;

		int hash = hashVector(location);
		try
		{
			playerName = this.questsConfig.getString("QuestTargets." + hash + ".LockedBy");
		}
		catch (Exception ex)
		{
			return null;
		}
		return playerName;
	}

	public String getQuestTargetTypeForGod(String godName)
	{
		String value = this.questsConfig.getString(godName + ".TargetType");

		return value;
	}

	public QUESTTYPE getQuestTypeForGod(String godName)
	{
		QUESTTYPE type = null;
		try
		{
			type = QUESTTYPE.valueOf(this.questsConfig.getString(godName + ".Type").toUpperCase());
		}
		catch (Exception ex)
		{
			Gods.get().logDebug("Could not recognize quest type '" + this.questsConfig.getString(new StringBuilder().append(godName).append(".Type").toString()) + "' for " + godName + ": " + ex.getMessage());
			type = QUESTTYPE.NONE;
		}
		return type;
	}

	public Set<Material> getRewardItems()
	{
		return this.rewardValues.keySet();
	}

	public List<ItemStack> getRewardsForQuestCompletion(int power)
	{
		List<ItemStack> rewards = new ArrayList<ItemStack>();
		while (power > 0)
		{
			int r = this.random.nextInt(this.rewardValues.size());

			int value = ((Integer) this.rewardValues.values().toArray()[r]).intValue();
			if ((value > 0) && (value <= power))
			{
				ItemStack items = new ItemStack((Material) this.rewardValues.keySet().toArray()[r], 1);
				rewards.add(items);
				power -= value;
			}
		}
		return rewards;
	}

	public List<ItemStack> getRewardsForQuestCompletion(String godName)
	{
		List<ItemStack> rewards = new ArrayList<ItemStack>();

		int power = (int) GodManager.get().getGodPower(godName);
		int t = 0;

		while (power > 0 && t++ < 100)
		{
			int r = this.random.nextInt(this.rewardValues.size());

			int value = (Integer) this.rewardValues.values().toArray()[r];

			if ((value > 0) && (value <= power))
			{
				ItemStack items = new ItemStack((Material) this.rewardValues.keySet().toArray()[r], 1);
				rewards.add(items);
				power -= value;
			}
		}

		return rewards;
	}

	public int getRewardValue(Material rewardItem)
	{
		return this.rewardValues.get(rewardItem).intValue();
	}

	public WorldGuardPlugin getWorldGuard()
	{
		Plugin worldGuardPlugin = Gods.get().getServer().getPluginManager().getPlugin("WorldGuard");

		if (Gods.get() == null || !(worldGuardPlugin instanceof WorldGuardPlugin))
		{
			return null;
		}

		return (WorldGuardPlugin) worldGuardPlugin;
	}

	public void godSayFailed(String godName)
	{
		int amount = getQuestAmountForGod(godName);
		int progress = getQuestProgressForGod(godName);
		String targetType = getQuestTargetTypeForGod(godName);

		LanguageManager.get().setAmount(amount - progress);
		try
		{
			LanguageManager.get().setType(targetType);
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		switch (questType)
		{
		case ITEMSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestFailed, 2 + this.random.nextInt(10));
			break;
		case SPREADLOVE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCrusadeQuestFailed, 2 + this.random.nextInt(10));
			break;
		case CRUSADE:
			break;
		case DELIVERITEM:
			break;
		case GIVEITEMS:
			break;
		case HOLYWAR:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestFailed, 2 + this.random.nextInt(100));
			break;
		case KILLBOSS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestFailed, 2 + this.random.nextInt(100));
			break;
		case GIVEROSE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestFailed, 2 + this.random.nextInt(100));
			break;
		case NONE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestFailed, 2 + this.random.nextInt(100));
			break;
		case CLAIMHOLYLAND:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestFailed, 2 + this.random.nextInt(100));
			break;
		case BUILDALTARS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestFailed, 2 + this.random.nextInt(100));
			break;
		case FIREWORKPARTY:
			break;
		case HUMANSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestFailed, 2 + this.random.nextInt(100));
			break;
		case GETHOLYARTIFACT:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestFailed, 2 + this.random.nextInt(100));
			break;
		case MOBSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestFailed, 2 + this.random.nextInt(100));
			break;
		case PILGRIMAGE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestFailed, 2 + this.random.nextInt(100));
			break;
		case SLAY:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestFailed, 2 + this.random.nextInt(100));
			break;
		case SLAYDRAGON:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayDragonQuestFailed, 2 + this.random.nextInt(100));
		default:
			break;
		}
	}

	public void GodSayNewGlobalQuest()
	{
		String questTargetType = getGlobalQuestTargetType();

		QUESTTYPE questType = getGlobalQuestType();
		for (String godName : GodManager.get().getOnlineGods())
		{
			try
			{
				switch (questType)
				{
				case PILGRIMAGE:
					LanguageManager.get().setType(questTargetType);

					LanguageManager.get().setPlayerName(questTargetType);

					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStarted, 2 + this.random.nextInt(100));

					break;
				case GETHOLYARTIFACT:
					LanguageManager.get().setType(questTargetType);

					LanguageManager.get().setPlayerName(questTargetType);

					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStarted, 2 + this.random.nextInt(100));

					break;
				case NONE:
					LanguageManager.get().setType(questTargetType);

					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStarted, 2 + this.random.nextInt(100));

					break;
				case MOBSACRIFICE:
					LanguageManager.get().setType(questTargetType);
					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestStarted, 2 + this.random.nextInt(100));
				default:
					break;
				}
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}
		}
	}

	private void godSayNewQuest(String godName)
	{
		int amount = getQuestAmountForGod(godName);
		String questTargetType = getQuestTargetTypeForGod(godName);

		QUESTTYPE questType = getQuestTypeForGod(godName);

		LanguageManager.get().setAmount(amount);
		try
		{
			switch (questType)
			{
			case SPREADLOVE:
				LanguageManager.get().setType(LanguageManager.get().getMobTypeName(EntityType.valueOf(questTargetType.toUpperCase())));
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCrusadeQuestStarted, 2 + this.random.nextInt(100));
				break;
			case GIVEITEMS:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBuildAltarsQuestStarted, 2 + this.random.nextInt(100));
				break;
			case GETHOLYARTIFACT:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStarted, 2 + this.random.nextInt(100));
				break;
			case DELIVERITEM:
			case NONE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStarted, 2 + this.random.nextInt(100));
				break;
			case KILLBOSS:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestStarted, 2 + this.random.nextInt(100));
				break;
			case CRUSADE:
				LanguageManager.get().setType(questTargetType);

				break;
			case HOLYWAR:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestStarted, 2 + this.random.nextInt(100));
				break;
			case GIVEROSE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestStarted, 2 + this.random.nextInt(100));
				break;
			case FIREWORKPARTY:
			case HUMANSACRIFICE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestStarted, 2 + this.random.nextInt(100));
				break;
			case MOBSACRIFICE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestStarted, 2 + this.random.nextInt(100));

				GodManager.get().sendInfoToBelievers(godName, LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
				break;
			case BUILDALTARS:
			case PILGRIMAGE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStarted, 2 + this.random.nextInt(100));

				GodManager.get().sendInfoToBelievers(godName, LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
				break;
			case ITEMSACRIFICE:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStarted, 2 + this.random.nextInt(100));
				break;
			case SLAY:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestStarted, 2 + this.random.nextInt(100));
				break;
			case SLAYDRAGON:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayDragonQuestStarted, 2 + this.random.nextInt(100));

				GodManager.get().sendInfoToBelievers(godName, LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 150);
				break;
			case CLAIMHOLYLAND:
				LanguageManager.get().setType(questTargetType);
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStarted, 2 + this.random.nextInt(100));

				GodManager.get().sendInfoToBelievers(godName, LanguageManager.LANGUAGESTRING.SacrificeHelp, ChatColor.AQUA, 150);
			default:
				break;
			}
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}
	}

	public void godSayProgress(String godName)
	{
		int amount = getQuestAmountForGod(godName);
		int progress = getQuestProgressForGod(godName);
		String targetType = getQuestTargetTypeForGod(godName);

		LanguageManager.get().setAmount(amount - progress);
		try
		{
			LanguageManager.get().setType(targetType);
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		switch (questType)
		{
		case ITEMSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestProgress, 2 + this.random.nextInt(10));
			break;
		case CRUSADE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCrusadeQuestProgress, 2 + this.random.nextInt(10));
			break;
		case SPREADLOVE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCrusadeQuestProgress, 2 + this.random.nextInt(10));
			break;
		case DELIVERITEM:
			break;
		case GIVEITEMS:
			break;
		case HOLYWAR:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestProgress, 2 + this.random.nextInt(100));
			break;
		case KILLBOSS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveBiblesQuestProgress, 2 + this.random.nextInt(100));
			break;
		case GIVEROSE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestProgress, 2 + this.random.nextInt(100));
			break;
		case NONE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestProgress, 2 + this.random.nextInt(100));
			break;
		case CLAIMHOLYLAND:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestProgress, 2 + this.random.nextInt(100));
			break;
		case BUILDALTARS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestProgress, 2 + this.random.nextInt(100));
			break;
		case FIREWORKPARTY:
			break;
		case HUMANSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestProgress, 2 + this.random.nextInt(100));
			break;
		case GETHOLYARTIFACT:
			break;
		case MOBSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestProgress, 2 + this.random.nextInt(100));
			break;
		case PILGRIMAGE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestProgress, 2 + this.random.nextInt(100));
			break;
		case SLAYDRAGON:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayDragonQuestProgress, 2 + this.random.nextInt(100));
			break;
		case SLAY:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestProgress, 2 + this.random.nextInt(100));
			break;
		default:
			break;
		}
	}

	public void godSayStatus(String godName)
	{
		int amount = getQuestAmountForGod(godName);
		int progress = getQuestProgressForGod(godName);
		String targetType = getQuestTargetTypeForGod(godName);

		LanguageManager.get().setAmount(amount - progress);
		try
		{
			LanguageManager.get().setType(targetType);
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		switch (questType)
		{
		case NONE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStatus, 2 + this.random.nextInt(100));
			break;
		case ITEMSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, 2 + this.random.nextInt(100));
			break;
		case SPREADLOVE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestStatus, 2 + this.random.nextInt(100));
			break;
		case GIVEITEMS:
			break;
		case DELIVERITEM:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, 2 + this.random.nextInt(100));
			break;
		case CRUSADE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, 2 + this.random.nextInt(100));
			break;
		case HOLYWAR:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestStatus, 2 + this.random.nextInt(100));
			break;

		case KILLBOSS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, 2 + this.random.nextInt(100));
			break;

		case GIVEROSE:
			if (this.random.nextInt(20) > 0)
			{
				return;
			}
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestStatus, 2 + this.random.nextInt(100));
			break;

		/*
		 * case PILGRIMAGE: { int delay = 2 + this.random.nextInt(100); if
		 * (this.random.nextInt(6) == 0) { try {
		 * LanguageManager.get().setType(targetType); } catch (Exception ex) {
		 * Gods.get().logDebug(ex.getStackTrace().toString()); }
		 * GodManager.get().godSayToBelievers(godName,
		 * LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus,
		 * delay); }
		 * 
		 * for (UUID believerId :
		 * BelieverManager.get().getBelieversForGod(godName)) { if
		 * (!BelieverManager.get().isHunting(believerId)) { if
		 * (this.random.nextInt(10) == 0) { Gods.get().sendInfo(believerId,
		 * LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0,
		 * "", 20); } } else { Location pilgrimageLocation =
		 * getQuestLocation(godName);
		 * 
		 * Player player = Gods.get().getServer().getPlayer(believerId);
		 * 
		 * if(player == null || player.isFlying()) { continue; }
		 * 
		 * if (pilgrimageLocation == null) { Gods.get().logDebug(
		 * "Quest target location is null for " + godName); return; }
		 * 
		 * if
		 * (!pilgrimageLocation.getWorld().getName().equals(player.getWorld().
		 * getName()) ) { Gods.get().logDebug("PilgrimageQuest for '" +
		 * player.getDisplayName() + "' is wrong world"); return; }
		 * 
		 * Vector vector =
		 * pilgrimageLocation.toVector().subtract(player.getLocation().toVector(
		 * ));
		 * 
		 * LanguageManager.get().setAmount((int) vector.length());
		 * Gods.get().sendInfo(believerId,
		 * LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA,
		 * (int) vector.length(), "", 20); } } } break;
		 */

		case SLAYDRAGON: {
			int delay = 2 + this.random.nextInt(100);

			if (this.random.nextInt(6) == 0)
			{
				try
				{
					LanguageManager.get().setType(targetType);
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayDragonQuestStatus, delay);
			}

			for (UUID believerId : BelieverManager.get().getBelieversForGod(godName))
			{
				if (!BelieverManager.get().isHunting(believerId))
				{
					if (this.random.nextInt(10) == 0)
					{
						Gods.get().sendInfo(believerId, LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0, "", 20);
					}
				}
				else
				{
					Location dragonLocation = getQuestLocation(godName);
					Player player = Gods.get().getServer().getPlayer(believerId);

					if (dragonLocation == null)
					{
						Gods.get().logDebug("Quest target location is null for " + godName);
						return;
					}
					if (player == null)
					{
						Gods.get().logDebug("DragonQuest player is null");
						return;
					}
					if (!dragonLocation.getWorld().getName().equals(player.getWorld().getName()))
					{
						Gods.get().logDebug("DragonQuest for '" + player.getDisplayName() + "' is wrong world");
						return;
					}
					Vector vector = dragonLocation.toVector().subtract(player.getLocation().toVector());

					LanguageManager.get().setAmount((int) vector.length());
					Gods.get().sendInfo(believerId, LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, 0, "", 0);
				}
			}
		}
			break;

		case CLAIMHOLYLAND:
			if (this.random.nextInt(20) > 0)
			{
				return;
			}
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, 2 + this.random.nextInt(100));
			break;
		case BUILDALTARS:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, 2 + this.random.nextInt(100));
			break;
		case FIREWORKPARTY:
		case HUMANSACRIFICE:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestStatus, 2 + this.random.nextInt(100));
			break;
		case GETHOLYARTIFACT:
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStatus, 2 + this.random.nextInt(100));
			break;
		case MOBSACRIFICE:
			if (this.random.nextInt(20) > 0)
			{
				return;
			}
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestStatus, 2 + this.random.nextInt(100));
			break;
		default:
			break;
		}
	}

	public void godsSayStatus()
	{
		String targetType = getGlobalQuestTargetType();

		try
		{
			LanguageManager.get().setType(targetType);
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		QUESTTYPE questType = getGlobalQuestType();

		for (String godName : GodManager.get().getOnlineGods())
		{
			switch (questType)
			{
			case ITEMSACRIFICE:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, 2 + this.random.nextInt(100));
				break;
			case SPREADLOVE:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCrusadeQuestStatus, 2 + this.random.nextInt(100));
				break;
			case GIVEITEMS:
				break;
			case DELIVERITEM:
				break;
			case CRUSADE:
				break;
			case HOLYWAR:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestStatus, 2 + this.random.nextInt(100));
				break;
			case KILLBOSS:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestStatus, 2 + this.random.nextInt(100));
				break;
			case GIVEROSE:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestStatus, 2 + this.random.nextInt(100));
				break;
			case NONE:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversClaimHolyLandQuestStatus, 2 + this.random.nextInt(100));
				break;

			case PILGRIMAGE: {
				int delay = 2 + this.random.nextInt(100);

				try
				{
					LanguageManager.get().setType(targetType);
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}

				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, delay);
				for (UUID believerId : BelieverManager.get().getBelieversForGod(godName))
				{
					if (!BelieverManager.get().isHunting(believerId))
					{
						if (this.random.nextInt(6) == 0)
						{
							Gods.get().sendInfo(believerId, LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA, 0, "", 20);
						}
					}
					else
					{
						Location pilgrimageLocation = getQuestLocation(godName);
						Player player = Gods.get().getServer().getPlayer(believerId);
						if (pilgrimageLocation == null)
						{
							Gods.get().logDebug("Quest target location is null for " + godName);
							return;
						}

						if (player == null)
						{
							Gods.get().logDebug("PilgrimageQuest player is null");
							return;
						}

						if (pilgrimageLocation.getWorld().getName().equals(player.getWorld().getName()))
						{
							Gods.get().logDebug("PilgrimageQuest for '" + player.getDisplayName() + "' is wrong world");
							return;
						}

						Vector vector = pilgrimageLocation.toVector().subtract(player.getLocation().toVector());

						Gods.get().sendInfo(believerId, LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA, (int) vector.length(), "", 20);
					}
				}
			}
				break;

			// case SLAYDRAGON:
			// int delay = 2 + this.random.nextInt(100);
			// try
			// {
			// LanguageManager.get().setType(targetType);
			// }
			// catch (Exception ex)
			// {
			// Gods.get().logDebug(ex.getStackTrace());
			// }
			// GodManager.get().godSayToBelievers(godName,
			// LanguageManager.LANGUAGESTRING.GodToBelieversSlayDragonQuestStatus,
			// delay);
			// for (String believerName :
			// BelieverManager.get().getBelieversForGod(godName))
			// {
			// if (!BelieverManager.get().isHunting(believerName))
			// {
			// if (this.random.nextInt(6) == 0)
			// {
			// Gods.get().sendInfo(believerName,
			// LanguageManager.LANGUAGESTRING.QuestTargetHelp, ChatColor.AQUA,
			// 0, "", 20);
			// }
			// }
			// else
			// {
			// Location dragonLocation = getQuestLocation(godName);
			// Player player = Gods.get().getServer().getPlayer(believerName);
			// if (dragonLocation == null)
			// {
			// Gods.get().logDebug("Quest target location is null for " +
			// godName);
			// return;
			// }
			// if (player == null)
			// {
			// Gods.get().logDebug("DragonQuest player '" + believerName + "' is
			// null");
			// return;
			// }
			// if
			// (dragonLocation.getWorld().getName().equals(player.getWorld().getName()))
			// {
			// Gods.get().logDebug("DragonQuest for '" + believerName + "' is
			// wrong
			// world");
			// return;
			// }
			// Vector vector =
			// dragonLocation.toVector().subtract(player.getLocation().toVector());
			//
			// Gods.get().sendInfo(believerName,
			// LanguageManager.LANGUAGESTRING.QuestTargetRange, ChatColor.AQUA,
			// (int)
			// vector.length(), "", 1);
			// }
			// }
			// break;
			case CLAIMHOLYLAND:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestStatus, 2 + this.random.nextInt(100));
				break;
			case BUILDALTARS:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestStatus, 2 + this.random.nextInt(100));
				break;
			case FIREWORKPARTY:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestStatus, 2 + this.random.nextInt(100));
				break;
			case MOBSACRIFICE:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolywarQuestStatus, 2 + this.random.nextInt(100));
				break;
			case SLAY:
				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestStatus, 2 + this.random.nextInt(100));
				break;
			case GETHOLYARTIFACT:
				int delay = 2 + this.random.nextInt(100);
				try
				{
					LanguageManager.get().setType(targetType);
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}

				GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestStatus, delay);

				for (UUID believerId : BelieverManager.get().getBelieversForGod(godName))
				{
					if (!BelieverManager.get().isHunting(believerId))
					{
						if (this.random.nextInt(6) == 0)
						{
							GodManager.get().godSayToBeliever(godName, believerId, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestHelp, delay + 20 + this.random.nextInt(100));
						}
					}
					else
					{
						Location artifactLocation = getGlobalQuestLocation();
						Player player = Gods.get().getServer().getPlayer(believerId);
						if (artifactLocation == null)
						{
							Gods.get().logDebug("GlobalArtifactQuest ArtifactLocation is null");
							return;
						}
						if (player == null)
						{
							Gods.get().logDebug("GlobalArtifactQuest player is null");
							return;
						}
						if (artifactLocation.getWorld().getName().equals(player.getWorld().getName()))
						{
							Gods.get().logDebug("GlobalArtifactQuest for '" + player.getDisplayName() + "' is wrong world");
							return;
						}
						Vector vector = artifactLocation.toVector().subtract(player.getLocation().toVector());

						LanguageManager.get().setAmount((int) vector.length());
						GodManager.get().godSayToBeliever(godName, believerId, LanguageManager.LANGUAGESTRING.GodToBelieversGetHolyArtifactQuestRange, delay + 20 + this.random.nextInt(100));
					}
				}
				break;
			default:
				break;
			}
		}
	}

	public void handleBibleMelee(String godName, UUID playerId)
	{
		if (!hasGlobalQuestType(QUESTTYPE.GETHOLYARTIFACT))
		{
			return;
		}
		Location pilgrimageLocation = getGlobalQuestLocation();

		String playerGod = BelieverManager.get().getGodForBeliever(playerId);
		if ((playerGod == null) || (pilgrimageLocation == null))
		{
			return;
		}
		GodManager.get().spawnGuidingMobs(godName, playerId, pilgrimageLocation);
	}

	public void handleBuiltPrayingAltar(String godName)
	{
		if (!hasQuest(godName))
		{
			return;
		}
		QUESTTYPE questType = getQuestTypeForGod(godName);

		boolean complete = false;
		if ((questType == null) || (questType != QUESTTYPE.BUILDALTARS))
		{
			return;
		}

		complete = addQuestProgressForGod(godName);

		if (complete)
		{
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBuildAltarsQuestCompleted, 2 + this.random.nextInt(10));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			int amount = getQuestAmountForGod(godName);
			int progress = getQuestProgressForGod(godName);

			LanguageManager.get().setAmount(amount - progress);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBuildAltarsQuestProgress, 2 + this.random.nextInt(10));
		}
	}

	public void handleEat(String playerName, String godName, String entityType)
	{
		if (!hasQuest(godName))
		{
			return;
		}
		QUESTTYPE questType = getQuestTypeForGod(godName);
		String questTargetType = getQuestTargetTypeForGod(godName);
		boolean complete = false;
		if (questType == null)
		{
			Gods.get().logDebug("handleEat(): null quest");
			return;
		}
		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			Gods.get().logDebug("handleEat(): null questType");
			return;
		}
		if (questType != QUESTTYPE.HOLYFEAST)
		{
			Gods.get().logDebug("handleEat(): quest is not feast");
			return;
		}
		Gods.get().logDebug("handling quest eating for " + godName);

		complete = addQuestProgressForGod(godName);
		if (complete)
		{
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversHolyFeastQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.get().addMoodForGod(godName, 2.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleJoinReligion(String playerName, String godName)
	{
		if (!hasQuest(godName))
		{
			return false;
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);
		getQuestTargetTypeForGod(godName);

		boolean complete = false;

		if ((questType == null) || (questType != QUESTTYPE.CONVERT))
		{
			return false;
		}

		complete = addQuestProgressForGod(godName);

		if (complete)
		{
			if (GodsConfiguration.get().isBiblesEnabled())
			{
				HolyBookManager.get().handleQuestCompleted(godName, questType, playerName);
			}
			LanguageManager.get().setPlayerName(playerName);
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.get().addMoodForGod(godName, 1.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
		return true;
	}

	public void handleKilledMob(String godName, String mobType)
	{
		if (!hasQuest(godName))
		{
			return;
		}
		QUESTTYPE questType = getQuestTypeForGod(godName);
		String questTargetType = getQuestTargetTypeForGod(godName);
		boolean complete = false;
		if ((questType == null) || (questType != QUESTTYPE.SLAY))
		{
			return;
		}
		if (questTargetType == null)
		{
			return;
		}
		if (!questTargetType.equalsIgnoreCase(mobType))
		{
			return;
		}
		complete = addQuestProgressForGod(godName);
		if (complete)
		{
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.get().addMoodForGod(godName, 2.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public void handleKilledPlayer(UUID playerId, String playerGod)
	{
		QUESTTYPE questType = getQuestTypeForGod(playerGod);
		if (questType == QUESTTYPE.PVPREVENGE)
		{
			hasQuest(playerGod);

			return;
		}
		if (!hasQuest(playerGod))
		{
		}
	}

	public boolean handleOpenChest(UUID playerId, Location blockLocation)
	{
		if (isQuestTarget(blockLocation))
		{
			String questGod = getQuestTargetGod(blockLocation);
			String godName = BelieverManager.get().getGodForBeliever(playerId);

			if (godName == null || !questGod.equals(godName))
			{
				return true;
			}

			try
			{
				LanguageManager.get().setType(getQuestTargetTypeForGod(questGod));
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			GodManager.get().godSayToBelievers(questGod, LanguageManager.LANGUAGESTRING.GodToBelieversPilgrimageQuestCompleted, 2);

			GodManager.get().addMoodForGod(godName, 2.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);
			BelieverManager.get().increasePrayerPower(playerId, 1);

			Gods.get().getServer().broadcastMessage(ChatColor.GOLD + Gods.get().getServer().getPlayer(playerId).getDisplayName() + ChatColor.AQUA + " completed a pilgrimage to " + ChatColor.GOLD + getQuestTargetTypeForGod(questGod));

			BelieverManager.get().setHunting(playerId, false);

			removeSuccessQuestForGod(godName);

			return false;
		}
		return false;
	}

	public void handlePickupItem(String playerName, Item item, Location location)
	{
	}

	public void handlePrayer(String godName, UUID playerId)
	{
		if (!hasQuest(godName))
		{
			return;
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		boolean complete = false;

		if (questType == null || questType != QUESTTYPE.CONVERT)
		{
			return;
		}

		String playerGod = BelieverManager.get().getGodForBeliever(playerId);

		if ((playerGod != null) && (playerGod.equals(godName)))
		{
			return;
		}

		complete = addQuestPlayerProgressForGod(godName, playerId);

		if (complete)
		{
			try
			{
				LanguageManager.get().setType(HolyBookManager.get().getBibleTitle(godName));
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			LanguageManager.get().setPlayerName(Gods.get().getServer().getPlayer(playerId).getDisplayName());
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversConvertQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.get().addMoodForGod(godName, 1.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handlePressurePlate(UUID playerId, Block block)
	{
		if (block == null)
		{
			return false;
		}

		if (hasGlobalQuestType(QUESTTYPE.CLAIMHOLYLAND))
		{
			if (isQuestTarget(block.getLocation()))
			{
				String godName = BelieverManager.get().getGodForBeliever(playerId);
				if (godName != null)
				{
					setDominationColor(block, GodManager.get().getColorForGod(godName));
				}
			}
		}
		return false;
	}

	public void handleReadBible(String godName, UUID playerId)
	{
		if (!hasQuest(godName))
		{
			return;
		}

		QUESTTYPE questType = getQuestTypeForGod(godName);

		boolean complete = false;

		if ((questType == null) || (questType != QUESTTYPE.GIVEITEMS))
		{
			return;
		}

		String playerGod = BelieverManager.get().getGodForBeliever(playerId);

		if (playerGod != null)
		{
			return;
		}

		complete = addQuestPlayerProgressForGod(godName, playerId);

		if (complete)
		{
			try
			{
				LanguageManager.get().setType(HolyBookManager.get().getBibleTitle(godName));
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			LanguageManager.get().setPlayerName(Gods.get().getServer().getPlayer(playerId).getDisplayName());
			GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversGiveItemsQuestCompleted, 2 + this.random.nextInt(10));

			GodManager.get().addMoodForGod(godName, 2.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}
	}

	public boolean handleSacrifice(Player player, String godName, String entityType)
	{
		if (!hasQuest(godName))
		{
			return false;
		}
		QUESTTYPE questType = getQuestTypeForGod(godName);
		String questTargetType = getQuestTargetTypeForGod(godName);

		boolean complete = false;

		if ((questType == null) || ((questType != QUESTTYPE.ITEMSACRIFICE) && (questType != QUESTTYPE.BURNBIBLES)))
		{
			return false;
		}

		if ((questTargetType == null) || (!questTargetType.equalsIgnoreCase(entityType)))
		{
			return false;
		}

		complete = addQuestProgressForGod(godName);
		BelieverManager.get().increasePrayerPower(player.getUniqueId(), 1);

		if (complete)
		{
			try
			{
				switch (questType)
				{
				case CLAIMHOLYLAND:
					LanguageManager.get().setType(LanguageManager.get().getItemTypeName(Material.getMaterial(questTargetType)));
					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestCompleted, 2 + this.random.nextInt(10));
					break;
				case ITEMSACRIFICE:
					LanguageManager.get().setType(LanguageManager.get().getItemTypeName(Material.getMaterial(questTargetType)));
					LanguageManager.get().setPlayerName(player.getDisplayName());
					GodManager.get().godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeQuestCompleted, 2 + this.random.nextInt(10));
				default:
					break;
				}
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			GodManager.get().addMoodForGod(godName, 2.0F * GodManager.get().getPleasedModifierForGod(godName));
			GodManager.get().addBeliefAndRewardBelievers(godName);

			removeSuccessQuestForGod(godName);
		}
		else
		{
			godSayProgress(godName);
		}

		return true;
	}

	boolean hasExpiredGlobalQuest()
	{
		Date thisDate = new Date();
		Date questStartTime = getGlobalQuestCreatedTime();
		int questMaxDuration = getGlobalQuestMaxDuration();

		long diff = thisDate.getTime() - questStartTime.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes > questMaxDuration;
	}

	boolean hasExpiredQuest(String godName)
	{
		Date thisDate = new Date();
		Date questStartTime = getQuestCreatedTimeForGod(godName);
		int questMaxDuration = getQuestMaxDurationForGod(godName);

		long diff = thisDate.getTime() - questStartTime.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes > questMaxDuration;
	}

	boolean hasGlobalQuest()
	{
		String quest = this.questsConfig.getString("Global.Type");

		return quest != null;
	}

	boolean hasGlobalQuestType(QUESTTYPE type)
	{
		QUESTTYPE currentType;
		try
		{
			String currentTypeString = this.questsConfig.getString("Global.Type");
			if (currentTypeString == null)
			{
				return false;
			}
			currentType = QUESTTYPE.valueOf(currentTypeString.toUpperCase());
		}
		catch (Exception ex)
		{
			Gods.get().logDebug("Could not recognize global quest type '" + this.questsConfig.getString("Global.Type") + "': " + ex.getMessage());
			currentType = QUESTTYPE.NONE;
		}
		return currentType == type;
	}

	private int hashVector(Location location)
	{
		return location.getBlockX() * 73856093 ^ location.getBlockY() * 19349663 ^ location.getBlockZ() * 83492791;
	}

	public boolean hasQuest(String godName)
	{
		String quest = this.questsConfig.getString(godName + ".Type");

		return quest != null;
	}

	boolean isQuestTarget(Location location)
	{
		int hash = hashVector(location);
		try
		{
			String questTargetLocationWorld = this.questsConfig.getString("QuestTargets." + hash + ".Location.World");

			if (questTargetLocationWorld == null)
			{
				return false;
			}

			int questTargetLocationX = this.questsConfig.getInt("QuestTargets." + hash + ".Location.X");
			int questTargetLocationY = this.questsConfig.getInt("QuestTargets." + hash + ".Location.Y");
			int questTargetLocationZ = this.questsConfig.getInt("QuestTargets." + hash + ".Location.Z");

			if (location.getBlockX() != questTargetLocationX)
			{
				return false;
			}

			if (location.getBlockZ() != questTargetLocationZ)
			{
				return false;
			}

			if (location.getBlockY() != questTargetLocationY)
			{
				return false;
			}

			if (!location.getWorld().getName().equals(questTargetLocationWorld))
			{
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public void load()
	{
		if (this.questsConfigFile == null)
		{
			this.questsConfigFile = new File(Gods.get().getDataFolder(), "quests.yml");
		}
		this.questsConfig = YamlConfiguration.loadConfiguration(this.questsConfigFile);

		Gods.get().log("Loaded " + this.questsConfig.getKeys(false).size() + " quests.");
	}

	public void removeFailedQuestForGod(String godName)
	{
		Location questLocation = getQuestLocation(godName);

		if (questLocation != null)
		{
			clearQuestTarget(questLocation);
		}

		switch (this.getQuestTypeForGod(godName))
		{
		case PILGRIMAGE: {
			Location location = this.getQuestLocation(godName);
			Block targetBlock = location.getWorld().getBlockAt(location);

			Block underBlock = targetBlock.getRelative(BlockFace.DOWN);
			underBlock.setType(Material.GRASS);

			Chest tb = (Chest) targetBlock.getState();
			Inventory contents = tb.getInventory();
			contents.clear();

			location.getWorld().getBlockAt(location).setType(Material.AIR);
		}
			break;
		default:
			break;
		}

		this.questsConfig.set(godName, null);

		save();
	}

	public void removeGlobalQuest()
	{
		this.questsConfig.set("Global", null);

		save();
	}

	public void removeSuccessQuestForGod(String godName)
	{
		Location questLocation = getQuestLocation(godName);

		if (questLocation != null)
		{
			clearQuestTarget(questLocation);
		}

		this.questsConfig.set(godName, null);

		save();
	}

	public void resetItemRewardValues()
	{
		this.rewardValues.put(Material.NETHER_WART, 33);
		this.rewardValues.put(Material.GLOWSTONE, 50);
		this.rewardValues.put(Material.COAL, 5);
		this.rewardValues.put(Material.IRON_INGOT, 10);
		this.rewardValues.put(Material.GOLD_INGOT, 50);
		this.rewardValues.put(Material.DIAMOND, 100);
		this.rewardValues.put(Material.PLAYER_HEAD, 75);
		this.rewardValues.put(Material.ENCHANTED_BOOK, 75);
		this.rewardValues.put(Material.POTION, 75);
		this.rewardValues.put(Material.COCOA, 30);
		this.rewardValues.put(Material.BLAZE_ROD, 50);
		this.rewardValues.put(Material.BOOK, 30);
		this.rewardValues.put(Material.COMPASS, 50);
		this.rewardValues.put(Material.PAPER, 20);
		this.rewardValues.put(Material.SUGAR_CANE, 25);
		this.rewardValues.put(Material.MELON_SEEDS, 5);
		this.rewardValues.put(Material.PUMPKIN_SEEDS, 5);
		this.rewardValues.put(Material.WHEAT_SEEDS, 1);
		this.rewardValues.put(Material.POPPY, 1);
		this.rewardValues.put(Material.DANDELION, 1);
	}

	public void save()
	{
		if ((this.questsConfig == null) || (this.questsConfigFile == null))
		{
			return;
		}
		try
		{
			this.questsConfig.save(this.questsConfigFile);
		}
		catch (Exception ex)
		{
			Gods.get().log("Could not save config to " + this.questsConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public void setDominationColor(Block block, ChatColor color)
	{
		if (color == null) {
			color = ChatColor.WHITE;
		}
		Material wool = Material.WHITE_WOOL;
		
		switch (color) {
			case WHITE:
				wool = Material.WHITE_WOOL;
				break;
			case DARK_GRAY:
				wool = Material.GRAY_WOOL;
				break;
			case RED:
				wool = Material.RED_WOOL;
				break;
			case DARK_GREEN:
				wool = Material.GREEN_WOOL;
				break;
			case DARK_RED:
				wool = Material.RED_WOOL;
				break;
			case BLUE:
				wool = Material.BLUE_WOOL;
				break;
			case GRAY:
				wool = Material.GRAY_WOOL;
				break;
			case GREEN:
				wool = Material.GREEN_WOOL;
				break;
			case YELLOW:
				wool = Material.YELLOW_WOOL;
				break;
			case DARK_BLUE:
				wool = Material.BLUE_WOOL;
				break;
			default:
				wool = Material.WHITE_WOOL;
		}
		
		if (wool == null) {
			wool = Material.WHITE_WOOL;
		}
		
		block.setType(wool);
		block.getRelative(BlockFace.UP).setType(Material.STONE_PRESSURE_PLATE);
		block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST).setType(wool);
		block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST).setType(wool);
		block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST).setType(wool);
		block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST).setType(wool);
		block.getRelative(BlockFace.NORTH).setType(Material.REDSTONE_LAMP);
		block.getRelative(BlockFace.EAST).setType(Material.REDSTONE_LAMP);
		block.getRelative(BlockFace.WEST).setType(Material.REDSTONE_LAMP);
		block.getRelative(BlockFace.SOUTH).setType(Material.REDSTONE_LAMP);
	}

	public void setItemRewardValue(Material item, int value)
	{
		this.rewardValues.put(item, Integer.valueOf(value));
	}

	void setQuestCompletedBy(Location location, String godName)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);

		int hash = hashVector(location);

		this.questsConfig.set("QuestTargets." + hash + ".LockedBy", godName);
		this.questsConfig.set("QuestTargets." + hash + ".LockedTime", formatter.format(new Date()));

		save();
	}

	void setQuestTarget(String godName, Location location)
	{
		int hash = hashVector(location);

		this.questsConfig.set("QuestTargets." + hash + ".God", godName);
		this.questsConfig.set("QuestTargets." + hash + ".Location.X", location.getBlockX());
		this.questsConfig.set("QuestTargets." + hash + ".Location.Y", location.getBlockY());
		this.questsConfig.set("QuestTargets." + hash + ".Location.Z", location.getBlockZ());
		this.questsConfig.set("QuestTargets." + hash + ".Location.World", location.getWorld().getName());
		// this.questsConfig.set("QuestTargets." + hash + ".CreatedTime",
		// formatter.format(thisDate));

		this.save();
	}

	public void setQuestTargetTypeForGod(String godName, String targetType)
	{
		this.questsConfig.set(godName + ".TargetType", targetType);

		save();
	}

	public void setQuestTypeForGod(String godName, int questType)
	{
		this.questsConfig.set(godName + ".Type", Integer.valueOf(questType));

		save();
	}

	private void setupPilgrimageQuest(Location targetLocation, String godName, GodManager.GodType godType)
	{
		Block targetBlock = Gods.get().getServer().getWorld(targetLocation.getWorld().getName()).getBlockAt(targetLocation);

		targetBlock.setType(Material.CHEST);

		Block underBlock = targetBlock.getRelative(BlockFace.DOWN);
		underBlock.setType(Material.GLOWSTONE);

		Inventory contents = null;
		Chest tb = (Chest) targetBlock.getState();
		contents = tb.getInventory();

		for (ItemStack rewardItem : getRewardsForQuestCompletion(200 + this.random.nextInt(150)))
		{
			contents.addItem(new ItemStack[] { rewardItem });
		}

		// Item artifactItem =
		// Gods.get().getHolyArtifactManager().createHolyArtifact(godType,
		// godName,
		// targetLocation);
		// contents.addItem(new ItemStack[] { artifactItem.getItemStack() });
	}
}