package com.gmail.buzziespy.MCInfection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Utils {
    MCInfection plugin;
    Configuration config;
    
    public Utils(MCInfection plugin) {
        this.plugin = plugin;
        config = plugin.config;
    }
    
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
    }
    
    public void playerRosterReport(Player p)
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

        for (String name: config.TEAM_WAITING)
        {
                s += name + " ";
        }
        s += "\n";



        //"You are on the <> team"
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
                s += ChatColor.RED + "You are on the SPECTATOR team and " + ChatColor.YELLOW + "WAITING TO PLAY.";
        }



        p.sendMessage(s);
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
    
    public Location locFromString(String loc) {
        String[] coords = loc.split(":");
        double x = Double.valueOf(coords[0]);
        double y = Double.valueOf(coords[1]);
        double z = Double.valueOf(coords[2]);
        float yaw = Float.valueOf(coords[3]);
        float pitch = Float.valueOf(coords[4]);
        World world = plugin.getServer().getWorld(coords[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    public String locToString(Location loc) {
        return (loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ() + ":" + Math.round(loc.getYaw()) + ":" + Math.round(loc.getPitch()) + ":" + loc.getWorld().getName());
    }
}
