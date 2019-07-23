package com.dogonfire.gods.managers;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.tasks.TaskLove;

public class MarriageManager
{
	public class MarriedCouple
	{
		public UUID		player1Id;
		public UUID		player2Id;
		public String	godName;
		public Date		lastLove;

		MarriedCouple(UUID player1Id, UUID player2Id, String godName, Date lastLove)
		{
			this.player1Id = player1Id;
			this.player2Id = player2Id;
			this.godName = godName;
			this.lastLove = lastLove;
		}
	}

	public class MarriedCoupleComparator implements Comparator<MarriageManager.MarriedCouple>
	{
		public MarriedCoupleComparator()
		{
		}

		@Override
		public int compare(MarriageManager.MarriedCouple object1, MarriageManager.MarriedCouple object2)
		{
			MarriageManager.MarriedCouple b1 = object1;
			MarriageManager.MarriedCouple b2 = object2;

			return (int) (b2.lastLove.getTime() - b1.lastLove.getTime());
		}
	}

	private static MarriageManager instance;

	public static MarriageManager get()
	{
		if (GodsConfiguration.get().isMarriageEnabled() && instance == null)
			instance = new MarriageManager();
		return instance;
	}

	private FileConfiguration	marriagesConfig		= null;

	private File				marriagesConfigFile	= null;

	private Random				random				= new Random();

	private Material			marriageTokenType	= Material.GOLD_NUGGET;

	private MarriageManager()
	{
	}

