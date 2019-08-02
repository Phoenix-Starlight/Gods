//package com.dogonfire.gods;
//
//import net.minecraft.server.v1_5_R2.EntityEnderDragon;
//import net.minecraft.server.v1_5_R2.World;
//
//import org.bukkit.Location;
//import org.bukkit.entity.Entity;
//
//public class DragonBoss extends EntityEnderDragon 
//{
//	private Gods plugin = null;
//	private String name = null;
//	
//	public double toX = 50;
//	public double toY = 50;
//	public double toZ = 50;
//	public int maxY;
//	public boolean finalmove = false;
//
//	public double XTick;
//	public double YTick;
//	public double ZTick;
//	public double distanceX;
//	public double distanceY;
//	public double distanceZ;
//	public double startX;
//	public double startY;
//	public double startZ;
//	
//	//The number represents the blocks in 1 server tick, 20 ticks = 1 second
//	//Do not set it over 1.0!
//	//DragonSpeed: 0.5	
//	public double speed = 0.5;
//	
//	Entity entity;
//
//	public DragonBoss(Gods plugin, String name, Location location, World world) 
//	{		
//		super(world);
//		
//		this.plugin = plugin;
//		this.name = name;
//		
//		location.setY(world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ()) + 20);
//		
//		this.maxY = 100;
//	  		
//		setPosition(location.getX(), location.getY(), location.getZ());
//
//		this.yaw = (location.getYaw() + 180.0F);
//
//		while (this.yaw > 360.0F)
//			this.yaw -= 360.0F;
//
//		while (this.yaw < 0.0F)
//			this.yaw += 360.0F;
//
//		if ((this.yaw < 45.0F) || (this.yaw > 315.0F))
//			this.yaw = 0.0F;
//		else if (this.yaw < 135.0F)
//			this.yaw = 90.0F;
//		else if (this.yaw < 225.0F)
//			this.yaw = 180.0F;
//		else
//			this.yaw = 270.0F;
//		
//		setNewTarinstance();
//	
//	}
//	
//	public String getName()
//	{
//		return name;
//	}
//	
//	public void setNewTarinstance()
//	{
//		startX = this.locX;
//		startY = this.locY;
//		startZ = this.locZ;
//			
//		toX = random.nextInt(2001) - 1000; 
//		toZ = random.nextInt(2001) - 1000; 
//		
//		if(random.nextInt(3)==0)
//		{
//			toY = world.getHighestBlockYAt((int)toX, (int)toZ) + 10;
//		}
//		else
//		{
//			toY = 100;			
//		}
//		
//		System.out.println("NewTarget " + toX + "," + toY + "," + toZ);
//		
//		yaw = getCorrectYaw(toX, toZ);		
//		
//		setupMove();
//		
//		finalmove = false;
//	}
//
//	public DragonBoss(World world) 
//	{
//		super(world);
//	}
//
//	public void setupMove() 
//	{
//		this.distanceX = (this.startX - this.toX);
//		this.distanceY = (this.startY - this.toY);
//		this.distanceZ = (this.startZ - this.toZ);
//		
//		double tick = Math.sqrt(this.distanceX * this.distanceX + this.distanceY * this.distanceY + this.distanceZ * this.distanceZ) / speed;
//		
//		this.XTick = (Math.abs(this.distanceX) / tick);
//		this.ZTick = (Math.abs(this.distanceZ) / tick);
//	}
//
//	public float getCorrectYaw(double targetx, double targetz)
//	{
//		if (this.locZ > targetz)
//		{
//			return (float)-Math.toDegrees(Math.atan((this.locX - targetx) / (this.locZ - targetz)));
//		}
//		
//	    if (this.locZ < targetz) 
//	    {
//	    	return (float)-Math.toDegrees(Math.atan((this.locX - targetx) / (this.locZ - targetz))) + 180.0F;
//	    }
//	    
//	    return this.yaw;	
//	}
//	  
//	public void c() 
//	{
//		plugin.getBossManager().dragonUpdate(this);		
//	}
///*
//	public void travel() 
//	{
//		Entity entity = getBukkitEntity();
//
//		if (entity.getPassenger() == null) 
//		{
//			// return;
//		}
//
//		double myX = this.locX;
//		double myY = this.locY;
//		double myZ = this.locZ;
//
//		if (this.finalmove) 
//		{
//			if ((int) this.locY > (int) this.toY) 
//			{
//				myY -= speed;
//			} 
//			else if ((int) this.locY < (int) this.toY) 
//			{
//				myY += speed;
//			} 
//			else 
//			{
//				//Travels.removePlayerandDragon(entity);
//				if(random.nextInt(100)==0)	
//				{
//					setNewTarinstance();
//					return;
//				}
//				else
//				{
//					detectPlayer(dragon);
//				}
//			}
//
//			System.out.println("Finalmove : " + myX + " " + myY + " " + myZ);
//			
//			setPosition(myX, myY, myZ);
//			return;
//		}
//
//		if ((int) this.locY < this.maxY) 
//		{
//			myY += speed;
//		}
//
//		if (myX < this.toX)
//		{
//			myX += this.XTick;
//		}
//		else 
//		{
//			myX -= this.XTick;
//		}
//
//		if (myZ < this.toZ)
//		{
//			myZ += this.ZTick;
//		}
//		else 
//		{
//			myZ -= this.ZTick;
//		}
//
//		if (Math.abs(myZ - this.toZ) < 50 && Math.abs(myX - this.toX) < 50) 
//		{
//			this.finalmove = true;
//		}
//
//		System.out.println("Normal move : " + myX + " " + myY + " " + myZ);
//
//		setPosition(myX, myY, myZ);
//	}
//	*/
//}