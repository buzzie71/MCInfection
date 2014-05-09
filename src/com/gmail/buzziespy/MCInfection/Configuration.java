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
    public List<Location> SPAWN_ZOMBIE;
    public List<Location> SPAWN_HUMAN;
    public Location SPAWN_ZOMBIE_HOLD;
    public Location SPAWN_HUMAN_HOLD;
    public Location SPAWN_LEAVE;
    public Location SPAWN_WAIT;
    public List<ItemStack> LOADOUT_ZOMBIE_INVEN;
    public List<ItemStack> LOADOUT_ZOMBIE_ARMOR;
    public List<PotionEffect> LOADOUT_ZOMBIE_POTIONS;
    public List<ItemStack> LOADOUT_HUMAN_INVEN;
    public List<ItemStack> LOADOUT_HUMAN_ARMOR;
    public List<PotionEffect> LOADOUT_HUMAN_POTIONS;
    
    public Configuration(MCInfection plugin) {
        this.plugin = plugin;
        utils = plugin.utils;
    }
    
    public void save() {
        reload();
        
        plugin.getConfig().set("team.zombie", TEAM_ZOMBIE);
        plugin.getConfig().set("team.human", TEAM_HUMAN);
        plugin.getConfig().set("team.waiting", TEAM_WAITING);
        plugin.getConfig().set("spawn.zombie", utils.locListToStrings(SPAWN_ZOMBIE));
        plugin.getConfig().set("spawn.human", utils.locListToStrings(SPAWN_HUMAN));
        plugin.getConfig().set("spawn.zombie-hold", utils.locToString(SPAWN_ZOMBIE_HOLD));
        plugin.getConfig().set("spawn.human-hold", utils.locToString(SPAWN_HUMAN_HOLD));
        plugin.getConfig().set("spawn.leave", utils.locToString(SPAWN_LEAVE));
        plugin.getConfig().set("spawn.wait", utils.locToString(SPAWN_WAIT));
        plugin.getConfig().set("loadout.zombie.inven", LOADOUT_ZOMBIE_INVEN);
        plugin.getConfig().set("loadout.zombie.armor", LOADOUT_ZOMBIE_ARMOR);
        plugin.getConfig().set("loadout.zombie.potions", LOADOUT_ZOMBIE_POTIONS);
        plugin.getConfig().set("loadout.human.inven", LOADOUT_HUMAN_INVEN);
        plugin.getConfig().set("loadout.human.armor", LOADOUT_HUMAN_ARMOR);
        plugin.getConfig().set("loadout.human.potions", LOADOUT_HUMAN_POTIONS);
                
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
        SPAWN_ZOMBIE = utils.locListFromStrings(plugin.getConfig().getList("spawn.zombie"));
        SPAWN_HUMAN = utils.locListFromStrings(plugin.getConfig().getList("spawn.human"));
        SPAWN_ZOMBIE_HOLD = utils.locFromString(plugin.getConfig().getString("spawn.zombie-hold"));
        SPAWN_HUMAN_HOLD = utils.locFromString(plugin.getConfig().getString("spawn.human-hold"));
        SPAWN_LEAVE = utils.locFromString(plugin.getConfig().getString("spawn.leave"));
        SPAWN_WAIT = utils.locFromString(plugin.getConfig().getString("spawn.wait"));
        LOADOUT_ZOMBIE_INVEN = (List<ItemStack>)plugin.getConfig().getList("loadout.zombie.inven");
        LOADOUT_ZOMBIE_ARMOR = (List<ItemStack>)plugin.getConfig().getList("loadout.zombie.armor");
        LOADOUT_ZOMBIE_POTIONS = (List<PotionEffect>)plugin.getConfig().getList("loadout.zombie.potions");
        LOADOUT_HUMAN_INVEN = (List<ItemStack>)plugin.getConfig().getList("loadout.human.inven");
        LOADOUT_HUMAN_ARMOR = (List<ItemStack>)plugin.getConfig().getList("loadout.human.armor");
        LOADOUT_HUMAN_POTIONS = (List<PotionEffect>)plugin.getConfig().getList("loadout.human.potions");
    }
    
    public void reload() {
        //cache any values that need to be stored over a fresh reload from config
        List<String> zombieTeam = TEAM_ZOMBIE;
        List<String> humanTeam = TEAM_HUMAN;
        List<String> waitingTeam = TEAM_WAITING;
        List<Location> zombieSpawn = SPAWN_ZOMBIE;
        List<Location> humanSpawn = SPAWN_HUMAN;
        Location holdZombieSpawn = SPAWN_ZOMBIE_HOLD;
        Location holdHumanSpawn = SPAWN_HUMAN_HOLD;
        Location leaveSpawn = SPAWN_LEAVE;
        Location waitSpawn = SPAWN_WAIT;
        List<ItemStack> zombieLoadoutInven = LOADOUT_ZOMBIE_INVEN;
        List<ItemStack> zombieLoadoutArmor = LOADOUT_ZOMBIE_ARMOR;
        List<PotionEffect> zombieLoadoutPotions = LOADOUT_ZOMBIE_POTIONS;
        List<ItemStack> humanLoadoutInven = LOADOUT_HUMAN_INVEN;
        List<ItemStack> humanLoadoutArmor = LOADOUT_HUMAN_ARMOR;
        List<PotionEffect> humanLoadoutPotions = LOADOUT_HUMAN_POTIONS;
        
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
        LOADOUT_ZOMBIE_INVEN = zombieLoadoutInven;
        LOADOUT_ZOMBIE_ARMOR = zombieLoadoutArmor;
        LOADOUT_HUMAN_INVEN = humanLoadoutInven;
        LOADOUT_HUMAN_ARMOR = humanLoadoutArmor;
        LOADOUT_ZOMBIE_POTIONS = zombieLoadoutPotions;
        LOADOUT_HUMAN_POTIONS = humanLoadoutPotions;
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
