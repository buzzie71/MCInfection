package com.gmail.buzziespy.MCInfection;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Configuration {
    MCInfection plugin;
    Utils utils;
    
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
        utils = plugin.utils;
    }
    
    public void save() {
        reload();
        
        plugin.getConfig().set("team.zombie", TEAM_ZOMBIE);
        plugin.getConfig().set("team.human", TEAM_HUMAN);
        plugin.getConfig().set("team.waiting", TEAM_WAITING);
        plugin.getConfig().set("spawn.zombie", SPAWN_ZOMBIE);
        plugin.getConfig().set("spawn.human", SPAWN_HUMAN);
        plugin.getConfig().set("spawn.zombie-hold", SPAWN_ZOMBIE_HOLD);
        plugin.getConfig().set("spawn.human-hold", SPAWN_HUMAN_HOLD);
        plugin.getConfig().set("spawn.leave", SPAWN_LEAVE);
        plugin.getConfig().set("spawn.wait", SPAWN_WAIT);
        plugin.getConfig().set("loadout.zombie", LOADOUT_ZOMBIE);
        plugin.getConfig().set("loadout.human", LOADOUT_HUMAN);
        plugin.getConfig().set("potions.zombie", POTIONS_ZOMBIE);
        plugin.getConfig().set("potions.human", POTIONS_HUMAN);
                
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
        SPAWN_ZOMBIE = utils.locFromString(plugin.getConfig().getString("spawn.zombie"));
        SPAWN_HUMAN = utils.locFromString(plugin.getConfig().getString("spawn.human"));
        SPAWN_ZOMBIE_HOLD = utils.locFromString(plugin.getConfig().getString("spawn.zombie-hold"));
        SPAWN_HUMAN_HOLD = utils.locFromString(plugin.getConfig().getString("spawn.human-hold"));
        SPAWN_LEAVE = utils.locFromString(plugin.getConfig().getString("spawn.leave"));
        SPAWN_WAIT = utils.locFromString(plugin.getConfig().getString("spawn.wait"));
        LOADOUT_ZOMBIE = (List<ItemStack>)plugin.getConfig().getList("loadout.zombie");
        LOADOUT_HUMAN = (List<ItemStack>)plugin.getConfig().getList("loadout.human");
        POTIONS_ZOMBIE = (List<PotionEffect>)plugin.getConfig().getList("potions.zombie");
        POTIONS_HUMAN = (List<PotionEffect>)plugin.getConfig().getList("potions.human");
    }
    
    public void reload() {
        //cache any values that need to be stored over a fresh reload from config
        List<String> zombieTeam = TEAM_ZOMBIE;
        List<String> humanTeam = TEAM_HUMAN;
        List<String> waitingTeam = TEAM_WAITING;
        Location zombieSpawn = SPAWN_ZOMBIE;
        Location humanSpawn = SPAWN_HUMAN;
        Location holdZombieSpawn = SPAWN_ZOMBIE_HOLD;
        Location holdHumanSpawn = SPAWN_HUMAN_HOLD;
        Location leaveSpawn = SPAWN_LEAVE;
        Location waitSpawn = SPAWN_WAIT;
        List<ItemStack> zombieLoadout = LOADOUT_ZOMBIE;
        List<ItemStack> humanLoadout = LOADOUT_HUMAN;
        List<PotionEffect> zombiePotions = POTIONS_ZOMBIE;
        List<PotionEffect> humanPotions = POTIONS_HUMAN;
        
        load();
        
        TEAM_ZOMBIE = zombieTeam;
        TEAM_HUMAN = humanTeam;
        TEAM_WAITING = waitingTeam;
        SPAWN_ZOMBIE = zombieSpawn;
        SPAWN_HUMAN = humanSpawn;
        SPAWN_ZOMBIE_HOLD = holdZombieSpawn;
        SPAWN_HUMAN_HOLD = holdHumanSpawn;
        SPAWN_LEAVE = leaveSpawn;
        SPAWN_WAIT = waitSpawn;
        LOADOUT_ZOMBIE = zombieLoadout;
        LOADOUT_HUMAN = humanLoadout;
        POTIONS_ZOMBIE = zombiePotions;
        POTIONS_HUMAN = humanPotions;
    }
    
    public String[] printCurrentSettings() {
        List<String> settings = new ArrayList<>();
        settings.add(ChatColor.RED + "-=/" + ChatColor.GREEN + "Current Infection Config" + ChatColor.RED + "\\=-");
        
        settings.add(ChatColor.AQUA + "Game Time: " + GAME_TIME);
        settings.add(ChatColor.AQUA + "Friendly Fire: " + statusCheck(FRIENDLY_FIRE));
        settings.add(ChatColor.AQUA + "Zombie Respawn: " + ZOMBIE_RESPAWN);
        settings.add(ChatColor.AQUA + "Human Respawn: " + HUMAN_RESPAWN);
        
        return settings.toArray(new String[settings.size()]);
    }
    
    public String statusCheck(Boolean toCheck) {
        if(toCheck)
            return (ChatColor.GREEN + "Enabled");
        else
            return (ChatColor.GRAY + "Disabled");
    }
}
