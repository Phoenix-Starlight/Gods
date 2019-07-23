package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.HolyLawManager.HolyLaw;
import com.dogonfire.gods.tasks.TaskGiveHolyArtifact;
import com.dogonfire.gods.tasks.TaskGiveItem;
import com.dogonfire.gods.tasks.TaskGodSpeak;
import com.dogonfire.gods.tasks.TaskHealPlayer;
import com.dogonfire.gods.tasks.TaskSpawnGuideMob;
import com.dogonfire.gods.tasks.TaskSpawnHostileMobs;

public class GodManager
{
	public static enum GodGender {
		None, Male, Female;
	}

	public static enum GodMood {
		EXALTED, PLEASED, NEUTRAL, DISPLEASED, ANGRY;
	}

	public static enum GodRelation {
		LOVERS, MARRIED, ENEMIES, FRIENDS, BFF, ROOMMATES;
	}

	public static enum GodType {
		FROST, LOVE, EVIL, SEA, MOON, SUN, THUNDER, PARTY, WAR, WEREWOLVES, CREATURES, WISDOM, NATURE;
	}

	public class NewPriestComparator implements Comparator<Object>
	{
		public NewPriestComparator()
		{
		}

		@Override
		public int compare(Object object1, Object object2)
		{
			GodManager.PriestCandidate c1 = (GodManager.PriestCandidate) object1;
			GodManager.PriestCandidate c2 = (GodManager.PriestCandidate) object2;

			float power1 = BelieverManager.get().getBelieverPower(c1.believerId);
			float power2 = BelieverManager.get().getBelieverPower(c2.believerId);

			return (int) (power2 - power1);
		}
	}

	public class PriestCandidate
	{
		public UUID believerId;

		PriestCandidate(UUID believerId)
		{
			this.believerId = believerId;
		}
	}

	private static GodManager instance;

	public static GodManager get()
	{
		if (instance == null)
			instance = new GodManager();
		return instance;
	}

	public static String parseBelief(String message)
	{
		return null;
	}

	private FileConfiguration	godsConfig		= null;

	private File				godsConfigFile	= null;

	private Random				random			= new Random();

	private List<String>		onlineGods		= new ArrayList<String>();

	private long				lastSaveTime;

	private String				pattern			= "HH:mm:ss dd-MM-yyyy";

	DateFormat					formatter		= new SimpleDateFormat(this.pattern);

	private GodManager()
	{
	}

	public boolean addAltar(Player player, String godName, Location location)
	{
		if (addBeliefByAltar(player, godName, location, true))
		{
			LanguageManager.get().setPlayerName(player.getName());

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverAltarBuilt, 2 + this.random.nextInt(30));

			return true;
		}

