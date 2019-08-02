package com.dogonfire.gods;

import java.io.File;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatManager
{
  private Gods plugin;
  private String chatTag = "[GOD]";
  private String playerChatFormat = "";
  private FileConfiguration chatConfig;
  private File chatConfigFile;
  
  ChatManager(Gods plugin)
  {
    this.plugin = plugin;
  }
  
  public void load() 
  {
	  
  }
  
  public void save()
  {
    if ((this.chatConfig == null) || (this.chatConfigFile == null)) 
    {
      return;
    }
    try
    {
      this.chatConfig.save(this.chatConfigFile);
    }
    catch (Exception ex)
    {
      this.plugin.log("Could not save config to " + this.chatConfigFile.getName() + ": " + ex.getMessage());
    }
  }
  
  public String addColor(String string)
  {
    return ChatColor.translateAlternateColorCodes('&', string);
  }
  
  public String formatChat(Player player, String godName, String message)
  {
    if (godName != null) 
    {
      this.playerChatFormat = message.replace(this.chatTag, godName + " ");
    } 
    else 
    {
      this.playerChatFormat = message.replace(this.chatTag, "");
    }
    return this.playerChatFormat;
  }
}