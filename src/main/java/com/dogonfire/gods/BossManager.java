//package com.dogonfire.gods;
//
//import java.io.File;
//import java.util.*;
//import java.lang.reflect.Method;
//
//
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.LivingEntity;
//import org.bukkit.entity.EnderDragon;
//import org.bukkit.event.entity.CreatureSpawnEvent;
//
//import net.minecraft.server.v1_10_R1.EntityTypes;
//
//import org.bukkit.World;
//
//import org.bukkit.block.Block;
//import org.bukkit.configuration.file.FileConfiguration;
//import org.bukkit.configuration.file.YamlConfiguration;
//
//
//public class BossManager 
//{
//	private Gods plugin;
//	private HashMap<String, Entity> bossEntityID = new HashMap<String, Entity>();
//	private HashMap<String, DragonBoss> dragons = new HashMap<String, DragonBoss>();
//	private FileConfiguration bossConfig = null;
//	private File bossConfigFile = null;
//	private Random random = new Random();
//
//	enum BossType {
//		TITAN,
//		DRAGON
//	}
//	
//	private String generateDragonBossName()
//	{
//		String name = "";
//		
//		return name;
//	}
//	
//	
//	public BossManager(Gods plugin)
//	{
//		this.plugin = plugin;
//		
//		try
//	    {
//			Method method = EntityTypes.class.getDeclaredMethod("a", new Class[] { Class.class, String.class, Integer.TYPE });
//			method.setAccessible(true);
//			method.invoke(EntityTypes.class, new Object[] { DragonBoss.class, "DragonBoss", Integer.valueOf(63) });
//	    } 
//		catch (Exception e) 
//		{
//			plugin.logDebug("Error registering Dragon Entity!");
//			e.printStackTrace();
//			//pm.disablePlugin(this);
//	      
//			return;
//	    }
//	}
//	
//	public void disable()
//	{
//		/*
//		for (DragonBoss dragon : dragons.values()) 
//		{
//		      LivingEntity a = (LivingEntity)dragon.getBukkitEntity();
//		      a.remove();
//		}
//		*/							
//	}
//	
//	public void load() 
//	{
//		if (this.bossConfigFile == null) 
//		{
//			this.bossConfigFile = new File(this.plugin.getDataFolder(), "bosses.yml");
//		}
//
//		this.bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
//
//		this.plugin.log("Loaded " + this.bossConfig.getKeys(false).size() + " bosses.");
//	}
//
//	public void save() 
//	{
//		if (bossConfig == null || bossConfigFile == null) 
//		{
//			return;
//		}
//
//		try 
//		{
//			bossConfig.save(this.bossConfigFile);
//		} 
//		catch (Exception ex) 
//		{
//			plugin.log("Could not save config to " + this.bossConfigFile + ": " + ex.getMessage());
//		}
//	}
//	
//	public int createNewBoss(BossType type, String godName, Location location)
//	{		
//		//DragonBoss dragon = new DragonBoss(location, location.getWorld());
//	/*	
//		String bossName = generateDragonBossName();
//	
//	    net.minecraft.server.v1_4_R1.World notchWorld = ((CraftWorld)location.getWorld()).getHandle();
//	    DragonBoss dragonBoss = new DragonBoss(plugin, bossName, location, notchWorld);
//	    notchWorld.addEntity(dragonBoss, CreatureSpawnEvent.SpawnReason.CUSTOM);
//	    LivingEntity dragon = (LivingEntity)dragonBoss.getBukkitEntity();
//			    
//		save();
//		
//		bossEntityID.put(bossName, dragon);
//		
//		return dragon.getEntityId();
//		*/
//		return 0;
//	}
//	
//	public String getBossName()
//	{
//		return "Calgor The Destroyer";
//		
//	}
//	
//	private Location getDragonBossCaveLocation(World world, int minDist, int maxDist, Location center) 
//	{
//		int run = 0;
//
//		Block target = null;
//
//		List defaultspawnblocks = new ArrayList();
//		defaultspawnblocks.add(Material.STONE);
//		defaultspawnblocks.add(Material.SMOOTH_STONE);
//		defaultspawnblocks.add(Material.MOSSY_COBBLESTONE);
//		defaultspawnblocks.add(Material.OBSIDIAN);
//
//		//World world = this.plugin.getServer().getWorld(worldName);
//		int x;
//		int z;
//		int y;
//		
//		do 
//		{
//			run++;
//
//			int minLevel = 4;
//			int maxLevel = 50;
//			int maxLight = 4;
//			int minLight = 0;
//			do 
//			{
//				x = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockX();
//				z = this.random.nextInt(maxDist * 2) - maxDist + center.getBlockZ();
//			} while ((Math.abs(x - center.getBlockX()) < minDist) || (Math.abs(z - center.getBlockZ()) < minDist));
//			
//			do 
//			{
//				y = this.random.nextInt(maxLevel);
//			} while (y < minLevel);
//
//			target = world.getBlockAt(x, y, z);
//
//			if ((target.getType() == Material.AIR)
//					&& (world.getBlockAt(x, y + 1, z).getType() == Material.AIR)
//					&& (world.getBlockAt(x, y + 2, z).getType() == Material.AIR)
//					&& (world.getBlockAt(x, y + 3, z).getType() == Material.AIR)) 
//			{
//				target = world.getBlockAt(x, y + 2, z);
//
//				if ((defaultspawnblocks.contains(target.getType())) && (world.getHighestBlockAt(target.getLocation()).getType() != Material.WATER)) 
//				{
//					return target.getLocation();
//				}
//			}
//
//		} while (run < 100);
//	
//		plugin.log("Could not find a Dragon Boss cave location " + world.getName());
//		return null;
//	}
//	
//	public void removeDragons()
//	{
//		for(org.bukkit.World world : plugin.getServer().getWorlds())
//		{
//			removeDragons(world);
//		}
//	}
//	
//	public void removeDragons(org.bukkit.World world)
//	{
//		if (world == null) 
//		{
//			return;
//	    }
//	    
//		int passed = 0;
//
//	    for (Entity entity : world.getEntities())
//	    {
//	    	if (entity instanceof EnderDragon)
//	    	{
//	    		entity.remove();
//	    		passed++;
//	    	}
//	    }
//	    
//	    world.save();
//	    
//	    plugin.logDebug("Removed " + passed + " boss dragon(s)");
//	}
//
//	void detectPlayer(DragonBoss dragon)
//	{
//		
//	}
//	
//	public void setNewTargetForDragon(DragonBoss dragon)
//	{
//		dragon.startX = dragon.locX;
//		dragon.startY = dragon.locY;
//		dragon.startZ = dragon.locZ;
//			
//		dragon.toX = random.nextInt(2001) - 1000; 
//		dragon.toZ = random.nextInt(2001) - 1000; 
//		
//		if(random.nextInt(3)==0)
//		{
//			//dragon.toY = dragon.world.getHighestBlockYAt((int)dragon.toX, (int)dragon.toZ) + 10;
//			World world = dragon.world.getWorld();
//			
//			Location location = getDragonBossCaveLocation(world, 200, 1000, new Location(world, 0, 0, 0));
//			
//			if(location!=null)
//			{
//				dragon.toY = location.getY();
//			}
//		}
//		else
//		{
//			dragon.toY = 100;			
//		}
//		
//		System.out.println("NewTarget " + dragon.toX + "," + dragon.toY + "," + dragon.toZ);
//		
//		double yaw = dragon.getCorrectYaw(dragon.toX, dragon.toZ);		
//		
//		dragon.setupMove();
//		
//		dragon.finalmove = false;
//		
//		bossConfig.set(dragon.getName() + "TargetPosition.X", dragon.toX);
//		
//	}
//	
//	public void dragonUpdate(DragonBoss dragon) 
//	{
//		Entity entity = dragon.getBukkitEntity();
//
//		if (entity.getPassenger() == null) 
//		{
//			// return;
//		}
//
//		double myX = dragon.locX;
//		double myY = dragon.locY;
//		double myZ = dragon.locZ;
//
//		if (dragon.finalmove) 
//		{
//			if ((int) dragon.locY > (int) dragon.toY) 
//			{
//				myY -= dragon.speed;
//			} 
//			else if ((int) dragon.locY < (int) dragon.toY) 
//			{
//				myY += dragon.speed;
//			} 
//			else 
//			{
//				//Travels.removePlayerandDragon(entity);
//				if(random.nextInt(100)==0)	
//				{
//					setNewTargetForDragon(dragon);
//				}
//				else
//				{
//					detectPlayer(dragon);
//				}
//			}
//
//			//System.out.println("Finalmove : " + myX + " " + myY + " " + myZ);
//			
//			dragon.setPosition(myX, myY, myZ);
//			return;
//		}
//
//		if ((int) dragon.locY < dragon.maxY) 
//		{
//			myY += dragon.speed;
//		}
//
//		if (myX < dragon.toX)
//		{
//			myX += dragon.XTick;
//		}
//		else 
//		{
//			myX -= dragon.XTick;
//		}
//
//		if (myZ < dragon.toZ)
//		{
//			myZ += dragon.ZTick;
//		}
//		else 
//		{
//			myZ -= dragon.ZTick;
//		}
//
//		if (Math.abs(myZ - dragon.toZ) < 50 && Math.abs(myX - dragon.toX) < 50) 
//		{
//			dragon.finalmove = true;
//		}
//
//		System.out.println("Normal move : " + myX + " " + myY + " " + myZ);
//
//		dragon.setPosition(myX, myY, myZ);
//	}
//
//	/*
//	public void titanUpdate(TitanBoss titan) 
//	{
//		Entity entity = titan.getBukkitEntity();
//
//		if (entity.getPassenger() == null) 
//		{
//			// return;
//		}
//
//		double myX = titan.locX;
//		double myY = titan.locY;
//		double myZ = titan.locZ;
//
//		if ((int) titan.locY < titan.maxY) 
//		{
//			myY += titan.speed;
//		}
//
//		if (myX < titan.toX)
//		{
//			myX += titan.XTick;
//		}
//		else 
//		{
//			myX -= titan.XTick;
//		}
//
//		if (myZ < titan.toZ)
//		{
//			myZ += titan.ZTick;
//		}
//		else 
//		{
//			myZ -= titan.ZTick;
//		}
//
//		if (Math.abs(myZ - titan.toZ) < 50 && Math.abs(myX - titan.toX) < 50) 
//		{
//			titan.finalmove = true;
//		}
//
//		//System.out.println("Normal move : " + myX + " " + myY + " " + myZ);
//
//		titan.setPosition(myX, myY, myZ);
//	}
//	*/
//}
