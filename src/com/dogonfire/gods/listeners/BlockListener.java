package com.dogonfire.gods.listeners;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.AltarManager;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.HolyArtifactManager;
import com.dogonfire.gods.managers.HolyBookManager;
import com.dogonfire.gods.managers.HolyLandManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.MarriageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;

public class BlockListener implements Listener
{
	private HashMap<String, Long> lastEatTimes = new HashMap<String, Long>();

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void OnBlockPlace(BlockPlaceEvent event)
	{
		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}
		Player player = event.getPlayer();
		if ((player == null) || (!Gods.get().isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if (player.isOp())
		{
			return;
		}
		if (event.getBlock() == null)
		{
			return;
		}
		
		if (!PermissionsManager.get().hasPermission(player, "gods.holyland"))
		{
			Gods.get().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		
		if (HolyLandManager.get().isNeutralLandLocation(event.getBlock().getLocation()))
		{
			player.sendMessage(ChatColor.RED + "You cannot build in neutral land");

			event.setCancelled(true);
			return;
		}
		
		String godName = HolyLandManager.get().getGodAtHolyLandLocation(event.getBlock().getLocation());
		String playerGod = null;
		
		if (godName == null)
		{
			return;
		}
		
		if (player != null)
		{
			playerGod = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		}
		
		if ((playerGod == null) || (!playerGod.equals(godName)))
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.YELLOW + godName);

			event.setCancelled(true);
			return;
		}
	
	}

	@EventHandler
	public void OnEntityCombust(EntityCombustEvent event)
	{
		if (!GodsConfiguration.get().isSacrificesEnabled())
		{
			return;
		}
		if (event.getEntity() == null)
		{
			return;
		}
		if (!(event.getEntity() instanceof Item))
		{
			return;
		}
		Item item = (Item) event.getEntity();
		if (!Gods.get().isEnabledInWorld(item.getWorld()))
		{
			return;
		}
		if (event.getEntity().getType() != EntityType.DROPPED_ITEM)
		{
			return;
		}
		String believerName = AltarManager.get().getDroppedItemPlayer(event.getEntity().getEntityId());
		if (believerName == null)
		{
			return;
		}
		Player player = Gods.get().getServer().getPlayer(believerName);
		if (player == null)
		{
			return;
		}
		if ((!player.isOp()) && (!PermissionsManager.get().hasPermission(player, "gods.altar.sacrifice")))
		{
			Gods.get().logDebug("Does not have gods.altar.sacrifice");
			return;
		}

		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		if (QuestManager.get().handleSacrifice(player, godName, item.getItemStack().getType().name()))
		{
			return;
		}

		GodManager.get().handleSacrifice(godName, player, item.getItemStack().getType());
	}

