package com.gmail.buzziespy.MCInfection;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Utils {
    MCInfection plugin;
    Configuration config;
    
    public Utils(MCInfection plugin) {
        this.plugin = plugin;
        config = plugin.config;
    }
    
    //checks if all spawn locations are set
    public boolean isGameSetUp() {
        if(plugin.config.SPAWN_HUMAN == null || plugin.config.SPAWN_HUMAN.isEmpty())
            return false;
        else if(plugin.config.SPAWN_HUMAN_HOLD == null)
            return false;
        else if(plugin.config.SPAWN_LEAVE == null)
            return false;
        else if(plugin.config.SPAWN_WAIT == null)
            return false;
        else if(plugin.config.SPAWN_ZOMBIE == null || plugin.config.SPAWN_ZOMBIE.isEmpty())
            return false;
        else if(plugin.config.SPAWN_ZOMBIE_HOLD == null)
            return false;
        else
            return true;
    }
    
    //checks if all team loadouts are set
    public boolean isLoadoutSetUp() {
        if(plugin.config.LOADOUT_HUMAN_ARMOR == null)
            return false;
        else if(plugin.config.LOADOUT_HUMAN_INVEN == null)
            return false;
        else if(plugin.config.LOADOUT_HUMAN_POTIONS == null)
            return false;
        else if(plugin.config.LOADOUT_ZOMBIE_ARMOR == null)
            return false;
        else if(plugin.config.LOADOUT_ZOMBIE_INVEN == null)
            return false;
        else if(plugin.config.LOADOUT_ZOMBIE_POTIONS == null)
            return false;
        else
            return true;
    }
    
    //Displays teams in console.  (Console does use this)
    /*
    public void rosterReport()
    {
        plugin.getLogger().info("Waiting team");
        for (String name: config.TEAM_WAITING)
        {
            plugin.getLogger().info(name);
        }
        plugin.getLogger().info("Human team");
        for (String name: config.TEAM_HUMAN)
        {
                plugin.getLogger().info(name);
        }
        plugin.getLogger().info("Zombie team");
        for (String name: config.TEAM_ZOMBIE)
        {
                plugin.getLogger().info(name);
        }
    }*/
    
    //Displays teams and their rosters in a message format
    //Does not currently support command blocks calling this information
    public void rosterReport(CommandSender cs)
    {
        String s = "";
        s += config.HUMAN_TEXT + "Humans: " + ChatColor.LIGHT_PURPLE;
        for (String name: config.TEAM_HUMAN)
        {
                s += name + " ";
        }
        s += "\n";

        s += config.ZOMBIE_TEXT + "Zombies: " + ChatColor.AQUA;
        for (String name: config.TEAM_ZOMBIE)
        {
                s += name + " ";
        }
        s += "\n";
        
        s += config.WAITING_TEXT + "Waiting: " + ChatColor.YELLOW;
        for (String name: config.TEAM_WAITING)
        {
                s += name + " ";
        }
        s += "\n";
        
        //"You are on the <> team" addition for players
        if (cs instanceof Player)
        {
        	Player p = (Player)cs;
	        if (isHuman(p))
	        {
	            s += config.HUMAN_TEXT + "You are on the HUMAN team.";
	        }
	        else if (isZombie(p))
	        {
	            s += config.ZOMBIE_TEXT + "You are on the ZOMBIE team.";
	        }
	        else if (isWaiting(p))
	        {
	            s += ChatColor.GOLD + "You are WAITING TO PLAY.";
	        }
        }

        cs.sendMessage(s);
    }

    public boolean isWaiting(Player p)
    {
        if (config.TEAM_WAITING.contains(p.getName()))
        {
            return true;
        }
        return false;
    }

    public boolean isHuman(Player p)
    {
        if (config.TEAM_HUMAN.contains(p.getName()))
        {
            return true;
        }
        return false;
    }

    public boolean isZombie(Player p)
    {
        if (config.TEAM_ZOMBIE.contains(p.getName()))
        {
            return true;
        }
        return false;
    }

    public boolean isInGame(Player p)
    {
        return (isZombie(p)||isHuman(p)||isWaiting(p));
    }
    
    //Returns a Location object after retrieving location info given at config address loc
    public Location locFromString(String loc) {
        if(loc == null) {
            return null;
        }
        String[] coords = loc.split(":");
        double x = Double.valueOf(coords[0]);
        double y = Double.valueOf(coords[1]);
        double z = Double.valueOf(coords[2]);
        float yaw = Float.valueOf(coords[3]);
        float pitch = Float.valueOf(coords[4]);
        World world = plugin.getServer().getWorld(coords[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    //Given a Location object, return the coords in String form
    //Coordinates currently rounded to make them easier to read for players
    //Removed Math.round() on X/Y/Z to prevent whole number coords from being stored in config (and read from it)
    public String locToString(Location loc) {
        if(loc == null) {
            return null;
        }
        return (loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + Math.round(loc.getYaw()) + ":" + Math.round(loc.getPitch()) + ":" + loc.getWorld().getName());
    }
    
    public List<Location> locListFromStrings(List<?> stringLocs) {
        if(stringLocs == null) {
            return null;
        }
        List<Location> locations = new ArrayList<>();
        for(String loc : (List<String>)stringLocs) {
            locations.add(locFromString(loc));
        }
        return locations;
    }
    
    public List<String> locListToStrings(List<Location> locList) {
        if(locList == null) {
            return null;
        }
        List<String> locations = new ArrayList<>();
        for(Location loc : locList) {
            locations.add(locToString(loc));
        }
        return locations;
    }
    
    public Location getRandomSpawn(List<Location> spawns) {
        if(spawns == null || spawns.isEmpty()) {
            return null;
        } else {
            return spawns.get((int)(Math.random() * spawns.size()));
        }
    }
    
    //Loadout application - for now always completely wipe inventory/armor/potions of player before actually applying loadout
    public void applyHumanLoadout(Player p) {
    	//wipe inventory/armor/potions
    	p.getInventory().clear();
    	p.getInventory().setArmorContents(null);
    	for (PotionEffect pe: p.getActivePotionEffects())
    	{
    		p.removePotionEffect(pe.getType());
    	}
    	
    	//apply loadout from config
        if(config.LOADOUT_HUMAN_INVEN != null && config.LOADOUT_HUMAN_INVEN.size() > 0) {
            p.getInventory().setContents(config.LOADOUT_HUMAN_INVEN.toArray(new ItemStack[config.LOADOUT_HUMAN_INVEN.size()]));
        }
        
        if(config.LOADOUT_HUMAN_ARMOR != null && config.LOADOUT_HUMAN_ARMOR.size() > 0) {
            p.getInventory().setArmorContents(config.LOADOUT_HUMAN_ARMOR.toArray(new ItemStack[config.LOADOUT_HUMAN_ARMOR.size()]));
        }
        
        if(config.LOADOUT_HUMAN_POTIONS != null && config.LOADOUT_HUMAN_POTIONS.size() > 0) {
            for(PotionEffect effect : config.LOADOUT_HUMAN_POTIONS) {
                p.addPotionEffect(new PotionEffect(effect.getType(), 7200*20, effect.getAmplifier()));
            }
        }
    }
    
    public void applyZombieLoadout(Player p) {
    	//wipe inventory/armor/potions
    	p.getInventory().clear();
    	p.getInventory().setArmorContents(null);
    	for (PotionEffect pe: p.getActivePotionEffects())
    	{
    		p.removePotionEffect(pe.getType());
    	}
    	
    	//apply loadout from config
        if(config.LOADOUT_ZOMBIE_INVEN != null && config.LOADOUT_ZOMBIE_INVEN.size() > 0) {
            p.getInventory().setContents(config.LOADOUT_ZOMBIE_INVEN.toArray(new ItemStack[config.LOADOUT_ZOMBIE_INVEN.size()]));
        }
        
        if(config.LOADOUT_ZOMBIE_ARMOR != null && config.LOADOUT_ZOMBIE_ARMOR.size() > 0) {
            p.getInventory().setArmorContents(config.LOADOUT_ZOMBIE_ARMOR.toArray(new ItemStack[config.LOADOUT_ZOMBIE_ARMOR.size()]));
        }
        
        if(config.LOADOUT_ZOMBIE_POTIONS != null && config.LOADOUT_ZOMBIE_POTIONS.size() > 0) {
            for(PotionEffect effect : config.LOADOUT_ZOMBIE_POTIONS) {
                p.addPotionEffect(new PotionEffect(effect.getType(), 7200*20, effect.getAmplifier()));
            }
        }
    }
}