		return false;
	}

	private boolean addBelief(Player player, String godName, boolean allowChangeGod)
	{
		String oldGodName = BelieverManager.get().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.RED, 0, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (!allowChangeGod)
			{
				BelieverManager.get().setChangingGod(player.getUniqueId());

				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.ConfirmChangeToOtherReligion, ChatColor.YELLOW, 0, oldGodName, 1);
				return false;
			}

			if (BelieverManager.get().hasRecentGodChange(player.getUniqueId()))
			{
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotChangeGodSoSoon, ChatColor.RED, 0, "", 1);
				return false;
			}

			BelieverManager.get().clearChangingGod(player.getUniqueId());
		}

		if (!BelieverManager.get().addPrayer(player.getUniqueId(), godName))
		{
			int timeUntilCanPray = BelieverManager.get().getTimeUntilCanPray(player.getUniqueId());

			Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CannotPraySoSoon, ChatColor.RED, timeUntilCanPray, "", 1);
			return false;
		}

		if (oldGodName != null && !oldGodName.equals(godName))
		{
			if (isPriestForGod(player.getUniqueId(), oldGodName))
			{
				removePriest(oldGodName, player.getUniqueId());
			}

			LanguageManager.get().setPlayerName(player.getName());

			godSayToBelievers(oldGodName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

			Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.YouLeftReligion, ChatColor.RED, 0, oldGodName, 20);

			GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerJoinedReligion, player.getUniqueId());

			BelieverManager.get().clearPrayerPower(player.getUniqueId());
		}
		else
		{
			Material foodType = getHolyFoodTypeForGod(godName);

			try
			{
				LanguageManager.get().setType(LanguageManager.get().getItemTypeName(foodType));
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			giveItem(godName, player, foodType, false);

			BelieverManager.get().increasePrayerPower(player.getUniqueId(), 1);
		}

		if (oldGodName == null || !oldGodName.equals(godName))
		{
			if (GodsConfiguration.get().isMarriageEnabled())
			{
				MarriageManager.get().divorce(player.getUniqueId());
			}
			QuestManager.get().handleJoinReligion(player.getName(), godName);
		}

		return true;
	}

	public void addBeliefAndRewardBelievers(String godName)
	{
		for (UUID playerId : BelieverManager.get().getBelieversForGod(godName))
		{
			Player player = Gods.get().getServer().getPlayer(playerId);

			if (player == null)
			{
				continue;
			}

			BelieverManager.get().incPrayer(player.getUniqueId(), godName);

			List<ItemStack> rewards = QuestManager.get().getRewardsForQuestCompletion(godName);

			for (ItemStack items : rewards)
			{
				giveItem(godName, player, items.getType(), false);
			}
		}
	}

	private boolean addBeliefByAltar(Player player, String godName, Location prayerLocation, boolean allowChangeGod)
	{
		if (!godExist(godName))
		{
			if (!player.isOp() && (!PermissionsManager.get().hasPermission(player, "gods.god.create")))
			{
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CreateGodNotAllowed, ChatColor.RED, 0, "", 20);
				return false;
			}

			Block altarBlock = AltarManager.get().getAltarBlockFromSign(prayerLocation.getBlock());

			GodGender godGender = AltarManager.get().getGodGenderFromAltarBlock(altarBlock);

			Gods.get().logDebug("Altar is " + altarBlock.getType().name());

			GodType godType = AltarManager.get().getGodTypeForAltarBlockType(altarBlock.getType());

			Gods.get().logDebug("God divine force is " + godType);

			createGod(godName, player.getLocation(), godGender, godType);

			if (GodsConfiguration.get().isBroadcastNewGods())
			{
				Gods.get().getServer().broadcastMessage(ChatColor.WHITE + player.getName() + ChatColor.AQUA + " started to believe in the " + LanguageManager.get().getGodGenderName(getGenderForGod(godName)) + " " + ChatColor.GOLD + godName);
			}

			Gods.get().log(player.getName() + " created new god " + godName);
		}

		return addBelief(player, godName, allowChangeGod);
	}

	public void addMoodForGod(String godName, float mood)
	{
		float godMood = (float) this.godsConfig.getDouble(godName + ".Mood");

		godMood += mood;
		if (godMood > 100.0F)
		{
			godMood = 100.0F;
		}
		else if (godMood < -100.0F)
		{
			godMood = -100.0F;
		}
		this.godsConfig.set(godName + ".Mood", Float.valueOf(godMood));

		saveTimed();
	}

	public boolean assignPriest(String godName, UUID playerId)
	{
		this.godsConfig.set(godName + ".PendingPriest", null);
		BelieverManager.get().clearPendingPriest(playerId);

		Gods.get().getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageManager.get().getPriestAssignCommand(playerId));

		Set<UUID> believers = BelieverManager.get().getBelieversForGod(godName);
		if (believers.contains(playerId))
		{
			List<String> priests = this.godsConfig.getStringList(godName + ".Priests");

			if (priests.contains(playerId.toString()))
			{
				Gods.get().log(playerId.toString() + " is already a priest of " + godName);
			}
			else
			{
				priests.add(playerId.toString());
			}

			this.godsConfig.set(formatGodName(godName) + ".Priests", priests);

			this.godsConfig.set(godName + ".PendingPriest", null);
			this.godsConfig.set(godName + ".PendingPriestTime", null);

			BelieverManager.get().setLastPrayerDate(playerId);

			saveTimed();
			return true;
		}
		else
		{
			return false;
		}
	}

	public void believerAccept(UUID believerId)
	{
		String godName = BelieverManager.get().getGodForBeliever(believerId);

		Player player = Gods.get().getServer().getPlayer(believerId);
		if (player == null)
		{
			Gods.get().logDebug("believerAccept(): player is null for " + believerId);
			return;
		}

		LanguageManager.get().setPlayerName(player.getName());
		if (GodsConfiguration.get().isMarriageEnabled())
		{
			UUID pendingMarriagePartner = MarriageManager.get().getProposal(believerId);

			if (pendingMarriagePartner != null)
			{
				Gods.get().log(player.getName() + " accepted the proposal to marry " + pendingMarriagePartner);

				MarriageManager.get().handleAcceptProposal(believerId, pendingMarriagePartner, godName);

				return;
			}
		}

		HolyLaw pendingHolyLaw = HolyLawManager.get().getCurrentHolyLaw(godName);

		if (pendingHolyLaw != null)
		{
			HolyLawManager.get().acceptPendingLawQuestion(godName);
			
			return;
		}		
		
		String pendingGodInvitation = BelieverManager.get().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			Gods.get().logDebug("pendingGodInvitation is " + pendingGodInvitation);
			if (addBelief(player, pendingGodInvitation, true))
			{
				BelieverManager.get().clearInvitation(believerId);

				Gods.get().log(player.getName() + " accepted the invitation to join " + godName);

				GodSay(pendingGodInvitation, player, LanguageManager.LANGUAGESTRING.GodToPlayerAcceptedInvitation, 2 + this.random.nextInt(40));
				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNewPlayerAccepted, player.getUniqueId());
			}
			else
			{
				Gods.get().log(player.getName() + " could NOT accept the invitation to join " + godName);
			}
			return;
		}

		UUID pendingPriest = getPendingPriest(godName);

		if (pendingPriest != null)
		{
			if (pendingPriest == believerId)
			{
				assignPriest(godName, believerId);
				saveTimed();

				Gods.get().log(player.getName() + " accepted the offer from " + godName + " to be priest");

				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.InviteHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods invite <playername>", ChatColor.WHITE + "/gods invite <playername>", 100);
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.FollowersHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods followers", ChatColor.WHITE + "/gods followers", 200);
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.DescriptionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods desc", ChatColor.WHITE + "/gods desc", 300);

				if (GodsConfiguration.get().isHolyArtifactsEnabled())
				{
					Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AttackHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods startattack", ChatColor.WHITE + "/gods startattack", 300);
				}
				try
				{
					GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestPriestAccepted, 2 + this.random.nextInt(40));
					GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPriestAccepted, player.getUniqueId());
				}
				catch (Exception ex)
				{
					Gods.get().log("ERROR: Could not say GodToPriestPriestAccepted text! " + ex.getMessage());
				}
				return;
			}
		}

		Gods.get().logDebug(player.getDisplayName() + " did not have anything to accepted from " + godName);
		GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
	}

	public boolean believerLeaveGod(UUID believerId)
	{
		String godName = BelieverManager.get().getGodForBeliever(believerId);
		if (godName == null)
		{
			return false;
		}

		if (isPriestForGod(believerId, godName))
		{
			removePriest(godName, believerId);
		}
		BelieverManager.get().believerLeave(godName, believerId);

		LanguageManager.get().setPlayerName(Gods.get().getServer().getPlayer(believerId).getDisplayName());

		if (GodsConfiguration.get().isMarriageEnabled())
		{
			MarriageManager.get().divorce(believerId);
		}

		BelieverManager.get().clearPrayerPower(believerId);

		godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerLeftReligion, 2 + this.random.nextInt(20));

		return true;
	}

	public void believerReject(UUID believerId)
	{
		String godName = BelieverManager.get().getGodForBeliever(believerId);
		Player player = Gods.get().getServer().getPlayer(believerId);

		LanguageManager.get().setPlayerName(player.getName());

		String pendingGodInvitation = BelieverManager.get().getInvitation(believerId);
		if (pendingGodInvitation != null)
		{
			BelieverManager.get().clearInvitation(believerId);

			Gods.get().log(player.getName() + " rejected the invitation to join " + pendingGodInvitation);

			Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.RejectedJoinOffer, ChatColor.RED, 0, pendingGodInvitation, 20);

			return;
		}

		UUID pendingPriest = getPendingPriest(godName);

		if (pendingPriest == null)
		{
			if (player != null)
			{
				GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverNoQuestion, 2 + this.random.nextInt(20));
			}
			return;
		}

		if (getPendingPriest(godName).equals(believerId))
		{
			this.godsConfig.set(godName + ".PendingPriest", null);

			BelieverManager.get().clearPendingPriest(believerId);

			if (player != null)
			{
				Gods.get().log(player.getName() + " rejected the offer from " + godName + " to be priest");

				GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPriestRejected, 2 + this.random.nextInt(20));
			}
			saveTimed();
		}
	}

	public boolean blessPlayer(String godName, UUID playerId, float godPower)
	{
		Player player = Gods.get().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (BelieverManager.get().hasRecentBlessing(playerId))
		{
			return false;
		}

		int blessingType = 0;
		int t = 0;

		float blessingPower = 1.0F + godPower / 100.0F;

		do
		{
			blessingType = this.random.nextInt(5);
			t++;
		}
		while ((t < 50) && (((blessingType == 0) && (!GodsConfiguration.get().isFastDiggingBlessingEnabled())) || ((blessingType == 1) && (!GodsConfiguration.get().isHealBlessingEnabled())) || ((blessingType == 2) && (!GodsConfiguration.get().isRegenerationBlessingEnabled()))
				|| ((blessingType == 3) && (!GodsConfiguration.get().isSpeedBlessingEnabled())) || ((blessingType == 4) && (!GodsConfiguration.get().isIncreaseDamageBlessingEnabled()))));

		switch (blessingType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, (int) (300.0F * blessingPower), 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, (int) (300.0F * blessingPower), 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (300.0F * blessingPower), 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (300.0F * blessingPower), 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, (int) (300.0F * blessingPower), 1));
		}

		BelieverManager.get().setBlessingTime(player.getUniqueId());

		return true;
	}

	public void blessPlayerWithHolyArtifact(String godName, Player player)
	{
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		giveHolyArtifact(godName, getDivineForceForGod(godName), player, true);
	}

	public ItemStack blessPlayerWithItem(String godName, Player player)
	{
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return null;
		}
		ItemStack item = getItemNeed(godName, player);
		if (item != null)
		{
			giveItem(godName, player, item.getType(), true);
		}
		return item;
	}

	public void clearContestedHolyLandForGod(String godName)
	{
		new SimpleDateFormat(this.pattern);
		new Date();

		this.godsConfig.set(godName + ".ContestedLand", null);

		saveTimed();
	}

	public void createGod(String godName, Location location, GodGender godGender, GodType godType)
	{
		Date thisDate = new Date();

		DateFormat formatter = new SimpleDateFormat(this.pattern);

		setHomeForGod(godName, location);
		setGenderForGod(godName, godGender);
		setDivineForceForGod(godName, godType);
		setPrivateAccess(godName, GodsConfiguration.get().isDefaultPrivateReligions());

		this.godsConfig.set(godName + ".Created", formatter.format(thisDate));

		saveTimed();
	}

	public boolean cursePlayer(String godName, UUID playerId, float godPower)
	{
		Player player = Gods.get().getServer().getPlayer(playerId);

		if (player == null)
		{
			return false;
		}

		if (BelieverManager.get().hasRecentCursing(playerId))
		{
			return false;
		}

		int curseType = 0;
		int t = 0;

		do
		{
			curseType = this.random.nextInt(7);
			t++;
		}
		while ((t < 50) && (((curseType == 5) && (!GodsConfiguration.get().isLightningCurseEnabled())) || ((curseType == 6) && (!GodsConfiguration.get().isMobCurseEnabled()))));

		float cursePower = 1.0F + godPower / 100.0F;

		switch (curseType)
		{
		case 0:
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (200.0F * cursePower), 1));
			break;
		case 1:
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (200.0F * cursePower), 1));
			break;
		case 2:
			player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (200.0F * cursePower), 1));
			break;
		case 3:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (200.0F * cursePower), 1));
			break;
		case 4:
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, (int) (200.0F * cursePower), 1));
			break;
		case 5:
			strikePlayerWithLightning(playerId, 1);
			break;
		case 6:
			strikePlayerWithMobs(godName, playerId, godPower);
		}

		BelieverManager.get().setCursingTime(player.getUniqueId());

		return true;
	}

	public String formatGodName(String godName)
	{
		return godName.substring(0, 1).toUpperCase() + godName.substring(1).toLowerCase();
	}

	private String generateHolyMobTypeForGod()
	{
		EntityType mobType = EntityType.UNKNOWN;
		int r1 = this.random.nextInt(7);
		switch (r1)
		{
		case 0:
			mobType = EntityType.CHICKEN;
			break;
		case 1:
			mobType = EntityType.COW;
			break;
		case 2:
			mobType = EntityType.PIG;
			break;
		case 3:
			mobType = EntityType.SHEEP;
			break;
		case 4:
			mobType = EntityType.OCELOT;
			break;
		case 5:
			mobType = EntityType.WOLF;
			break;
		case 6:
			mobType = EntityType.MUSHROOM_COW;
		}
		return mobType.name();
	}

	private String generateUnholyMobTypeForGod()
	{
		EntityType mobType = EntityType.UNKNOWN;
		int r1 = this.random.nextInt(11);
		switch (r1)
		{
		case 0:
			mobType = EntityType.CHICKEN;
			break;
		case 1:
			mobType = EntityType.COW;
			break;
		case 2:
			mobType = EntityType.ENDERMAN;
			break;
		case 3:
			mobType = EntityType.PIG;
			break;
		case 4:
			mobType = EntityType.SHEEP;
			break;
		case 5:
			mobType = EntityType.OCELOT;
			break;
		case 6:
			mobType = EntityType.WOLF;
			break;
		case 7:
			mobType = EntityType.SQUID;
			break;
		case 8:
			mobType = EntityType.SPIDER;
			break;
		case 9:
			mobType = EntityType.SKELETON;
			break;
		case 10:
			mobType = EntityType.ZOMBIE;
		}
		return mobType.name();
	}

	public Set<String> getAllGods()
	{
		Set<String> gods = this.godsConfig.getKeys(false);

		return gods;
	}

	public List<String> getAllianceRelations(String godName)
	{
		return this.godsConfig.getStringList(godName + ".Allies");
	}

	public float getAngryModifierForGod(String godName)
	{
		return -1.0F;
	}

	private Material getAxeBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND_AXE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.IRON_AXE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.STONE_AXE;
		}
		return Material.WOODEN_AXE;
	}

	public String getBlessedPlayerForGod(String godName)
	{
		Date lastBlessedDate = getLastBlessedTimeForGod(godName);
		if (lastBlessedDate == null)
		{
			return null;
		}
		Date thisDate = new Date();

		long diff = thisDate.getTime() - lastBlessedDate.getTime();
		long diffSeconds = diff / 1000L;
		if (diffSeconds > GodsConfiguration.get().getMaxBlessingTime())
		{
			this.godsConfig.set(godName + ".BlessedPlayer", null);
			this.godsConfig.set(godName + ".BlessedTime", null);
			saveTimed();

			return null;
		}
		return this.godsConfig.getString(godName + ".BlessedPlayer");
	}

	public ChatColor getColorForGod(String godName)
	{
		GodType godType = getDivineForceForGod(godName);

		return getColorForGodType(godType);
	}

	public ChatColor getColorForGodType(GodType godType)
	{
		ChatColor color = ChatColor.WHITE;
		if (godType == null)
		{
			return ChatColor.WHITE;
		}
		switch (godType)
		{
		case THUNDER:
			color = ChatColor.DARK_GRAY;
			break;
		case EVIL:
			color = ChatColor.RED;
			break;
		case WISDOM:
			color = ChatColor.DARK_GREEN;
			break;
		case FROST:
			color = ChatColor.BLACK;
			break;
		case SUN:
			color = ChatColor.DARK_RED;
			break;
		case SEA:
			color = ChatColor.BOLD;
			break;
		case LOVE:
			color = ChatColor.BLUE;
			break;
		case MOON:
			color = ChatColor.GRAY;
			break;
		case WAR:
			color = ChatColor.GREEN;
			break;
		case NATURE:
			color = ChatColor.YELLOW;
			break;
		case PARTY:
			color = ChatColor.LIGHT_PURPLE;
			break;
		case CREATURES:
			color = ChatColor.DARK_BLUE;
			break;
		case WEREWOLVES:
			color = ChatColor.GRAY;
		default:
			return color;
		}
		return color;
	}

	public Location getContestedHolyLandAttackLocationForGod(String godName)
	{
		Long.valueOf(this.godsConfig.getLong(godName + ".ContestedLand"));

		int x = this.godsConfig.getInt(godName + ".ContestedLand" + ".X");
		int y = this.godsConfig.getInt(godName + ".ContestedLand" + ".Y");
		int z = this.godsConfig.getInt(godName + ".ContestedLand" + ".Z");
		String worldName = this.godsConfig.getString(godName + ".ContestedLand" + ".World");

		return new Location(Gods.get().getServer().getWorld(worldName), x, y, z);
	}

	public Long getContestedHolyLandForGod(String godName)
	{
		new SimpleDateFormat(this.pattern);
		new Date();
		Long contestedLand = Long.valueOf(this.godsConfig.getLong(godName + ".ContestedLand.Hash"));
		if (contestedLand.longValue() == 0L)
		{
			return null;
		}
		return contestedLand;
	}

	public int getContestedHolyLandKillsForGod(String godName, int n)
	{
		getContestedHolyLandForGod(godName);

		int kills = this.godsConfig.getInt(godName + ".ContestedKills");

		return kills;
	}

	public Player getCursedPlayerForGod(String godName)
	{
		Date lastCursedDate = getLastCursingTimeForGod(godName);
		if (lastCursedDate == null)
		{
			return null;
		}
		Date thisDate = new Date();

		long diff = thisDate.getTime() - lastCursedDate.getTime();
		long diffMinutes = diff / 60000L;
		if (diffMinutes > GodsConfiguration.get().getMaxCursingTime())
		{
			this.godsConfig.set(godName + ".CursedPlayer", null);
			this.godsConfig.set(godName + ".CursedTime", null);
			saveTimed();

			return null;
		}

		return Gods.get().getServer().getPlayer(this.godsConfig.getString(godName + ".CursedPlayer"));
	}

	public GodType getDivineForceForGod(String godName)
	{
		GodType type = GodType.FROST;
		try
		{
			type = GodType.valueOf(this.godsConfig.getString(godName + ".DivineForce"));
		}
		catch (Exception ex)
		{
			Gods.get().log("Could not parse GodType " + this.godsConfig.getString(new StringBuilder(String.valueOf(godName)).append(".DivineForce").toString()) + " for the god '" + godName + "'. Assigning a random GodType.");
			do
			{
				type = GodType.values()[this.random.nextInt(GodType.values().length)];
			}
			while (type == GodType.WEREWOLVES);
			setDivineForceForGod(godName, type);
		}
		return type;
	}

	public Material getHolyFoodTypeForGod(String godName)
	{
		String foodTypeString = this.godsConfig.getString(godName + ".EatFoodType");
		Material foodType = Material.AIR;
		if (foodTypeString == null)
		{
			int r1 = this.random.nextInt(8);
			switch (r1)
			{
			case 0:
				foodType = Material.APPLE;
				break;
			case 1:
				foodType = Material.BREAD;
				break;
			case 2:
				foodType = Material.COOKED_SALMON;
				break;
			case 3:
				foodType = Material.COOKED_COD;
				break;
			case 4:
				foodType = Material.MELON;
				break;
			case 5:
				foodType = Material.COOKED_BEEF;
				break;
			case 6:
				foodType = Material.COOKED_PORKCHOP;
				break;
			case 7:
				foodType = Material.CARROT;
			}
			foodTypeString = foodType.name();

			this.godsConfig.set(godName + ".EatFoodType", foodTypeString);

			saveTimed();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		return foodType;
	}

	public List<String> getEnemyGodsForGod(String godName)
	{
		return this.godsConfig.getStringList(godName + ".War");
	}

	public String getEnemyPlayerForGod(String godName)
	{
		List<String> enemyGods = getEnemyGodsForGod(godName);
		if (enemyGods.size() == 0)
		{
			return null;
		}
		int g = 0;
		do
		{
			String enemyGod = enemyGods.get(enemyGods.size());
			if (enemyGod != null)
			{
				Set<UUID> believers = BelieverManager.get().getBelieversForGod(enemyGod);

				int b = 0;
				while (b < 10)
				{
					int r = this.random.nextInt(believers.size());

					String believerName = (String) believers.toArray()[r];
					if (Gods.get().getServer().getPlayer(believerName) != null)
					{
						return believerName;
					}
					b++;
				}
			}
			g++;
		}
		while (

		g < 50);
		return null;
	}

	public float getExactMoodForGod(String godName)
	{
		return (float) this.godsConfig.getDouble(godName + ".Mood");
	}

	public float getFalloffModifierForGod(String godName)
	{
		Random moodRandom = new Random(getSeedForGod(godName));

		float baseFalloff = (1 + moodRandom.nextInt(40)) / 20.0F;

		double falloffValue = -GodsConfiguration.get().getMoodFalloff() * (1.0F + baseFalloff * BelieverManager.get().getOnlineBelieversForGod(godName).size()) * (1.0D + Math.sin(System.currentTimeMillis() / 1500000.0F));

		Gods.get().logDebug(godName + " mood falloff is " + falloffValue);

		return (float) falloffValue;
	}

	private Material getFoodBlessing(String godName)
	{
		return getHolyFoodTypeForGod(godName);
	}

	public GodGender getGenderForGod(String godName)
	{
		String genderString = this.godsConfig.getString(godName + ".Gender");
		GodGender godGender = GodGender.None;

		if (genderString != null)
		{
			try
			{
				godGender = GodGender.valueOf(genderString);
			}
			catch (Exception ex)
			{
				godGender = GodGender.None;
			}
		}

		return godGender;
	}

	public String getGodDescription(String godName)
	{
		String description = this.godsConfig.getString(godName + ".Description");
		if (description == null)
		{
			description = new String("No description :/");
		}
		return description;
	}

	public int getGodLevel(String godName)
	{
		float power = getGodPower(godName);
		if (power < 3.0F)
		{
			return 0;
		}
		if (power < 10.0F)
		{
			return 1;
		}
		return 2;
	}

	public boolean getGodMobDamage(String godName)
	{
		return (GodsConfiguration.get().isHolyLandDefaultMobDamage()) || (this.godsConfig.getBoolean(godName + ".MobDamage"));
	}

	public boolean getGodMobSpawning(String godName)
	{
		return this.godsConfig.getBoolean(godName + ".MobSpawning");
	}

	public float getGodPower(String godName)
	{
		float godPower = 0.0F;
		int minGodPower = 0;

		String name = this.godsConfig.getString(godName);

		if (name == null)
		{
			return 0.0F;
		}

		Set<UUID> believers = BelieverManager.get().getBelieversForGod(godName);

		if (GodsConfiguration.get().isUseWhitelist())
		{
			minGodPower = (int) WhitelistManager.get().getMinGodPower(godName);
		}

		for (UUID believerId : believers)
		{
			float believerPower = BelieverManager.get().getBelieverPower(believerId);

			godPower += believerPower;
		}
		if (godPower < minGodPower)
		{
			godPower = minGodPower;
		}
		return godPower;
	}

	public boolean getGodPvP(String godName)
	{
		return (GodsConfiguration.get().isHolyLandDefaultPvP()) || (this.godsConfig.getBoolean(godName + ".PvP"));
	}

	public int getHealthBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return 3;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return 2;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return 1;
		}
		return 0;
	}

	public double getHealthNeed(String godName, Player player)
	{
		return player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - player.getHealth();
	}

	private Material getHoeBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND_HOE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.IRON_HOE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.STONE_HOE;
		}
		return Material.WOODEN_HOE;
	}

	public EntityType getHolyMobTypeForGod(String godName)
	{
		String mobTypeString = this.godsConfig.getString(godName + ".NotSlayMobType");
		EntityType mobType = EntityType.UNKNOWN;
		if (mobTypeString == null)
		{
			do
			{
				mobTypeString = generateHolyMobTypeForGod();
			}
			while (mobTypeString.equals(getUnholyMobTypeForGod(godName).name()));
			this.godsConfig.set(godName + ".NotSlayMobType", mobTypeString);

			saveTimed();
		}
		mobType = Enum.valueOf(EntityType.class, mobTypeString);
		if (mobType == null)
		{
			do
			{
				mobTypeString = generateHolyMobTypeForGod();
			}
			while (mobTypeString.equals(getUnholyMobTypeForGod(godName).name()));
			this.godsConfig.set(godName + ".NotSlayMobType", mobTypeString);

			save();

			mobType = EntityType.valueOf(mobTypeString.toUpperCase());
		}
		return mobType;
	}

	public Location getHomeForGod(String godName)
	{
		Location location = new Location(null, 0.0D, 0.0D, 0.0D);

		String worldName = this.godsConfig.getString(godName + ".Home.World");
		if (worldName == null)
		{
			return null;
		}
		location.setWorld(Gods.get().getServer().getWorld(worldName));

		location.setX(this.godsConfig.getDouble(godName + ".Home.X"));
		location.setY(this.godsConfig.getDouble(godName + ".Home.Y"));
		location.setZ(this.godsConfig.getDouble(godName + ".Home.Z"));

		return location;
	}

	public List<UUID> getInvitedPlayerForGod(String godName)
	{
		List<String> players = this.godsConfig.getStringList(godName + ".InvitedPlayers");

		if (players.size() == 0)
		{
			return null;
		}

		List<UUID> invitedPlayers = new ArrayList<UUID>();

		for (String playerId : players)
		{
			invitedPlayers.add(UUID.fromString(playerId));
		}

		return invitedPlayers;
	}

	private ItemStack getItemNeed(String godName, Player player)
	{
		if (!hasFood(player, godName))
		{
			return new ItemStack(getFoodBlessing(godName));
		}
		if (!hasPickAxe(player))
		{
			return new ItemStack(getPickAxeBlessing(godName));
		}
		if (!hasSword(player))
		{
			return new ItemStack(getSwordBlessing(godName));
		}
		if (!hasSpade(player))
		{
			return new ItemStack(getSpadeBlessing(godName));
		}
		if (!hasAxe(player))
		{
			return new ItemStack(getAxeBlessing(godName));
		}
		if (!hasHoe(player))
		{
			return new ItemStack(getHoeBlessing(godName));
		}
		return null;
	}

	public String getLanguageFileForGod(String godName)
	{
		String languageFileName = this.godsConfig.getString(godName + ".LanguageFileName");

		if (languageFileName == null)
		{
			GodType godType = GodManager.get().getDivineForceForGod(godName);
			if (godType == null)
			{
				godType = GodType.values()[this.random.nextInt(GodType.values().length)];
				GodManager.get().setDivineForceForGod(godName, godType);

				Gods.get().logDebug("getLanguageFileForGod: Could not find a type for " + godName + ", so setting his type to " + godType.name());
			}

			GodGender godGender = GodManager.get().getGenderForGod(godName);

			if (godGender == GodGender.None)
			{
				Gods.get().logDebug("getLanguageFileForGod: Could not find a gender for " + godName + ", so setting his type to " + godGender.name());

				switch (random.nextInt(2))
				{
				case 0:
					godGender = GodGender.Male;
					break;
				case 1:
					godGender = GodGender.Female;
					break;
				}
			}

			languageFileName = GodsConfiguration.get().getLanguageIdentifier() + "_" + godType.name().toLowerCase() + "_" + godGender.name().toLowerCase() + ".yml";

			Gods.get().log("getLanguageFileForGod: Setting language file " + languageFileName);

			this.godsConfig.set(godName + ".LanguageFileName", languageFileName);

			saveTimed();
		}

		return languageFileName;
	}

	public Date getLastBlessedTimeForGod(String godName)
	{
		String lastBlessedString = this.godsConfig.getString(godName + ".BlessedTime");
		if (lastBlessedString == null)
		{
			return null;
		}
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date lastBlessedDate = null;
		try
		{
			lastBlessedDate = formatter.parse(lastBlessedString);
		}
		catch (Exception ex)
		{
			lastBlessedDate = new Date();
			lastBlessedDate.setTime(0L);
		}
		return lastBlessedDate;
	}

	public Date getLastCursingTimeForGod(String godName)
	{
		String lastCursedString = this.godsConfig.getString(godName + ".CursedTime");
		if (lastCursedString == null)
		{
			return null;
		}
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date lastCursedDate = null;
		try
		{
			lastCursedDate = formatter.parse(lastCursedString);
		}
		catch (Exception ex)
		{
			lastCursedDate = new Date();
			lastCursedDate.setTime(0L);
		}
		return lastCursedDate;
	}

	public long getMinutesSinceLastQuest(String godName)
	{
		Date thisDate = new Date();
		Date questDate = null;
		this.godsConfig.set(godName + ".LastQuestTime", formatter.format(thisDate));

		String lastQuestDateString = this.godsConfig.getString(godName + ".LastQuestTime");
		try
		{
			questDate = formatter.parse(lastQuestDateString);
		}
		catch (Exception ex)
		{
			questDate = new Date();
			questDate.setTime(0L);
		}

		long diff = thisDate.getTime() - questDate.getTime();

		return diff / 60000;
	}

	public GodMood getMoodForGod(String godName)
	{
		float godMood = (float) this.godsConfig.getDouble(godName + ".Mood");
		if (godMood < -70.0F)
		{
			return GodMood.ANGRY;
		}
		if (godMood < -20.0F)
		{
			return GodMood.DISPLEASED;
		}
		if (godMood < 20.0F)
		{
			return GodMood.NEUTRAL;
		}
		if (godMood < 70.0F)
		{
			return GodMood.PLEASED;
		}
		return GodMood.EXALTED;
	}

	private UUID getNextBelieverForPriest(String godName)
	{
		Set<UUID> allBelievers = BelieverManager.get().getBelieversForGod(godName);

		List<PriestCandidate> candidates = new ArrayList<PriestCandidate>();

		if (allBelievers == null || allBelievers.size() == 0)
		{
			Gods.get().logDebug("Did not find any priest candidates");
			return null;
		}

		UUID pendingPriest = getPendingPriest(godName);

		for (UUID candidate : allBelievers)
		{
			Player player = Gods.get().getServer().getPlayer(candidate);
			if (player != null)
			{
				if (!isPriest(candidate))
				{
					if ((pendingPriest == null) || (!pendingPriest.equals(candidate)))
					{
						if (!BelieverManager.get().hasRecentPriestOffer(candidate))
						{
							if (PermissionsManager.get().hasPermission(player, "gods.priest"))
							{
								candidates.add(new PriestCandidate(candidate));
							}
						}
					}
				}
			}
		}

		if (candidates.size() == 0)
		{
			return null;
		}

		Collections.sort(candidates, new NewPriestComparator());

		PriestCandidate finalCandidate = null;
		if (candidates.size() > 2)
		{
			finalCandidate = (PriestCandidate) candidates.toArray()[this.random.nextInt(3)];
		}
		else
		{
			finalCandidate = (PriestCandidate) candidates.toArray()[0];
		}

		return finalCandidate.believerId;
	}

	public Material getUnholyFoodTypeForGod(String godName)
	{
		String foodTypeString = this.godsConfig.getString(godName + ".NotEatFoodType");
		Material foodType = Material.AIR;
		if (foodTypeString == null)
		{
			do
			{
				int r1 = this.random.nextInt(8);
				switch (r1)
				{
				case 0:
					foodType = Material.APPLE;
					break;
				case 1:
					foodType = Material.BREAD;
					break;
				case 2:
					foodType = Material.COOKED_SALMON;
					break;
				case 3:
					foodType = Material.COOKED_COD;
					break;
				case 4:
					foodType = Material.MELON;
					break;
				case 5:
					foodType = Material.COOKED_BEEF;
					break;
				case 6:
					foodType = Material.COOKED_PORKCHOP;
					break;
				case 7:
					foodType = Material.CARROT;
				}
				foodTypeString = foodType.name();
			}
			while (foodTypeString.equals(getHolyFoodTypeForGod(godName).name()));
			this.godsConfig.set(godName + ".NotEatFoodType", foodTypeString);

			saveTimed();
		}
		else
		{
			foodType = Material.getMaterial(foodTypeString);
		}
		return foodType;
	}

	public List<String> getOfflineGods()
	{
		Set<String> allGods = this.godsConfig.getKeys(false);
		List<String> offlineGods = new ArrayList<String>();
		for (String godName : allGods)
		{
			if (!this.onlineGods.contains(godName))
			{
				offlineGods.add(godName);
			}
		}
		return offlineGods;
	}

	public List<String> getOnlineGods()
	{
		return this.onlineGods;
	}

	public UUID getPendingPriest(String godName)
	{
		String believer = this.godsConfig.getString(godName + ".PendingPriest");

		if ((believer == null) || (believer.equals("none")))
		{
			return null;
		}

		Player player = Gods.get().getServer().getPlayer(UUID.fromString(believer));

		if (player == null)
		{
			return null;
		}

		return player.getUniqueId();
	}

	private Material getPickAxeBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND_PICKAXE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.IRON_PICKAXE;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.STONE_PICKAXE;
		}
		return Material.WOODEN_PICKAXE;
	}

	public float getPleasedModifierForGod(String godName)
	{
		Random moodRandom = new Random(getSeedForGod(godName));

		return 5 + moodRandom.nextInt(10);
	}

	public List<UUID> getPriestsForGod(String godName)
	{
		List<String> names = this.godsConfig.getStringList(godName + ".Priests");
		List<UUID> list = new ArrayList<UUID>();

		if (names == null || names.isEmpty())
		{
			Gods.get().logDebug("No priests for " + godName);
			return list;
		}

		for (String name : names)
		{
			if (name != null && !name.equals("none"))
			{
				Date thisDate = new Date();
				Date lastPrayerDate = BelieverManager.get().getLastPrayerTime(UUID.fromString(name));

				UUID believerId = UUID.fromString(name);

				long diff = thisDate.getTime() - lastPrayerDate.getTime();

				long diffHours = diff / 3600000L;
				if (diffHours > GodsConfiguration.get().getMaxPriestPrayerTime())
				{
					LanguageManager.get().setPlayerName(name);
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversRemovedPriest, 2 + this.random.nextInt(40));
					removePriest(godName, believerId);
				}
				else
				{
					list.add(believerId);
				}
			}
		}
		return list;
	}

	public String getQuestType(String godName)
	{
		String name = this.godsConfig.getString(godName + ".QuestType");
		if ((name == null) || (name.equals("none")))
		{
			return null;
		}
		return name;
	}

	private Material getRewardBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.GOLD_INGOT;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.CAKE;
		}
		return Material.COAL;
	}

	public Material getSacrificeItemTypeForGod(String godName)
	{
		String itemName = "";
		Integer value = Integer.valueOf(0);
		String sacrificeItemName = null;

		ConfigurationSection configSection = this.godsConfig.getConfigurationSection(godName + ".SacrificeValues");
		if ((configSection == null) || (configSection.getKeys(false).size() == 0))
		{
			return null;
		}
		for (int i = 0; i < configSection.getKeys(false).size(); i++)
		{
			itemName = (String) configSection.getKeys(false).toArray()[this.random.nextInt(configSection.getKeys(false).size())];

			value = Integer.valueOf(this.godsConfig.getInt(godName + ".SacrificeValues." + itemName));
			if (value.intValue() > 10)
			{
				sacrificeItemName = itemName;
			}
		}
		if (sacrificeItemName != null)
		{
			return Material.getMaterial(sacrificeItemName);
		}
		return null;
	}

	private Material getSacrificeNeedForGod(String godName)
	{
		Random materialRandom = new Random(getSeedForGod(godName));
		List<Integer> materials = new ArrayList<Integer>();

		for (int n = 0; n < 5; n++)
		{
			materials.add(materialRandom.nextInt(24));
		}

		int typeIndex = 0;
		Material type = Material.AIR;

		do
		{
			typeIndex = materials.get(this.random.nextInt(materials.size())).intValue();

			switch (typeIndex)
			{
			case 0:
				type = Material.POPPY;
				break;
			case 1:
				type = Material.OAK_LEAVES;
				break;
			case 2:
				type = getUnholyFoodTypeForGod(godName);
				break;
			case 3:
				type = Material.RABBIT_HIDE;
				break;
			case 4:
				type = Material.RABBIT_FOOT;
				break;
			case 5:
				type = Material.CACTUS;
				break;
			case 6:
				type = Material.BREAD;
				break;
			case 7:
				type = Material.CARROT;
				break;
			case 8:
				type = Material.IRON_PICKAXE;
				break;
			case 9:
				type = Material.IRON_INGOT;
				break;
			case 10:
				type = Material.GOLD_INGOT;
				break;
			case 11:
				type = Material.APPLE;
				break;
			case 12:
				type = Material.BOOK;
				break;
			case 13:
				type = Material.CAKE;
				break;
			case 14:
				type = Material.MELON;
				break;
			case 15:
				type = Material.COOKIE;
				break;
			case 16:
				type = Material.PUMPKIN;
				break;
			case 17:
				type = Material.SUGAR_CANE;
				break;
			case 18:
				type = Material.EGG;
				break;
			case 19:
				type = Material.WHEAT;
				break;
			case 20:
				type = Material.SPIDER_EYE;
				break;
			case 21:
				type = Material.POTATO;
				break;
			case 22:
				type = Material.BONE;
				break;
			case 23:
				type = Material.FEATHER;
			}
		}
		while (type == getHolyFoodTypeForGod(godName) || type == Material.AIR);

		return type;
	}

	private Material getSacrificeUnwantedForGod(String godName)
	{
		List<Material> unwantedItems = new ArrayList<Material>();
		ConfigurationSection configSection = this.godsConfig.getConfigurationSection(godName + ".SacrificeValues.");
		if (configSection != null)
		{
			for (String itemType : configSection.getKeys(false))
			{
				Material item = null;
				try
				{
					item = Material.valueOf(itemType);
				}
				catch (Exception ex)
				{
					continue;
				}
				if (this.godsConfig.getDouble(godName + ".SacrificeValues." + itemType) <= 0.0D)
				{
					unwantedItems.add(item);
				}
			}
		}
		else
		{
			return null;
		}
		if (unwantedItems.size() == 0)
		{
			return null;
		}
		return unwantedItems.get(this.random.nextInt(unwantedItems.size()));
	}

	private float getSacrificeValueForGod(String godName, Material type)
	{
		return (float) this.godsConfig.getDouble(godName + ".SacrificeValues." + type.name());
	}

	public long getSeedForGod(String godName)
	{
		long seed = this.godsConfig.getLong(godName + ".Seed");
		if (seed == 0L)
		{
			seed = this.random.nextLong();
			this.godsConfig.set(godName + ".Seed", Long.valueOf(seed));

			saveTimed();
		}
		return seed;
	}

	private Material getSpadeBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND_SHOVEL;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.IRON_SHOVEL;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.STONE_SHOVEL;
		}
		return Material.WOODEN_SHOVEL;
	}

	private Material getSwordBlessing(String godName)
	{
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel3Items())
		{
			return Material.DIAMOND_SWORD;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel2Items())
		{
			return Material.IRON_SWORD;
		}
		if (getGodPower(godName) > GodsConfiguration.get().getGodPowerForLevel1Items())
		{
			return Material.STONE_SWORD;
		}
		return Material.WOODEN_SWORD;
	}

	public String getTitleForGod(String godName)
	{
		if (!GodsConfiguration.get().isUseGodTitles())
		{
			return "";
		}
		GodType godType = GodManager.get().getDivineForceForGod(godName);
		if (godType == null)
		{
			return "";
		}
		return LanguageManager.get().getGodTypeName(godType, LanguageManager.get().getGodGenderName(GodManager.get().getGenderForGod(godName)));
	}

	public Set<String> getTopGods()
	{
		Set<String> topGods = this.godsConfig.getKeys(false);

		return topGods;
	}

	public EntityType getUnholyMobTypeForGod(String godName)
	{
		String mobTypeString = this.godsConfig.getString(godName + ".SlayMobType");
		EntityType mobType = EntityType.UNKNOWN;
		if (mobTypeString == null)
		{
			mobTypeString = generateUnholyMobTypeForGod();

			this.godsConfig.set(godName + ".SlayMobType", mobTypeString);

			saveTimed();
		}
		mobType = Enum.valueOf(EntityType.class, mobTypeString);
		if (mobType == null)
		{
			mobTypeString = generateUnholyMobTypeForGod();

			this.godsConfig.set(godName + ".SlayMobType", mobTypeString);

			save();

			mobType = EntityType.valueOf(mobTypeString.toUpperCase());
		}
		return mobType;
	}

	private int getVerbosityForGod(String godName)
	{
		int verbosity = this.godsConfig.getInt(godName + ".Verbosity");
		if (verbosity == 0)
		{
			verbosity = 1 + this.random.nextInt(50);

			this.godsConfig.set(godName + ".Verbosity", Integer.valueOf(verbosity));

			save();
		}
		Random moodRandom = new Random(getSeedForGod(godName));

		double variation = 1.0D + 1.0D * Math.sin(moodRandom.nextFloat() + System.currentTimeMillis() / 3600000.0F);

		double godVerbosity = getGodPower(godName) / 100.0F + verbosity;

		return (int) (1.0D + variation * (GodsConfiguration.get().getGodVerbosity() * godVerbosity));
	}

	public List<String> getWarRelations(String godName)
	{
		return this.godsConfig.getStringList(godName + ".Enemies");
	}

	public void giveHolyArtifact(String godName, GodType godType, Player player, boolean speak)
	{
		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskGiveHolyArtifact(godName, godType, player, speak), 2L);
	}

	public void giveItem(String godName, Player player, Material material, boolean speak)
	{
		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskGiveItem(godName, player, material, speak), 2L);
	}

	public boolean godExist(String godName)
	{
		String name = this.godsConfig.getString(formatGodName(godName) + ".Created");
		if (name == null)
		{
			return false;
		}
		return true;
	}

	public void GodSay(String godName, Player player, LanguageManager.LANGUAGESTRING message, int delay)
	{
		String playerNameString = LanguageManager.get().getPlayerName();
		String typeNameString = LanguageManager.get().getType();
		int amount = LanguageManager.get().getAmount();

		if (player == null)
		{
			Gods.get().logDebug("GodSay(): Player is null!");
			return;
		}

		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		Gods.get().logDebug(godName + " to " + player.getName() + ": " + LanguageManager.get().getLanguageString(godName, message));

		if (!PermissionsManager.get().hasPermission(player, "gods.listen"))
		{
			return;
		}

		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskGodSpeak(godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);
	}

	private boolean godSayNeededSacrificeToBeliever(String godName, UUID believerId)
	{
		if (GodsConfiguration.get().isSacrificesEnabled())
		{
			Material itemType = getSacrificeItemTypeForGod(godName);
			if (itemType != null)
			{
				String itemName = LanguageManager.get().getItemTypeName(itemType);
				try
				{
					LanguageManager.get().setType(itemName);
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}

				godSayToBeliever(godName, believerId, LanguageManager.LANGUAGESTRING.GodToBelieversSacrificeItemType);

				return true;
			}
		}
		return false;
	}

	public void godSayToBeliever(String godName, UUID playerId, LanguageManager.LANGUAGESTRING message)
	{
		godSayToBeliever(godName, playerId, message, 2 + this.random.nextInt(10));
	}

	public void godSayToBeliever(String godName, UUID playerId, LanguageManager.LANGUAGESTRING message, int delay)
	{
		Player player = Gods.get().getServer().getPlayer(playerId);

		if (player == null)
		{
			Gods.get().logDebug("GodSayToBeliever player is null");
			return;
		}
		GodSay(godName, player, message, delay);
	}

	public void godSayToBelievers(String godName, LanguageManager.LANGUAGESTRING message, int delay)
	{
		for (UUID playerId : BelieverManager.get().getBelieversForGod(godName))
		{
			Player player = Gods.get().getServer().getPlayer(playerId);
			if (player != null)
			{
				GodSay(godName, player, message, delay);
			}
		}
	}

	public void GodSayToBelieversExcept(String godName, LanguageManager.LANGUAGESTRING message, UUID exceptPlayer)
	{
		for (UUID playerId : BelieverManager.get().getBelieversForGod(godName))
		{
			Player player = Gods.get().getServer().getPlayer(playerId);

			if (player != null && player.getUniqueId() != exceptPlayer)
			{
				GodSay(godName, player, message, 2 + this.random.nextInt(20));
			}
		}
	}

	public void GodSayToPriest(String godName, LanguageManager.LANGUAGESTRING message)
	{
		List<UUID> priests = getPriestsForGod(godName);
		if (priests == null)
		{
			return;
		}

		for (UUID priest : priests)
		{
			Player player = Gods.get().getServer().getPlayer(priest);
			if (player != null)
			{
				GodSay(godName, player, message, 2 + this.random.nextInt(30));
			}
		}
	}

	public void GodSayWithQuestion(String godName, Player player, LanguageManager.LANGUAGESTRING message, int delay)
	{
		String playerNameString = LanguageManager.get().getPlayerName();
		String typeNameString = LanguageManager.get().getType();
		int amount = LanguageManager.get().getAmount();

		if (player == null)
		{
			Gods.get().logDebug("GodSay(): Player is null!");
			return;
		}
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		Gods.get().logDebug(godName + " to " + player.getName() + ": " + LanguageManager.get().getLanguageString(godName, message));
		if (!PermissionsManager.get().hasPermission(player, "gods.listen"))
		{
			return;
		}

		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskGodSpeak(godName, player.getUniqueId(), playerNameString, typeNameString, amount, message), delay);

		Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverQuestionHelp, ChatColor.AQUA, ChatColor.WHITE + "/gods yes or /gods no", ChatColor.WHITE + "/gods yes or /gods no", delay + 80);
	}

	public void GodsSayToBelievers(LanguageManager.LANGUAGESTRING message, int delay)
	{
		for (String godName : getOnlineGods())
		{
			godSayToBelievers(godName, message, delay);
		}
	}

	public boolean handleAltarPray(Location location, Player player, String godName)
	{
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}

		if (addBeliefByAltar(player, godName, location, BelieverManager.get().getChangingGod(player.getUniqueId())))
		{
			Block altarBlock = AltarManager.get().getAltarBlockFromSign(player.getWorld().getBlockAt(location));

			if (GodManager.get().getGenderForGod(godName) == GodGender.None)
			{
				GodGender godGender = AltarManager.get().getGodGenderFromAltarBlock(altarBlock);

				Gods.get().logDebug("God did not have a gender, setting gender to " + godGender);

				GodManager.get().setGenderForGod(godName, godGender);
			}
			if (GodManager.get().getDivineForceForGod(godName) == null)
			{
				GodType godType = AltarManager.get().getGodTypeForAltarBlockType(altarBlock.getType());

				Gods.get().logDebug("God did not have a divine force, setting divine force to " + godType);

				GodManager.get().setDivineForceForGod(godName, godType);
			}

			addMoodForGod(godName, getPleasedModifierForGod(godName));

			if ((GodsConfiguration.get().isHolyLandEnabled()) && (PermissionsManager.get().hasPermission(player, "gods.holyland")))
			{
				HolyLandManager.get().setPrayingHotspot(player.getName(), godName, altarBlock.getLocation());
			}

			QuestManager.get().handlePrayer(godName, player.getUniqueId());

			LanguageManager.get().setPlayerName(player.getName());

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPraying, 2 + this.random.nextInt(10));
			location.getWorld().playEffect(location, Effect.MOBSPAWNER_FLAMES, 25);

			return true;
		}

		return false;
	}

	public void handleBibleMelee(String godName, Player player)
	{
	}

	public void handleEat(Player player, String godName, String foodType)
	{
		Material eatFoodType = getHolyFoodTypeForGod(godName);
		Material notEatFoodType = getUnholyFoodTypeForGod(godName);

		if (foodType.equals(eatFoodType.name()))
		{
			addMoodForGod(godName, getPleasedModifierForGod(godName));

			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				try
				{
					LanguageManager.get().setType(LanguageManager.get().getItemTypeName(eatFoodType));
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}
				LanguageManager.get().setPlayerName(player.getDisplayName());
				if (GodsConfiguration.get().isCommandmentsBroadcastFoodEaten())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversEatFoodBlessing);
				}
			}
		}

		if (foodType.equals(notEatFoodType.name()))
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			if (cursePlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				try
				{
					LanguageManager.get().setType(LanguageManager.get().getItemTypeName(notEatFoodType));
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}

				LanguageManager.get().setPlayerName(player.getDisplayName().toUpperCase());

				if (GodsConfiguration.get().isCommandmentsBroadcastFoodEaten())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotEatFoodCursing);
				}
			}
		}
	}

	public void handleKilled(Player player, String godName, String mobType)
	{
		if ((!GodsConfiguration.get().isCommandmentsEnabled()) || (mobType == null))
		{
			return;
		}
		EntityType holyMobType = getHolyMobTypeForGod(godName);
		EntityType unholyMobType = getUnholyMobTypeForGod(godName);
		if ((unholyMobType != null) && (mobType.equals(unholyMobType.name())))
		{
			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				addMoodForGod(godName, getPleasedModifierForGod(godName));

				LanguageManager.get().setPlayerName(player.getDisplayName());
				try
				{
					LanguageManager.get().setType(LanguageManager.get().getMobTypeName(unholyMobType));
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}
				if (GodsConfiguration.get().isCommandmentsBroadcastMobSlain())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing, 2 + this.random.nextInt(20));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversSlayMobBlessing);
				}
			}
		}

		if ((holyMobType != null) && (mobType.equals(holyMobType.name())))
		{
			if (cursePlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				addMoodForGod(godName, getAngryModifierForGod(godName));

				LanguageManager.get().setPlayerName(player.getDisplayName().toUpperCase());
				try
				{
					LanguageManager.get().setType(LanguageManager.get().getMobTypeName(holyMobType));
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}
				if (GodsConfiguration.get().isCommandmentsBroadcastMobSlain())
				{
					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing, 2 + this.random.nextInt(10));
				}
				else
				{
					godSayToBeliever(godName, player.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieversNotSlayMobCursing);
				}
			}
		}
	}

	public void handleKilledPlayer(UUID playerId, String godName, GodType godType)
	{
		if (godType == null)
		{
			return;
		}
		if (GodsConfiguration.get().isLeaveReligionOnDeath())
		{
			BelieverManager.get().believerLeave(godName, playerId);
		}
	}

	public boolean handlePray(Player player, String godName)
	{
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return false;
		}

		if (addBelief(player, godName, BelieverManager.get().getChangingGod(player.getUniqueId())))
		{
			addMoodForGod(godName, getPleasedModifierForGod(godName));

			LanguageManager.get().setPlayerName(player.getName());

			GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverPraying, 2 + this.random.nextInt(10));

			player.getLocation().getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 25);

			return true;
		}
		return false;
	}

	public void handleReadBible(String godName, Player player)
	{
	}

	public void handleSacrifice(String godName, Player believer, Material type)
	{
		if (believer == null)
		{
			return;
		}

		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return;
		}

		if (godName == null)
		{
			return;
		}

		int godPower = (int) GodManager.get().getGodPower(godName);

		Gods.get().log(believer.getDisplayName() + " sacrificed " + type.name() + " to " + godName);

		Material eatFoodType = getHolyFoodTypeForGod(godName);

		if (type == eatFoodType)
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			cursePlayer(godName, believer.getUniqueId(), getGodPower(godName));

			try
			{
				LanguageManager.get().setType(LanguageManager.get().getItemTypeName(eatFoodType));
			}
			catch (Exception ex)
			{
				Gods.get().logDebug(ex.getStackTrace().toString());
			}

			LanguageManager.get().setPlayerName(believer.getDisplayName());

			if (GodsConfiguration.get().isCommandmentsBroadcastFoodEaten())
			{
				godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice, 2 + this.random.nextInt(10));
			}
			else
			{
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverHolyFoodSacrifice);
			}

			strikePlayerWithLightning(believer.getUniqueId(), 1 + this.random.nextInt(3));

			return;
		}

		float value = getSacrificeValueForGod(godName, type);

		LanguageManager.get().setPlayerName(believer.getDisplayName());

		try
		{
			LanguageManager.get().setType(LanguageManager.get().getItemTypeName(type));
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		if (value > 10.0F)
		{
			addMoodForGod(godName, getPleasedModifierForGod(godName));
			BelieverManager.get().addPrayer(believer.getUniqueId(), godName);

			blessPlayer(godName, believer.getUniqueId(), godPower);
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverGoodSacrifice);

			BelieverManager.get().increasePrayerPower(believer.getUniqueId(), 1);
		}
		else if (value >= -5.0F)
		{
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMehSacrifice);
		}
		else
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));
			strikePlayerWithLightning(believer.getUniqueId(), 1 + this.random.nextInt(3));
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverBadSacrifice);
		}

		value -= 1.0F;

		this.godsConfig.set(godName + ".SacrificeValues." + type.name(), Float.valueOf(value));

		saveTimed();
	}

	public boolean hasAllianceRelation(String godName, String otherGodName)
	{
		return this.godsConfig.contains(godName + ".Allies" + otherGodName);
	}

	private boolean hasAxe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_AXE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_AXE))
		{
			return true;
		}
		return false;
	}

	private boolean hasFood(Player player, String godName)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(GodManager.get().getHolyFoodTypeForGod(godName)))
		{
			return true;
		}
		return false;
	}

	public boolean hasGodAccess(UUID believerId, String godName)
	{
		if (!isPrivateAccess(godName))
		{
			return true;
		}

		String currentGodName = BelieverManager.get().getGodForBeliever(believerId);

		if ((currentGodName == null) || (!currentGodName.equals(godName)))
		{
			return false;
		}
		return true;
	}

	private boolean hasHoe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_HOE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_HOE))
		{
			return true;
		}
		return false;
	}

	private boolean hasPickAxe(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_PICKAXE))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_PICKAXE))
		{
			return true;
		}
		return false;
	}

	private boolean hasSpade(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		if (inventory.contains(Material.WOODEN_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.STONE_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.IRON_SHOVEL))
		{
			return true;
		}
		if (inventory.contains(Material.DIAMOND_SHOVEL))
		{
			return true;
		}
		return false;
	}

	private boolean hasSword(Player player)
	{
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getSize(); i++)
		{
			ItemStack stack = inventory.getItem(i);
			if ((stack != null) && ((stack.getType().equals(Material.WOODEN_SWORD)) || (stack.getType().equals(Material.STONE_SWORD)) || (stack.getType().equals(Material.IRON_SWORD)) || (stack.getType().equals(Material.DIAMOND_SWORD))) && (stack.getAmount() != 0))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasWarRelation(String godName, String otherGodName)
	{
		return this.godsConfig.contains(godName + ".Enemies" + otherGodName);
	}

	public void healPlayer(String godName, Player player, double healing)
	{
		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskHealPlayer(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverHealthBlessing), 2L);
	}

	public boolean increaseContestedHolyLandKillsForGod(String godName, int n)
	{
		new SimpleDateFormat(this.pattern);
		new Date();

		getContestedHolyLandForGod(godName);

		int kills = this.godsConfig.getInt(godName + ".ContestedKills");

		this.godsConfig.set(godName + ".ContestedKills", Integer.valueOf(kills + n));

		saveTimed();

		return kills + n > 10;
	}

	public boolean isDeadGod(String godName)
	{
		if ((BelieverManager.get().getBelieversForGod(godName).size() == 0) && (GodManager.get().getGodPower(godName) < 1.0F))
		{
			removeGod(godName);

			return true;
		}
		return false;
	}

	public boolean isPriest(UUID believerId)
	{
		if (believerId == null)
		{
			return false;
		}

		Set<String> gods = getAllGods();

		for (String godName : gods)
		{
			List<UUID> list = getPriestsForGod(godName);

			if (list != null && list.contains(believerId))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isPriestForGod(UUID believerId, String godName)
	{
		if (believerId == null)
		{
			return false;
		}

		List<UUID> priests = getPriestsForGod(godName);

		if (priests != null && priests.contains(believerId))
		{
			return true;
		}
		return false;
	}

	public boolean isPrivateAccess(String godName)
	{
		Boolean access = Boolean.valueOf(this.godsConfig.getBoolean(godName + ".PrivateAccess"));
		if (access != null)
		{
			return access.booleanValue();
		}
		return false;
	}

	public void load()
	{
		this.godsConfigFile = new File(Gods.get().getDataFolder(), "gods.yml");

		this.godsConfig = YamlConfiguration.loadConfiguration(this.godsConfigFile);

		Gods.get().log("Loaded " + this.godsConfig.getKeys(false).size() + " gods.");
		for (String godName : this.godsConfig.getKeys(false))
		{
			String priestName = this.godsConfig.getString(godName + ".PriestName");
			if (priestName != null)
			{
				List<String> list = new ArrayList<String>();
				list.add(priestName);

				this.godsConfig.set("PriestName", null);
				this.godsConfig.set(godName + ".Priests", list);

				save();
			}
		}
	}

	private boolean manageBelieverForAngryGod(String godName, Player believer)
	{
		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		int godPower = 1 + (int) GodManager.get().getGodPower(godName);

		if (this.random.nextInt(1 + 1000 / godPower) == 0)
		{
			if (BelieverManager.get().hasRecentPrayer(believer.getUniqueId()))
			{
				return false;
			}

			if (cursePlayer(godName, believer.getUniqueId(), godPower))
			{
				LanguageManager.get().setPlayerName(believer.getDisplayName());

				GodSay(godName, believer, LanguageManager.LANGUAGESTRING.GodToBelieverCursedAngry, 2 + this.random.nextInt(10));

				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((BelieverManager.get().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomAngrySpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForDispleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}
		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((BelieverManager.get().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomDispleasedSpeech);
			return true;
		}
		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForExaltedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if ((believer.getGameMode() != GameMode.CREATIVE) && PermissionsManager.get().hasPermission(believer, "gods.itemblessings"))
		{
			if (!BelieverManager.get().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (GodsConfiguration.get().isItemBlessingEnabled())
				{
					float power = getGodPower(godName);

					if (power >= GodsConfiguration.get().getMinGodPowerForItemBlessings() && this.random.nextInt((int) (1.0F + 50.0F / power)) == 0)
					{
						double healing = getHealthNeed(godName, believer);

						if ((healing > 1.0D) && (this.random.nextInt(3) == 0))
						{
							healPlayer(godName, believer, getHealthBlessing(godName));

							BelieverManager.get().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							LanguageManager.get().setPlayerName(believer.getDisplayName());
							try
							{
								LanguageManager.get().setType(LanguageManager.get().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								Gods.get().logDebug(ex.getStackTrace().toString());
							}

							BelieverManager.get().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}

			if (GodsConfiguration.get().isHolyArtifactsEnabled())
			{
				if (!BelieverManager.get().hasRecentHolyArtifactBlessing(believer.getUniqueId()))
				{
					float power = getGodPower(godName);

					if ((power >= GodsConfiguration.get().getMinGodPowerForItemBlessings()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						blessPlayerWithHolyArtifact(godName, believer);

						LanguageManager.get().setPlayerName(believer.getDisplayName());
						BelieverManager.get().setHolyArtifactBlessingTime(believer.getUniqueId());

						return true;
					}
				}
			}
		}

		if (!BelieverManager.get().hasRecentItemBlessing(believer.getUniqueId()))
		{
			if (blessPlayer(godName, believer.getUniqueId(), getGodPower(godName)))
			{
				LanguageManager.get().setPlayerName(believer.getDisplayName());

				GodSay(godName, believer, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, believer.getUniqueId());

				return true;
			}
		}

		if (GodsConfiguration.get().isMarriageEnabled() && this.random.nextInt(501) == 0)
		{
			List<MarriageManager.MarriedCouple> marriedCouples = MarriageManager.get().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				LanguageManager.get().setPlayerName(Gods.get().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + Gods.get().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((BelieverManager.get().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomExaltedSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForNeutralGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}
		if ((GodsConfiguration.get().isMarriageEnabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriageManager.MarriedCouple> marriedCouples = MarriageManager.get().getMarriedCouples();
			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				LanguageManager.get().setPlayerName(Gods.get().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + Gods.get().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}
		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((BelieverManager.get().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}
			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomNeutralSpeech);
			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private boolean manageBelieverForPleasedGod(String godName, Player believer)
	{
		if (believer == null)
		{
			return false;
		}

		if (!Gods.get().isEnabledInWorld(believer.getWorld()))
		{
			return false;
		}

		if (believer.getGameMode() != GameMode.CREATIVE && PermissionsManager.get().hasPermission(believer, "gods.itemblessings"))
		{
			if (!BelieverManager.get().hasRecentItemBlessing(believer.getUniqueId()))
			{
				if (GodsConfiguration.get().isItemBlessingEnabled())
				{
					float power = getGodPower(godName);
					if ((power >= GodsConfiguration.get().getMinGodPowerForItemBlessings()) && (this.random.nextInt((int) (1.0F + 100.0F / power)) == 0))
					{
						double healing = getHealthNeed(godName, believer);
						if ((healing > 1.0D) && (this.random.nextInt(2) == 0))
						{
							healPlayer(godName, believer, getHealthBlessing(godName));

							BelieverManager.get().setItemBlessingTime(believer.getUniqueId());

							return true;
						}

						ItemStack blessedItem = blessPlayerWithItem(godName, believer);

						if (blessedItem != null)
						{
							LanguageManager.get().setPlayerName(believer.getDisplayName());
							try
							{
								LanguageManager.get().setType(LanguageManager.get().getItemTypeName(blessedItem.getType()));
							}
							catch (Exception ex)
							{
								Gods.get().logDebug(ex.getStackTrace().toString());
							}
							BelieverManager.get().setItemBlessingTime(believer.getUniqueId());

							return true;
						}
					}
				}
			}
		}

		if ((GodsConfiguration.get().isMarriageEnabled()) && (this.random.nextInt(501) == 0))
		{
			List<MarriageManager.MarriedCouple> marriedCouples = MarriageManager.get().getMarriedCouples();

			if (marriedCouples.size() > 0)
			{
				MarriageManager.MarriedCouple couple = marriedCouples.get(this.random.nextInt(marriedCouples.size()));

				LanguageManager.get().setPlayerName(Gods.get().getServer().getOfflinePlayer(couple.player1Id).getName() + " and " + Gods.get().getServer().getOfflinePlayer(couple.player2Id).getName());
				godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverMarriedCouple);
				return true;
			}
		}

		if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
		{
			if ((BelieverManager.get().hasRecentPrayer(believer.getUniqueId())) && (this.random.nextInt(2) == 0))
			{
				return false;
			}

			godSayToBeliever(godName, believer.getUniqueId(), LanguageManager.LANGUAGESTRING.GodToBelieverRandomPleasedSpeech);

			return true;
		}

		if (this.random.nextInt(1 + 600 / getVerbosityForGod(godName)) == 0)
		{
			if (godSayNeededSacrificeToBeliever(godName, believer.getUniqueId()))
			{
				return true;
			}
		}
		return false;
	}

	private void manageBelievers(String godName)
	{
		Set<UUID> believers = BelieverManager.get().getOnlineBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();
		if (believers.size() == 0)
		{
			return;
		}

		GodMood godMood = getMoodForGod(godName);

		List<UUID> priests = getPriestsForGod(godName);
		for (int n = 0; n < 10; n++)
		{
			UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];

			if (!managedBelievers.contains(believerId))
			{
				if (priests.size() == 0)
				{
					LanguageManager.get().setPlayerName("our priest");
				}
				else
				{
					UUID priest = priests.get(this.random.nextInt(priests.size()));

					if (priest != null)
					{
						LanguageManager.get().setPlayerName(Gods.get().getServer().getOfflinePlayer(priest).getName());
					}
				}

				Player believer = Gods.get().getServer().getPlayer(believerId);

				switch (godMood)
				{
				case ANGRY:
					manageBelieverForExaltedGod(godName, believer);
					break;
				case DISPLEASED:
					manageBelieverForPleasedGod(godName, believer);
					break;
				case EXALTED:
					manageBelieverForNeutralGod(godName, believer);
					break;
				case NEUTRAL:
					manageBelieverForDispleasedGod(godName, believer);
					break;
				case PLEASED:
					manageBelieverForAngryGod(godName, believer);
				}

				managedBelievers.add(believerId);
			}
		}
	}

	private void manageBlessings(String godName)
	{
		if (!GodsConfiguration.get().isBlessingEnabled())
		{
			return;
		}
		String blessedPlayer = getBlessedPlayerForGod(godName);
		if (blessedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			Player player = Gods.get().getServer().getPlayer(blessedPlayer);

			if ((player == null) || (!PermissionsManager.get().hasPermission(player, "gods.blessings")))
			{
				return;
			}

			if (blessPlayer(godName, player.getUniqueId(), getGodPower(godName)))
			{
				LanguageManager.get().setPlayerName(blessedPlayer);

				GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPlayerBlessed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerBlessed, player.getUniqueId());
			}
		}
	}

	private void manageCurses(String godName)
	{
		if (!GodsConfiguration.get().isCursingEnabled())
		{
			return;
		}

		Player cursedPlayer = getCursedPlayerForGod(godName);

		if (cursedPlayer == null)
		{
			return;
		}

		int godPower = 1 + (int) GodManager.get().getGodPower(godName);

		if (this.random.nextInt(1 + 100 / godPower) == 0)
		{
			if (!PermissionsManager.get().hasPermission(cursedPlayer, "gods.curses"))
			{
				return;
			}

			if (cursePlayer(godName, cursedPlayer.getUniqueId(), godPower))
			{
				LanguageManager.get().setPlayerName(cursedPlayer.getDisplayName());

				GodSay(godName, cursedPlayer, LanguageManager.LANGUAGESTRING.GodToPlayerCursed, 2 + this.random.nextInt(10));

				GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversPlayerCursed, cursedPlayer.getUniqueId());
			}
		}
	}

	private void manageHolyLands()
	{
		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}
		if (this.random.nextInt(1000) > 0)
		{
			return;
		}
		
		HolyLandManager.get().removeAbandonedLands();
	}
	
	private void manageMiracles(String godName)
	{
		Set<UUID> believers = BelieverManager.get().getOnlineBelieversForGod(godName);
		if (believers.size() == 0)
		{
			return;
		}

		UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];
		
		// Detect more than 3 croptes around and setState to RIPE
		//Material land = Material.
	}

	private void manageLostBelievers(String godName)
	{
		if (this.random.nextInt(100) > 0)
		{
			return;
		}

		Set<UUID> believers = BelieverManager.get().getBelieversForGod(godName);
		Set<UUID> managedBelievers = new HashSet<UUID>();

		if (believers.size() == 0)
		{
			return;
		}

		Gods.get().logDebug("Managing lost believers for " + godName);

		for (int n = 0; n < 5; n++)
		{
			UUID believerId = (UUID) believers.toArray()[this.random.nextInt(believers.size())];
			if (!managedBelievers.contains(believerId))
			{
				Date thisDate = new Date();

				long timeDiff = thisDate.getTime() - BelieverManager.get().getLastPrayerTime(believerId).getTime();

				if (timeDiff > 3600000 * GodsConfiguration.get().getMaxBelieverPrayerTime())
				{
					String believerName = Gods.get().getServer().getOfflinePlayer(believerId).getName();
					LanguageManager.get().setPlayerName(believerName);

					godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversLostBeliever, 2 + this.random.nextInt(100));

					BelieverManager.get().removeBeliever(godName, believerId);
				}
			}

			managedBelievers.add(believerId);
		}
	}

	private void manageMood(String godName)
	{
		if (BelieverManager.get().getOnlineBelieversForGod(godName).size() == 0)
		{
			return;
		}
		GodManager.get().addMoodForGod(godName, GodManager.get().getFalloffModifierForGod(godName));
	}

	public boolean managePriests(String godName)
	{
		int numberOfBelievers = BelieverManager.get().getBelieversForGod(godName).size();

		List<UUID> priestNames = getPriestsForGod(godName);

		if (priestNames == null)
		{
			priestNames = new ArrayList<UUID>();
		}

		if (numberOfBelievers < GodsConfiguration.get().getMinBelieversForPriest() + 6 * priestNames.size())
		{
			return false;
		}

		if (priestNames.size() < GodsConfiguration.get().getMaxPriestsPrGod())
		{
			if (this.random.nextInt(3) == 0)
			{
				Gods.get().logDebug(godName + " has too few priests. Finding one...");

				UUID believerId = getNextBelieverForPriest(godName);
				if (believerId == null)
				{
					Gods.get().logDebug(godName + " could not find a candidate for a priest");
					return false;
				}

				Player player = Gods.get().getServer().getPlayer(believerId);

				if (player == null)
				{
					return false;
				}

				if (setPendingPriest(godName, believerId))
				{
					Gods.get().log(godName + " offered " + player.getName() + " to be priest");
					LanguageManager.get().setPlayerName(player.getName());

					GodSayWithQuestion(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverOfferPriest, 2);

					return true;
				}
			}
		}

		for (UUID priestId : priestNames)
		{
			if (this.random.nextInt(1 + 1000 / getVerbosityForGod(godName)) == 0)
			{
				Player player = Gods.get().getServer().getPlayer(priestId);

				if (player != null)
				{
					LanguageManager.get().setPlayerName(player.getDisplayName());
					int r = 0;
					int t = 0;
					do
					{
						r = this.random.nextInt(3);
						t++;
					}
					while ((t < 50) && (((r == 1) && (!GodsConfiguration.get().isBiblesEnabled())) || ((r == 2) && (!GodsConfiguration.get().isPropheciesEnabled()))));
					try
					{
						switch (r)
						{
						case 0:
							switch (this.random.nextInt(4))
							{
							case 0:
								LanguageManager.get().setType(LanguageManager.get().getItemTypeName(getHolyFoodTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestEatFoodType);
								break;
							case 1:
								LanguageManager.get().setType(LanguageManager.get().getItemTypeName(getUnholyFoodTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotEatFoodType);
								break;
							case 2:
								LanguageManager.get().setType(LanguageManager.get().getMobTypeName(getUnholyMobTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestSlayMobType);
								break;
							case 3:
								LanguageManager.get().setType(LanguageManager.get().getMobTypeName(getHolyMobTypeForGod(godName)));
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestNotSlayMobType);
							}
							return true;
						case 1:
							if (GodsConfiguration.get().isBiblesEnabled())
							{
								String bibleTitle = HolyBookManager.get().getBibleTitle(godName);
								LanguageManager.get().setType(bibleTitle);
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestUseBible);
								return true;
							}
							break;
						case 2:
							if (GodsConfiguration.get().isPropheciesEnabled())
							{
								String bibleTitle = HolyBookManager.get().getBibleTitle(godName);
								try
								{
									LanguageManager.get().setType(bibleTitle);
								}
								catch (Exception ex)
								{
									Gods.get().logDebug(ex.getStackTrace().toString());
								}
								GodSayToPriest(godName, LanguageManager.LANGUAGESTRING.GodToPriestUseProphecies);
								return true;
							}
							break;
						case 3:
							if (GodsConfiguration.get().isHolyArtifactsEnabled())
							{
								String bibleTitle = HolyBookManager.get().getBibleTitle(godName);
								try
								{
									LanguageManager.get().setType(bibleTitle);
								}
								catch (Exception ex)
								{
									Gods.get().logDebug(ex.getStackTrace().toString());
								}
								return true;
							}
							break;
						case 4:
							if (GodsConfiguration.get().isMarriageEnabled())
							{
								String bibleTitle = HolyBookManager.get().getBibleTitle(godName);
								LanguageManager.get().setType(bibleTitle);

								return true;
							}
							break;
						}
					}
					catch (Exception ex)
					{
						Gods.get().logDebug(ex.getStackTrace().toString());
					}
				}
			}
		}
		return false;
	}

	private void manageQuests(String godName)
	{
		if (!GodsConfiguration.get().isQuestsEnabled())
		{
			return;
		}

		int numberOfBelievers = BelieverManager.get().getOnlineBelieversForGod(godName).size();

		if (!QuestManager.get().hasQuest(godName))
		{
			if (numberOfBelievers < GodsConfiguration.get().getRequiredBelieversForQuests() || this.getMinutesSinceLastQuest(godName) < GodsConfiguration.get().getMinMinutesBetweenQuests())
			{
				return;
			}

			QuestManager.get().generateQuest(godName);
		}
		else if (QuestManager.get().hasExpiredQuest(godName))
		{
			addMoodForGod(godName, getAngryModifierForGod(godName));

			QuestManager.get().godSayFailed(godName);

			QuestManager.get().removeFailedQuestForGod(godName);
		}
		else if (random.nextInt(5) == 0)
		{
			QuestManager.get().godSayStatus(godName);
		}
	}

	private void manageSacrifices()
	{
		if (!GodsConfiguration.get().isSacrificesEnabled())
		{
			return;
		}

		if (this.random.nextInt(10) > 0)
		{
			return;
		}

		AltarManager.get().clearDroppedItems();
	}

	private void manageSacrifices(String godName)
	{
		if (!GodsConfiguration.get().isSacrificesEnabled())
		{
			return;
		}

		int godPower = 1 + (int) GodManager.get().getGodPower(godName);
		if (this.random.nextInt(20 + (int) (70.0F / godPower)) > 0)
		{
			return;
		}
		Material type = getSacrificeNeedForGod(godName);

		float value = getSacrificeValueForGod(godName, type);

		value += 1 + this.random.nextInt(3);
		if (value > 64.0F)
		{
			value = 64.0F;
		}
		else if (value < -64.0F)
		{
			value = -64.0F;
		}
		Gods.get().logDebug("Increasing wanted " + type.name() + " sacrifice need for " + godName + " to " + value);

		this.godsConfig.set(godName + ".SacrificeValues." + type.name(), Float.valueOf(value));

		saveTimed();

		type = getSacrificeUnwantedForGod(godName);
		if (type != null)
		{
			value = 0.25F * getSacrificeValueForGod(godName, type);
			if (value > -0.5D)
			{
				value = 0.0F;
			}
			Gods.get().logDebug("Reducing unwanted " + type.name() + " sacrifice need for " + godName + " to " + value);
			if (value == 0.0F)
			{
				this.godsConfig.set(godName + ".SacrificeValues." + type.name(), null);
			}
			else
			{
				this.godsConfig.set(godName + ".SacrificeValues." + type.name(), Float.valueOf(value));
			}
			save();
		}
	}

	public void OtherGodSayToBelievers(String godName, LanguageManager.LANGUAGESTRING message, int delay)
	{
		for (Player player : Gods.get().getServer().getOnlinePlayers())
		{
			String playerGod = BelieverManager.get().getGodForBeliever(player.getUniqueId());

			if (playerGod != null && !playerGod.equals(godName))
			{
				GodSay(godName, player, message, delay);
			}
		}
	}

	public boolean removeBeliever(UUID believerId)
	{
		String godName = BelieverManager.get().getGodForBeliever(believerId);

		if (godName == null)
		{
			return false;
		}

		if (isPriestForGod(believerId, godName))
		{
			removePriest(godName, believerId);
		}

		BelieverManager.get().removeBeliever(godName, believerId);

		LanguageManager.get().setPlayerName(Gods.get().getServer().getOfflinePlayer(believerId).getName());
		godSayToBelievers(godName, LanguageManager.LANGUAGESTRING.GodToBelieversLostBeliever, 2 + this.random.nextInt(100));

		return true;
	}

	public void removeGod(String godName)
	{
		for (String otherGodName : getAllGods())
		{
			if (hasAllianceRelation(otherGodName, godName))
			{
				toggleAllianceRelationForGod(otherGodName, godName);
			}

			if (hasWarRelation(otherGodName, godName))
			{
				toggleWarRelationForGod(otherGodName, godName);
			}
		}

		this.godsConfig.set(godName, null);

		HolyBookManager.get().clearBible(godName);

		save();
	}

	public void removePriest(String godName, UUID playerId)
	{
		Gods.get().getServer().dispatchCommand(Bukkit.getConsoleSender(), LanguageManager.get().getPriestRemoveCommand(playerId));

		List<String> priests = this.godsConfig.getStringList(godName + ".Priests");

		priests.remove(playerId.toString());

		this.godsConfig.set(godName + ".Priests", priests);

		saveTimed();

		Gods.get().log(godName + " removed " + Gods.get().getServer().getOfflinePlayer(playerId).getName() + " as priest");
	}

	public boolean rewardBeliever(String godName, Player believer)
	{
		ItemStack items = new ItemStack(getRewardBlessing(godName));

		giveItem(godName, believer, items.getType(), false);

		return true;
	}

	public void save()
	{
		this.lastSaveTime = System.currentTimeMillis();
		if ((this.godsConfig == null) || (this.godsConfigFile == null))
		{
			return;
		}
		try
		{
			this.godsConfig.save(this.godsConfigFile);
		}
		catch (Exception ex)
		{
			Gods.get().log("Could not save config to " + this.godsConfigFile + ": " + ex.getMessage());
		}
		Gods.get().log("Saved configuration");
	}

	public void saveTimed()
	{
		if (System.currentTimeMillis() - this.lastSaveTime < 180000L)
		{
			return;
		}
		save();
	}

	public void sendInfoToBelievers(String godName, LanguageManager.LANGUAGESTRING message, ChatColor color, int delay)
	{
		for (UUID playerId : BelieverManager.get().getBelieversForGod(godName))
		{
			Player player = Gods.get().getServer().getPlayer(playerId);

			if (player != null)
			{
				Gods.get().sendInfo(playerId, message, color, 0, "", 10);
			}
		}
	}

	public void sendInfoToBelievers(String godName, LanguageManager.LANGUAGESTRING message, ChatColor color, String name, int amount1, int amount2, int delay)
	{
		for (UUID playerId : BelieverManager.get().getBelieversForGod(godName))
		{
			Player player = Gods.get().getServer().getPlayer(playerId);
			if (player != null)
			{
				Gods.get().sendInfo(playerId, message, color, name, amount1, amount2, 10);
			}
		}
	}

	public void setBlessedPlayerForGod(String godName, UUID believerId)
	{
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date thisDate = new Date();

		this.godsConfig.set(godName + ".BlessedPlayer", believerId);
		this.godsConfig.set(godName + ".BlessedTime", formatter.format(thisDate));

		saveTimed();
	}

	public void setColorForGod(String godName, ChatColor color)
	{
		this.godsConfig.set(godName + ".Color", color.name());

		saveTimed();
	}

	public void setContestedHolyLandForGod(String godName, Location contestedLand)
	{
		new SimpleDateFormat(this.pattern);
		new Date();

		this.godsConfig.set(godName + ".ContestedLand.Hash", Long.valueOf(HolyLandManager.get().hashLocation(contestedLand)));

		this.godsConfig.set(godName + ".ContestedLand" + ".X", Integer.valueOf(contestedLand.getBlockX()));
		this.godsConfig.set(godName + ".ContestedLand" + ".Y", Integer.valueOf(contestedLand.getBlockY()));
		this.godsConfig.set(godName + ".ContestedLand" + ".Z", Integer.valueOf(contestedLand.getBlockZ()));
		this.godsConfig.set(godName + ".ContestedLand" + ".World", contestedLand.getWorld().getName());

		HolyLandManager.get().setContestedLand(contestedLand, godName);

		saveTimed();
	}

	public void setCursedPlayerForGod(String godName, UUID believerId)
	{
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date thisDate = new Date();

		this.godsConfig.set(godName + ".CursedPlayer", believerId);
		this.godsConfig.set(godName + ".CursedTime", formatter.format(thisDate));

		saveTimed();
	}

	public void setDivineForceForGod(String godName, GodType divineForce)
	{
		this.godsConfig.set(godName + ".DivineForce", divineForce.name().toUpperCase());

		save();
	}

	public void setGenderForGod(String godName, GodGender godGender)
	{
		this.godsConfig.set(godName + ".Gender", godGender.name());

		saveTimed();
	}

	public void setGodDescription(String godName, String description)
	{
		this.godsConfig.set(godName + ".Description", description);

		saveTimed();
	}

	public void setGodMobSpawning(String godName, boolean mobSpawning)
	{
		this.godsConfig.set(godName + ".MobSpawning", Boolean.valueOf(mobSpawning));

		saveTimed();
	}

	public void setGodPvP(String godName, boolean pvp)
	{
		this.godsConfig.set(godName + ".PvP", Boolean.valueOf(pvp));

		saveTimed();
	}

	public void setHomeForGod(String godName, Location location)
	{
		this.godsConfig.set(godName + ".Home.X", Double.valueOf(location.getX()));
		this.godsConfig.set(godName + ".Home.Y", Double.valueOf(location.getY()));
		this.godsConfig.set(godName + ".Home.Z", Double.valueOf(location.getZ()));
		this.godsConfig.set(godName + ".Home.World", location.getWorld().getName());

		saveTimed();
	}

	public boolean setPendingPriest(String godName, UUID believerId)
	{
		String lastPriestTime = this.godsConfig.getString(godName + ".PendingPriestTime");

		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date lastDate = null;
		Date thisDate = new Date();
		try
		{
			lastDate = formatter.parse(lastPriestTime);
		}
		catch (Exception ex)
		{
			lastDate = new Date();
			lastDate.setTime(0L);
		}
		long diff = thisDate.getTime() - lastDate.getTime();
		long diffMinutes = diff / 60000L % 60L;
		if (diffMinutes < 3L)
		{
			return false;
		}

		if (believerId == null)
		{
			return false;
		}

		this.godsConfig.set(godName + ".PendingPriest", believerId.toString());

		saveTimed();

		BelieverManager.get().setPendingPriest(believerId);

		return true;
	}

	public boolean setPlayerOnFire(String playerName, int seconds)
	{
		for (Player matchPlayer : Gods.get().getServer().matchPlayer(playerName))
		{
			matchPlayer.setFireTicks(seconds);
		}
		return true;
	}

	public void setPrivateAccess(String godName, boolean privateAccess)
	{
		this.godsConfig.set(godName + ".PrivateAccess", Boolean.valueOf(privateAccess));

		saveTimed();
	}

	public void setTimeSinceLastQuest(String godName)
	{
		DateFormat formatter = new SimpleDateFormat(this.pattern);
		Date thisDate = new Date();

		this.godsConfig.set(godName + ".LastQuestTime", formatter.format(thisDate));

		saveTimed();
	}

	public void spawnGuidingMobs(String godName, UUID playerId, Location targetLocation)
	{
		EntityType mobType = getHolyMobTypeForGod(godName);

		Player player = Gods.get().getServer().getPlayer(playerId);
		if (player == null)
		{
			return;
		}
		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskSpawnGuideMob(player, targetLocation, mobType), 2L);
	}

	public void spawnHostileMobs(String godName, Player player, EntityType mobType, int numberOfMobs)
	{
		Gods.get().getServer().getScheduler().runTaskLater(Gods.get(), new TaskSpawnHostileMobs(godName, player, mobType, numberOfMobs), 2L);
	}

	public boolean strikeCreatureWithLightning(Creature creature, int damage)
	{
		if (damage <= 0)
		{
			creature.getWorld().strikeLightningEffect(creature.getLocation());
		}
		else
		{
			LightningStrike strike = creature.getWorld().strikeLightning(creature.getLocation());
			creature.damage(damage - 1, strike);
		}
		return true;
	}

	public boolean strikePlayerWithLightning(UUID playerId, int damage)
	{
		Player player = Gods.get().getServer().getPlayer(playerId);

		if (player != null)
		{
			if (damage <= 0)
			{
				player.getWorld().strikeLightningEffect(player.getLocation());
			}
			else
			{
				LightningStrike strike = player.getWorld().strikeLightning(player.getLocation());
				player.damage(damage - 1, strike);
			}
		}
		return true;
	}

	public boolean strikePlayerWithMobs(String godName, UUID playerId, float godPower)
	{
		Player player = Gods.get().getServer().getPlayer(playerId);

		if (player == null)
		{
			Gods.get().logDebug("player is null");
		}

		EntityType mobType = EntityType.UNKNOWN;

		switch (this.random.nextInt(5))
		{
		case 0:
			mobType = EntityType.SKELETON;
			break;
		case 1:
			mobType = EntityType.ZOMBIE;
			break;
		case 2:
			mobType = EntityType.PIG_ZOMBIE;
			break;
		case 3:
			mobType = EntityType.SPIDER;
			break;
		case 4:
			mobType = EntityType.WOLF;
			break;
		case 5:
			mobType = EntityType.GIANT;
		}
		int numberOfMobs = 1 + (int) (godPower / 10.0F);

		spawnHostileMobs(godName, player, mobType, numberOfMobs);

		return true;
	}

	public boolean toggleAllianceRelationForGod(String godName, String allyGodName)
	{
		List<String> gods = this.godsConfig.getStringList(godName + ".Allies");
		if (!gods.contains(allyGodName))
		{
			gods.add(allyGodName);

			this.godsConfig.set(godName + ".Allies", gods);

			gods = this.godsConfig.getStringList(allyGodName + ".Allies");
			if (!gods.contains(godName))
			{
				gods.add(godName);
				this.godsConfig.set(allyGodName + ".Allies", gods);
			}
			if (this.godsConfig.getStringList(godName + ".Enemies").contains(allyGodName))
			{
				this.godsConfig.set(godName + ".Enemies." + allyGodName, null);
			}
			if (this.godsConfig.getStringList(allyGodName + ".Enemies").contains(godName))
			{
				this.godsConfig.set(allyGodName + ".Enemies." + godName, null);
			}
			saveTimed();

			return true;
		}
		gods.remove(allyGodName);
		this.godsConfig.set(godName + ".Allies", gods);

		gods = this.godsConfig.getStringList(allyGodName + ".Allies");
		if (gods.contains(godName))
		{
			gods.remove(godName);
			this.godsConfig.set(allyGodName + ".Allies", gods);
		}
		if (this.godsConfig.getStringList(godName + ".Enemies").contains(allyGodName))
		{
			this.godsConfig.set(godName + ".Enemies." + allyGodName, null);
		}
		if (this.godsConfig.getStringList(allyGodName + ".Enemies").contains(godName))
		{
			this.godsConfig.set(allyGodName + ".Enemies." + godName, null);
		}
		save();

		return false;
	}

	public boolean toggleWarRelationForGod(String godName, String enemyGodName)
	{
		List<String> gods = this.godsConfig.getStringList(godName + ".Enemies");
		if (!gods.contains(enemyGodName))
		{
			gods.add(enemyGodName);
			this.godsConfig.set(godName + ".Enemies", gods);

			gods = this.godsConfig.getStringList(enemyGodName + ".Enemies");
			if (!gods.contains(godName))
			{
				gods.add(godName);
				this.godsConfig.set(enemyGodName + ".Enemies", gods);
			}
			if (this.godsConfig.getStringList(godName + ".Allies").contains(enemyGodName))
			{
				this.godsConfig.set(godName + ".Allies." + enemyGodName, null);
			}
			if (this.godsConfig.getStringList(enemyGodName + ".Allies").contains(godName))
			{
				this.godsConfig.set(enemyGodName + ".Allies." + godName, null);
			}
			saveTimed();

			return true;
		}
		gods.remove(enemyGodName);
		this.godsConfig.set(godName + ".Enemies", gods);

		gods = this.godsConfig.getStringList(enemyGodName + ".Enemies");
		if (gods.contains(godName))
		{
			gods.remove(godName);
			this.godsConfig.set(enemyGodName + ".Enemies", gods);
		}
		if (this.godsConfig.getStringList(godName + ".Allies").contains(enemyGodName))
		{
			this.godsConfig.set(godName + ".Allies." + enemyGodName, null);
		}
		if (this.godsConfig.getStringList(enemyGodName + ".Allies").contains(godName))
		{
			this.godsConfig.set(enemyGodName + ".Allies." + godName, null);
		}
		save();

		return false;
	}

	public void update()
	{
		if (this.random.nextInt(50) == 0)
		{
			Gods.get().logDebug("Processing dead offline Gods...");

			long timeBefore = System.currentTimeMillis();

			List<String> godNames = getOfflineGods();
			for (String offlineGodName : godNames)
			{
				if (isDeadGod(offlineGodName))
				{
					Gods.get().log("Removed dead offline God '" + offlineGodName + "'");
				}
			}
			long timeAfter = System.currentTimeMillis();

			Gods.get().logDebug("Processed " + godNames.size() + " offline Gods in " + (timeAfter - timeBefore) + " ms");
		}

		List<String> godNames = getOnlineGods();

		long timeBefore = System.currentTimeMillis();

		if (godNames.size() == 0)
		{
			return;
		}
		String godName = (String) godNames.toArray()[this.random.nextInt(godNames.size())];

		Gods.get().logDebug("Processing God '" + godName + "'");

		boolean godTalk = false;

		manageMood(godName);

		if (!godTalk)
		{
			godTalk = managePriests(godName);
		}

		manageLostBelievers(godName);

		if (!godTalk)
		{
			manageBelievers(godName);
		}

		if (!godTalk)
		{
			manageQuests(godName);
		}

		manageBlessings(godName);

		manageCurses(godName);

		manageSacrifices(godName);

		manageSacrifices();

		manageHolyLands();
		
		manageMiracles(godName);

		long timeAfter = System.currentTimeMillis();

		Gods.get().logDebug("Processed 1 Online God in " + (timeAfter - timeBefore) + " ms");
		if (this.random.nextInt(1000) == 0)
		{
			Gods.get().logDebug("Processing chests...");
		}
	}

	public void updateOnlineGods()
	{
		this.onlineGods.clear();
		for (Player player : Gods.get().getServer().getOnlinePlayers())
		{
			String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
			if (godName != null)
			{
				if (!this.onlineGods.contains(godName))
				{
					this.onlineGods.add(godName);
				}
			}
		}
	}
}