	public void divorce(UUID believerId)
	{
		String partnerId = this.marriagesConfig.getString(believerId.toString() + ".Married.Partner");

		this.marriagesConfig.set(believerId.toString(), null);

		if (partnerId != null)
		{
			this.marriagesConfig.set(partnerId, null);

			Player partner = Gods.get().getServer().getPlayer(partnerId);
			if (partner != null)
			{
				Gods.get().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.DivorcedYou, ChatColor.RED, "DIVORCED", Gods.get().getServer().getPlayer(partnerId).getDisplayName(), 1);
			}
		}
		save();
	}

	public UUID getGettingMarriedPartner(UUID playerId)
	{
		String partnerId = this.marriagesConfig.getString(playerId.toString() + ".GettingMarried.Partner");

		if (partnerId == null)
		{
			return null;
		}

		return UUID.fromString(partnerId);
	}

	public List<MarriedCouple> getMarriedCouples()
	{
		Set<String> list = this.marriagesConfig.getKeys(false);
		List<MarriedCouple> couples = new ArrayList<MarriedCouple>();
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastLove = null;

		List<UUID> names = new ArrayList<UUID>();

		for (String player : list)
		{
			UUID playerId = UUID.fromString(player);
			UUID partnerId = getPartnerId(playerId);

			if (partnerId == null)
			{
				continue;
			}

			if (!names.contains(partnerId))
			{
				String lastLoveString = this.marriagesConfig.getString(playerId + ".Married.LastLove");

				try
				{
					lastLove = formatter.parse(lastLoveString);
				}
				catch (Exception ex)
				{
					Gods.get().log("Invalid lastlove format for " + playerId);
					lastLove = new Date();

					this.marriagesConfig.set(playerId.toString() + ".Married.LastLove", formatter.format(lastLove));
					this.marriagesConfig.set(partnerId.toString() + ".Married.LastLove", formatter.format(lastLove));

					save();
				}

				couples.add(new MarriedCouple(playerId, partnerId, BelieverManager.get().getGodForBeliever(playerId), lastLove));

				names.add(playerId);
			}
		}

		Collections.sort(couples, new MarriedCoupleComparator());

		return couples;
	}

	public UUID getPartnerId(UUID believerId)
	{
		String partner = this.marriagesConfig.getString(believerId.toString() + ".Married.Partner");

		if (partner == null)
		{
			return null;
		}

		return UUID.fromString(partner);
	}

	public String getPartnerName(UUID playerId)
	{
		String partnerId = this.marriagesConfig.getString(playerId.toString() + ".Married.Partner");

		if (partnerId == null)
		{
			return null;
		}

		return Gods.get().getServer().getOfflinePlayer(UUID.fromString(partnerId)).getName();
	}

	public UUID getProposal(UUID believerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		Date offerDate = null;

		String offerDateString = this.marriagesConfig.getString(believerId + ".MarriageProposal.Time");
		try
		{
			offerDate = formatter.parse(offerDateString);
		}
		catch (Exception ex)
		{
			Gods.get().logDebug("Could no parse marriage proposal time: " + ex.getMessage());
			offerDate = new Date();
			offerDate.setTime(0L);
		}

		long diff = thisDate.getTime() - offerDate.getTime();
		long diffSeconds = diff / 1000L;

		if (diffSeconds > 30L)
		{
			Gods.get().logDebug("getProposal DiffSeconds is " + diffSeconds);

			this.marriagesConfig.set(believerId + ".MarriageProposal", null);

			save();

			return null;
		}

		return UUID.fromString(this.marriagesConfig.getString(believerId.toString() + ".MarriageProposal.Partner"));
	}

	public void handleAcceptProposal(UUID playerId1, UUID playerId2, String godName)
	{
		Player player1 = Gods.get().getServer().getPlayer(playerId1);
		Player player2 = Gods.get().getServer().getPlayer(playerId2);

		try
		{
			LanguageManager.get().setType(LanguageManager.get().getItemTypeName(this.marriageTokenType));
		}
		catch (Exception ex)
		{
			Gods.get().logDebug(ex.getStackTrace().toString());
		}

		LanguageManager.get().setPlayerName(player2.getDisplayName());
		GodManager.get().GodSay(godName, player1, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedMarriageProposal, 2 + this.random.nextInt(40));

		LanguageManager.get().setPlayerName(player1.getDisplayName());
		GodManager.get().GodSay(godName, player2, LanguageManager.LANGUAGESTRING.GodToBelieverAcceptedYourMarriageProposal, 2 + this.random.nextInt(40));

		setGettingMarried(playerId1, playerId2);
	}

	public void handlePickupItem(Player player, Item item, Location location)
	{
		if (item.getItemStack().getType() != this.marriageTokenType)
		{
			return;
		}

		{
			UUID partnerId = getPartnerId(player.getUniqueId());

			// Already married
			if (partnerId != null)
			{
				return;
			}
		}

		UUID gettingMarriedPartnerId = getGettingMarriedPartner(player.getUniqueId());

		if (gettingMarriedPartnerId == null)
		{
			return;
		}

		Player partner = Gods.get().getServer().getPlayer(gettingMarriedPartnerId);

		if (player == null || partner == null)
		{
			return;
		}

		this.marriagesConfig.set(player.getUniqueId().toString() + ".GettingMarried.HasPickupWeddingToken", true);

		// if (hasPickupWeddingRing(player.getUniqueId()))
		{
			String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());

			if (hasPickupWeddingRing(gettingMarriedPartnerId))
			{
				setMarried(player.getUniqueId(), gettingMarriedPartnerId);

				float godPower = GodManager.get().getGodPower(godName);

				GodManager.get().blessPlayer(godName, player.getUniqueId(), godPower);
				GodManager.get().blessPlayer(godName, gettingMarriedPartnerId, godPower);

				LanguageManager.get().setPlayerName(partner.getDisplayName());
				GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				LanguageManager.get().setPlayerName(player.getDisplayName());
				GodManager.get().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarried, 10);

				Gods.get().getServer().broadcastMessage(ChatColor.WHITE + player.getDisplayName() + ChatColor.AQUA + " just married " + ChatColor.WHITE + partner.getDisplayName() + ChatColor.AQUA + " in the name of " + ChatColor.GOLD + godName + ChatColor.AQUA + "!");
				if ((GodsConfiguration.get().isHolyArtifactsEnabled()) && (GodsConfiguration.get().isMarriageFireworksEnabled()))
				{
					HolyPowerManager.get().shootFirework(player, 16);
					HolyPowerManager.get().shootFirework(partner, 16);
				}
			}
			else
			{
				try
				{
					LanguageManager.get().setType(LanguageManager.get().getItemTypeName(this.marriageTokenType));
				}
				catch (Exception ex)
				{
					Gods.get().logDebug(ex.getStackTrace().toString());
				}
				LanguageManager.get().setPlayerName(partner.getDisplayName());
				GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToBelieverMarriageTokenPickup, 10);

				LanguageManager.get().setPlayerName(player.getDisplayName());
				GodManager.get().GodSay(godName, partner, LanguageManager.LANGUAGESTRING.GodToBelieverMarriagePartnerTokenPickup, 10);
			}
		}

		save();
	}

	public boolean hasPickupWeddingRing(UUID playerId)
	{
		if (playerId == null)
		{
			Gods.get().log("playerId==null");
		}

		if (this.marriagesConfig.getString(playerId.toString() + ".GettingMarried.HasPickupWeddingToken") == null)
		{
			return false;
		}

		return true;
	}

	public void load()
	{
		if (this.marriagesConfigFile == null)
		{
			this.marriagesConfigFile = new File(Gods.get().getDataFolder(), "marriages.yml");
		}

		this.marriagesConfig = YamlConfiguration.loadConfiguration(this.marriagesConfigFile);

		Gods.get().log("Loaded " + this.marriagesConfig.getKeys(false).size() + " marriages.");
	}

	public void love(UUID playerId)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();
		String partnerId = this.marriagesConfig.getString(playerId.toString() + ".Married.Partner");

		if (partnerId != null)
		{
			Player player = Gods.get().getServer().getPlayer(playerId);
			Player partner = Gods.get().getServer().getPlayer(UUID.fromString(partnerId));

			this.marriagesConfig.set(player.getUniqueId().toString() + ".Married.LastLove", formatter.format(thisDate));
			this.marriagesConfig.set(partner.getUniqueId().toString() + ".Married.LastLove", formatter.format(thisDate));

			Gods.get().getServer().getScheduler().scheduleSyncDelayedTask(Gods.get(), new TaskLove(player, partner), 1L);
			if (partner != null)
			{
				Gods.get().sendInfo(partner.getUniqueId(), LanguageManager.LANGUAGESTRING.MarrigeLovesYou, ChatColor.GREEN, Gods.get().getServer().getPlayer(playerId).getDisplayName(), ChatColor.DARK_RED + "LOVES", 1);
			}
		}

		save();
	}

	public void proposeMarriage(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player2.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player1.toString() + ".Married", null);
		this.marriagesConfig.set(player2.toString() + ".Married", null);

		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Partner", player1.toString());

		save();
	}

	public void save()
	{
		if ((this.marriagesConfig == null) || (this.marriagesConfigFile == null))
		{
			return;
		}
		try
		{
			this.marriagesConfig.save(this.marriagesConfigFile);
		}
		catch (Exception ex)
		{
			Gods.get().log("Could not save config to " + this.marriagesConfigFile.getName() + ": " + ex.getMessage());
		}
	}

	public void setGettingMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".Married", null);
		this.marriagesConfig.set(player2.toString() + ".Married", null);
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal", null);
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal", null);

		this.marriagesConfig.set(player1.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".GettingMarried.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".GettingMarried.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".GettingMarried.Partner", player1.toString());

		save();
	}

	public void setMarriageProposal(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".Married", null);
		this.marriagesConfig.set(player2.toString() + ".Married", null);
		this.marriagesConfig.set(player1.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player2.toString() + ".GettingMarried", null);

		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal.Partner", player1.toString());

		save();
	}

	public void setMarried(UUID player1, UUID player2)
	{
		String pattern = "HH:mm:ss dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.marriagesConfig.set(player1.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player2.toString() + ".GettingMarried", null);
		this.marriagesConfig.set(player1.toString() + ".MarriageProposal", null);
		this.marriagesConfig.set(player2.toString() + ".MarriageProposal", null);

		this.marriagesConfig.set(player1.toString() + ".Married.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player1.toString() + ".Married.Partner", player2.toString());
		this.marriagesConfig.set(player2.toString() + ".Married.Time", formatter.format(thisDate));
		this.marriagesConfig.set(player2.toString() + ".Married.Partner", player1.toString());

		save();
	}
}