	@EventHandler
	public void OnEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() == null)
		{
			return;
		}
		if (!Gods.get().isEnabledInWorld(event.getDamager().getWorld()))
		{
			return;
		}
		if (GodsConfiguration.get().isQuestsEnabled())
		{
			if ((event.getDamager() instanceof Player))
			{
				Player attackerPlayer = (Player) event.getDamager();
				if ((event.getEntity() instanceof Player))
				{
					Player victimPlayer = (Player) event.getEntity();

					String attackerGodName = BelieverManager.get().getGodForBeliever(attackerPlayer.getUniqueId());
					String victimGodName = BelieverManager.get().getGodForBeliever(victimPlayer.getUniqueId());
					if ((attackerGodName != null) && (victimGodName != null))
					{
						if (GodManager.get().hasWarRelation(attackerGodName, victimGodName))
						{
							Gods.get().logDebug("hasWarRelation");

							event.setCancelled(false);
						}
					}
				}
			}
		}
		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}
		if (HolyLandManager.get().isNeutralLandLocation(event.getEntity().getLocation()))
		{
			event.setCancelled(true);
		}
		String godName = HolyLandManager.get().getGodAtHolyLandLocation(event.getEntity().getLocation());
		if (godName != null)
		{
			if ((event.getDamager() instanceof Player))
			{
				Player player = (Player) event.getDamager();

				String attackerGodName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
				if (attackerGodName == null)
				{
					if ((GodsConfiguration.get().isHolyLandLightning()) && ((event.getEntity() instanceof Player)))
					{
						GodManager.get().strikePlayerWithLightning(player.getUniqueId(), 3);
					}
					event.setCancelled(true);

					return;
				}
				if (!godName.equals(attackerGodName))
				{
					if ((GodsConfiguration.get().isHolyLandLightning()) && ((event.getEntity() instanceof Player)))
					{
						GodManager.get().strikePlayerWithLightning(player.getUniqueId(), 3);
					}
					if (!GodManager.get().hasWarRelation(godName, attackerGodName))
					{
						event.setCancelled(true);
					}
				}
				else if ((event.getEntity() instanceof Player))
				{
					GodManager.get().getGodPvP(godName);
				}
			}
			else if (((event.getDamager() instanceof LivingEntity)) && (!GodManager.get().getGodMobDamage(godName)))
			{
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void OnEntityDeath(EntityDeathEvent event)
	{
		if (!(event.getEntity().getKiller() instanceof Player))
		{
			return;
		}

		Player player = event.getEntity().getKiller();

		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());

		if (godName == null)
		{
			return;
		}

		// if (Gods.get().propheciesEnabled)
		// {
		// Gods.get().getProphecyManager().handleMobKill(player.getName(),
		// godName,
		// event.getEntityType().name());
		// }

		if (GodsConfiguration.get().isQuestsEnabled())
		{
			QuestManager.get().handleKilledMob(godName, event.getEntityType().name());
		}

		if (GodsConfiguration.get().isHolyArtifactsEnabled())
		{
			HolyArtifactManager.get().handleDeath(event.getEntity().getKiller().getName(), godName, event.getEntity().getKiller().getInventory().getItemInMainHand());
		}

		if ((!player.isOp()) && (!PermissionsManager.get().hasPermission(player, "gods.commandments")))
		{
			return;
		}

		GodManager.get().handleKilled(player, godName, event.getEntityType().name());
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event)
	{
		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}
		String targetLandGodName = HolyLandManager.get().getGodAtHolyLandLocation(event.getLocation());
		if (targetLandGodName != null)
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void OnPlayerConsume(PlayerItemConsumeEvent event)
	{
		Player player = event.getPlayer();

		String godName = BelieverManager.get().getGodForBeliever(event.getPlayer().getUniqueId());
		Material type = player.getInventory().getItemInMainHand().getType();
		if (godName != null)
		{
			Long lastEatTime = this.lastEatTimes.get(player.getName());
			Long currentTime = Long.valueOf(System.currentTimeMillis());
			if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
			{
				if ((GodsConfiguration.get().isCommandmentsEnabled()) && (player.getHealth() != player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()))
				{
					if ((player.isOp()) || (PermissionsManager.get().hasPermission(player, "gods.commandments")))
					{
						GodManager.get().handleEat(player, godName, type.name());
					}
				}
				if (GodsConfiguration.get().isQuestsEnabled())
				{
					QuestManager.get().handleEat(player.getName(), godName, type.name());
				}
				this.lastEatTimes.put(player.getName(), currentTime);
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event)
	{
		Player player = event.getEntity();
		if (!Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		String godName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		GodManager.GodType godType = GodManager.get().getDivineForceForGod(godName);

		GodManager.get().handleKilledPlayer(player.getUniqueId(), godName, godType);
		QuestManager.get().handleKilledPlayer(player.getUniqueId(), godName);

		double powerAfter = 0.0D;
		double powerBefore = 0.0D;
		if ((event.getEntity().getKiller() != null) && ((event.getEntity().getKiller() instanceof Player)))
		{
			Player killer = event.getEntity().getKiller();
			String killerGodName = BelieverManager.get().getGodForBeliever(killer.getUniqueId());
			if (killerGodName != null)
			{
				if (godName == null)
				{
					if (GodManager.get().getDivineForceForGod(killerGodName) == GodManager.GodType.WAR)
					{
						powerBefore = BelieverManager.get().getBelieverPower(player.getUniqueId());
						BelieverManager.get().increasePrayer(killer.getUniqueId(), killerGodName, 2);
						BelieverManager.get().increasePrayerPower(killer.getUniqueId(), 2);
						powerAfter = BelieverManager.get().getBelieverPower(player.getUniqueId());

						Gods.get().sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingHeathen, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
					}
				}
				else
				{
					List<String> warRelations = GodManager.get().getWarRelations(killerGodName);
					if (warRelations != null)
					{
						if (warRelations.contains(godName))
						{
							powerBefore = BelieverManager.get().getBelieverPower(player.getUniqueId());
							BelieverManager.get().increasePrayer(killer.getUniqueId(), killerGodName, 2);
							BelieverManager.get().increasePrayerPower(killer.getUniqueId(), 2);
							powerAfter = BelieverManager.get().getBelieverPower(player.getUniqueId());

							Gods.get().sendInfo(killer.getUniqueId(), LanguageManager.LANGUAGESTRING.YouEarnedPowerBySlayingEnemy, ChatColor.AQUA, (int) (powerAfter - powerBefore), killerGodName, 20);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void OnPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || !Gods.instance().isEnabledInWorld(player.getWorld()))
		{
			return;
		}
		if ((!player.isOp()) && (!PermissionsManager.get().hasPermission(player, "gods.altar.sacrifice")))
		{
			Gods.get().logDebug("OnPlayerDropItem(): Does not have gods.altar.sacrifice");
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE)
		{
			return;
		}
		AltarManager.get().addDroppedItem(event.getItemDrop().getEntityId(), player.getName());
		if (GodsConfiguration.get().isHolyArtifactsEnabled())
		{
			HolyArtifactManager.get().handleDrop(player.getName(), event.getItemDrop(), event.getItemDrop().getLocation());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		if ((player == null) || (!Gods.get().isEnabledInWorld(player.getWorld())))
		{
			return;
		}

		if (GodsConfiguration.get().isQuestsEnabled())
		{
			if (event.getAction() == Action.PHYSICAL)
			{
				if (QuestManager.get().handlePressurePlate(player.getUniqueId(), event.getClickedBlock()))
				{
					event.setCancelled(true);
				}
			}

			if (event.getClickedBlock() != null && QuestManager.get().handleOpenChest(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()))
			{
				event.setCancelled(true);
			}
		}

		if (GodsConfiguration.get().isBiblesEnabled())
		{
			if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
			{
				if ((event.getItem() != null) && (player.getInventory().getItemInMainHand() != null))
				{
					ItemStack book = player.getInventory().getItemInMainHand();
					if (book.getType() == Material.WRITTEN_BOOK)
					{
						String godName = HolyBookManager.get().getGodForBible(book);
						if (godName != null)
						{
							Long lastEatTime = this.lastEatTimes.get(player.getName());
							Long currentTime = Long.valueOf(System.currentTimeMillis());
							if ((lastEatTime == null) || (currentTime.longValue() - lastEatTime.longValue() > 10000L))
							{
								GodManager.get().handleReadBible(godName, player);
								QuestManager.get().handleReadBible(godName, player.getUniqueId());
								this.lastEatTimes.put(player.getName(), currentTime);
							}
						}
					}
				}
			}
			else if ((event.getAction().equals(Action.LEFT_CLICK_AIR)) || (event.getAction().equals(Action.LEFT_CLICK_BLOCK)))
			{
				if ((event.getItem() != null) && (player.getInventory().getItemInMainHand() != null))
				{
					ItemStack book = player.getInventory().getItemInMainHand();
					if (book.getType() == Material.WRITTEN_BOOK)
					{
						String godName = HolyBookManager.get().getGodForBible(book);
						if (godName != null)
						{
							GodManager.get().handleBibleMelee(godName, player);
							QuestManager.get().handleBibleMelee(godName, player.getUniqueId());
						}
					}
				}
			}
		}

		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}

		if (!PermissionsManager.get().hasPermission(event.getPlayer(), "gods.holyland"))
		{
			Gods.get().logDebug(event.getPlayer().getName() + " does not have holyland permission");
			return;
		}
		if ((!player.isOp()) && (HolyLandManager.get().isNeutralLandLocation(player.getLocation())))
		{
			if (!GodsConfiguration.get().isAllowInteractionInNeutralLands())
			{
				event.setCancelled(true);
			}
			return;
		}
		if (event.getClickedBlock() == null)
		{
			return;
		}
		String blockGodName = HolyLandManager.get().getGodAtHolyLandLocation(event.getClickedBlock().getLocation());
		if (blockGodName == null)
		{
			return;
		}
		if ((GodsConfiguration.get().getHolylandBreakableBlockTypes().contains(event.getClickedBlock().getType())) || (AltarManager.get().isAltarBlock(event.getClickedBlock())))
		{
			return;
		}
		if (HolyLandManager.get().isContestedLand(player.getLocation()))
		{
			player.sendMessage(ChatColor.RED + "This Holy Land is contested! Win the battle before you can access this Holy Land!");
			event.setCancelled(true);
			return;
		}
		String playerGodName = BelieverManager.get().getGodForBeliever(player.getUniqueId());
		if (playerGodName == null)
		{
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
		if (!playerGodName.equals(blockGodName))
		{
			if (GodManager.get().hasAllianceRelation(blockGodName, playerGodName))
			{
				return;
			}
			if (player.isOp())
			{
				return;
			}
			player.sendMessage(ChatColor.RED + "You do not have access to the holy land of " + ChatColor.GOLD + blockGodName);
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void OnPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		String godName = null;
		if ((player == null) || (!Gods.get().isEnabledInWorld(player.getWorld())))
		{
			return;
		}
		if ((event.getAction().equals(Action.RIGHT_CLICK_AIR)) || (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
		{
			Material type = player.getInventory().getItemInMainHand().getType();
			if ((type != null) && (type != Material.AIR))
			{
				godName = BelieverManager.get().getGodForBeliever(event.getPlayer().getUniqueId());
				if (godName != null)
				{
					if (GodsConfiguration.get().isHolyArtifactsEnabled())
					{
						HolyArtifactManager.get().handleActivate(player.getName(), player.getInventory().getItemInMainHand());
					}
				}
			}
		}
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
		{
			if (!AltarManager.get().isAltarSign(event.getClickedBlock()))
			{
				return;
			}

			BlockState state = event.getClickedBlock().getState();

			Sign sign = (Sign) state;
			if (GodsConfiguration.get().isCursingEnabled())
			{
				Player cursedPlayer = AltarManager.get().getCursedPlayerFromAltar(event.getClickedBlock(), sign.getLines());

				if (cursedPlayer != null)
				{
					if (GodManager.get().isPriest(player.getUniqueId()))
					{
						Player oldCursedPlayer = GodManager.get().getCursedPlayerForGod(godName);
						if ((oldCursedPlayer != null) && oldCursedPlayer == cursedPlayer)
						{
							GodManager.get().setCursedPlayerForGod(godName, null);

							LanguageManager.get().setPlayerName(cursedPlayer.getDisplayName());

							GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerUnset, 2);
							GodManager.get().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerUnset, cursedPlayer.getUniqueId());
						}
						else
						{
							GodManager.get().setCursedPlayerForGod(godName, cursedPlayer.getUniqueId());

							LanguageManager.get().setPlayerName(cursedPlayer.getDisplayName());

							GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestCursedPlayerSet, 2);
							GodManager.get().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversCursedPlayerSet, player.getUniqueId());

							Gods.get().log(player.getName() + " asked " + godName + " for curses on " + cursedPlayer);
						}
					}
					else
					{
						Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.CursesNotAllowed, ChatColor.DARK_RED, 0, "", 1);
					}

					return;
				}
			}
			if (GodsConfiguration.get().isBlessingEnabled())
			{
				Player blessedPlayer = AltarManager.get().getBlessedPlayerFromAltarSign(event.getClickedBlock(), sign.getLines());
				if (blessedPlayer != null)
				{
					if (GodManager.get().isPriest(player.getUniqueId()))
					{
						String oldBlessedPlayer = GodManager.get().getBlessedPlayerForGod(godName);
						if ((oldBlessedPlayer != null) && (oldBlessedPlayer.equals(blessedPlayer.getUniqueId().toString())))
						{
							GodManager.get().setBlessedPlayerForGod(godName, null);

							LanguageManager.get().setPlayerName(blessedPlayer.getDisplayName());

							GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerUnset, 2);

							GodManager.get().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerUnset, player.getUniqueId());
						}
						else
						{
							GodManager.get().setBlessedPlayerForGod(godName, blessedPlayer.getUniqueId());

							LanguageManager.get().setPlayerName(blessedPlayer.getDisplayName());

							GodManager.get().GodSay(godName, player, LanguageManager.LANGUAGESTRING.GodToPriestBlessedPlayerSet, 2);

							GodManager.get().GodSayToBelieversExcept(godName, LanguageManager.LANGUAGESTRING.GodToBelieversBlessedPlayerSet, player.getUniqueId());

							Gods.get().log(player.getName() + " asked " + godName + " for blessings on " + blessedPlayer);
						}
					}
					else
					{
						Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.BlessingsNotAllowed, ChatColor.DARK_RED, 0, "", 1);
					}
					return;
				}
			}

			if ((!event.getPlayer().isOp()) && (!PermissionsManager.get().hasPermission(event.getPlayer(), "gods.altar.pray")))
			{
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.AltarPrayingNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			Block block = event.getClickedBlock();

			if (!AltarManager.get().isPrayingAltar(block))
			{
				return;
			}

			godName = sign.getLine(2);

			if (godName == null)
			{
				return;
			}

			godName = godName.trim();

			if (godName.length() <= 1)
			{
				Gods.get().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.InvalidGodName, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			godName = GodManager.get().formatGodName(godName);

			if ((Gods.get().isBlacklistedGod(godName)) || (!Gods.get().isWhitelistedGod(godName)))
			{
				Gods.get().sendInfo(player.getUniqueId(), LanguageManager.LANGUAGESTRING.PrayToBlacklistedGodNotAllowed, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			if (!GodManager.get().hasGodAccess(player.getUniqueId(), godName))
			{
				Gods.get().sendInfo(event.getPlayer().getUniqueId(), LanguageManager.LANGUAGESTRING.PrivateGodNoAccess, ChatColor.DARK_RED, 0, "", 1);
				return;
			}

			if (GodManager.get().handleAltarPray(block.getLocation(), event.getPlayer(), godName))
			{
				Gods.get().log(event.getPlayer().getDisplayName() + " prayed to " + godName + " at an altar");
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		GodManager.get().updateOnlineGods();
	}

	@EventHandler
	public void OnPlayerPickupItem(EntityPickupItemEvent event)
	{
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity();

		if (player == null || !Gods.get().isEnabledInWorld(player.getWorld()))
		{
			return;
		}

		if (GodsConfiguration.get().isHolyArtifactsEnabled())
		{
			if (HolyArtifactManager.get().isHolyArtifact(event.getItem().getItemStack()))
			{
				if (HolyArtifactManager.get().hasHolyArtifact(player.getName()))
				{
					event.setCancelled(true);
					return;
				}
			}
		}

		if (GodsConfiguration.get().isMarriageEnabled())
		{
			MarriageManager.get().handlePickupItem(player, event.getItem(), event.getItem().getLocation());
		}

		if (GodsConfiguration.get().isQuestsEnabled())
		{
			QuestManager.get().handlePickupItem(player.getName(), event.getItem(), event.getItem().getLocation());
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();

		this.lastEatTimes.remove(player.getName());
		if (!GodsConfiguration.get().isHolyLandEnabled())
		{
			return;
		}
		HolyLandManager.get().handleQuit(player.getName());

		GodManager.get().updateOnlineGods();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void OnSignChange(SignChangeEvent event)
	{
		Player player = event.getPlayer();

		if ((player == null) || (!Gods.get().isEnabledInWorld(player.getWorld())))
		{
			return;
		}

		if ((GodsConfiguration.get().isCursingEnabled()) && (AltarManager.get().isCursingAltar(event.getBlock(), event.getLines())))
		{
			if (!AltarManager.get().handleNewCursingAltar(event))
			{
				ItemStack sign = new ItemStack(Material.SIGN);
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}

		if ((GodsConfiguration.get().isBlessingEnabled()) && (AltarManager.get().isBlessingAltar(event.getBlock(), event.getLines())))
		{
			if (!AltarManager.get().handleNewBlessingAltar(event))
			{
				ItemStack sign = new ItemStack(Material.SIGN);
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}

		if (AltarManager.get().isPrayingAltar(event.getBlock()))
		{
			if (!AltarManager.get().handleNewPrayingAltar(event))
			{
				ItemStack sign = new ItemStack(Material.SIGN);
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), sign);
			}
			return;
		}

		if (AltarManager.get().isRitualAltar(event.getBlock(), event.getLines()))
		{
			if (!AltarManager.get().handleNewRitualAltar(event))
			{
				ItemStack sign = new ItemStack(Material.SIGN);
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.SIGN, 1));
			}
			return;
		}
	}
}