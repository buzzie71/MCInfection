package com.gmail.buzziespy.MCInfection;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Configuration {
    MCInfection plugin;
    
    //Game constants
    public short HUMAN_WOOL = (short)14;
    public String HUMAN_TEXT = ChatColor.GREEN + "";
    public short ZOMBIE_WOOL = (short)5;
    public String ZOMBIE_TEXT = ChatColor.RED + "";
    
    //Game configurables
    public int GAME_TIME;
    public boolean FRIENDLY_FIRE;
    public int ZOMBIE_RESPAWN;
    public int HUMAN_RESPAWN;
    public List<String> TEAM_ZOMBIE;
    public List<String> TEAM_HUMAN;
    public List<String> TEAM_WAITING;
    public Location SPAWN_ZOMBIE;
    public Location SPAWN_HUMAN;
    public Location SPAWN_ZOMBIE_HOLD;
    public Location SPAWN_HUMAN_HOLD;
    public Location SPAWN_LEAVE;
    public Location SPAWN_WAIT;
    public List<ItemStack> LOADOUT_ZOMBIE;
    public List<ItemStack> LOADOUT_HUMAN;
    public List<PotionEffect> POTIONS_ZOMBIE;
    public List<PotionEffect> POTIONS_HUMAN;
    
    public Configuration(MCInfection plugin) {
        this.plugin = plugin;
    }
    
    public void save() {
        reload();
        //push any values into the config that have been changed.
        plugin.saveConfig();
    }
    
    public void load() {
        plugin.reloadConfig();
        
        GAME_TIME = plugin.getConfig().getInt("game.time", 60);
        FRIENDLY_FIRE = plugin.getConfig().getBoolean("game.friendly-fire", false);
        ZOMBIE_RESPAWN = plugin.getConfig().getInt("game.zombie-respawn", 5);
        HUMAN_RESPAWN = plugin.getConfig().getInt("game.human-respawn", 5);
        TEAM_ZOMBIE = (List<String>)plugin.getConfig().getList("team.zombie");
        TEAM_HUMAN = (List<String>)plugin.getConfig().getList("team.human");
        TEAM_WAITING = (List<String>)plugin.getConfig().getList("team.waiting");
        //Load Values
    }
    
    public void reload() {
        //cache any values that need to be stored over a fresh reload from config
        load();
        //Set those values back
    }
}
