package com.gmail.buzziespy.MCInfection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class InfectionListener implements Listener {
    MCInfection plugin;
    Utils utils;
    Configuration config;
    
    public InfectionListener(MCInfection plugin) {
        this.plugin = plugin;
        utils = plugin.utils;
        config = plugin.config;
    }
    
    public void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e)
    {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player)
        {
            //Handle PvP damage here
            Player attacker = (Player)e.getDamager();
            Player defender = (Player)e.getEntity();

            //prevent friendly fire if toggled
            if (!config.FRIENDLY_FIRE && (utils.isHuman(attacker) && utils.isHuman(defender))||(utils.isZombie(attacker) && utils.isZombie(defender)))
            {
                    e.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e)
    {
        //Prevent players in game from dropping items on death
        if (utils.isHuman(e.getEntity()) || utils.isZombie(e.getEntity()))
        {
            e.getDrops().clear();
        }

        if (utils.isHuman(e.getEntity()))
        {
            //Set the dead player's spawn to a pre-designated location in the config to wait five seconds before respawning on the map.
            //In gameplay, this would likely be some sort of bedrock holding cell outside of the map.

            //joinZombie(e.getEntity());

            //check if there are any humans left alive; if not, declare game over
            plugin.getServer().broadcastMessage(ChatColor.GREEN + e.getEntity().getName() + " has been infected!");
            if (plugin.gameActive && config.TEAM_HUMAN.size() == 1 && config.TEAM_HUMAN.get(0).equals(e.getEntity().getName()))
            {
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "The zombies have won! Game over!");
                //End the game
                plugin.gameActive = false;
                plugin.gameEnd.cancel();
                plugin.game30.cancel();
                plugin.game10.cancel();
                //move all players back to waiting list
                plugin.resetPlayers();
            }
        }
    }
    
    //TODO: This here is said to be buggy/redundant
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e)
    {
        //do nothing if player is not in game
        if (!utils.isInGame(e.getPlayer()))
        {
            return;
        }

        //Send all in-game players players to the wait spawn if game is over
        if (!plugin.gameActive && utils.isInGame(e.getPlayer()))
        {
            if (config.SPAWN_WAIT == null)
            {
                    e.setRespawnLocation(e.getPlayer().getBedSpawnLocation());
            }
            else
            {
                    e.setRespawnLocation(config.SPAWN_WAIT);
            }
            e.getPlayer().sendMessage(ChatColor.YELLOW + "You are now waiting to play!");
            return;
        }

        if (utils.isHuman(e.getPlayer()) && plugin.gameActive)
        {
            //e.setRespawnLocation(config.SPAWN_HUMAN_HOLD);
        	e.getPlayer().teleport(config.SPAWN_HUMAN_HOLD);
        }

        if (utils.isZombie(e.getPlayer()) && plugin.gameActive)
        {
            e.setRespawnLocation(config.SPAWN_ZOMBIE_HOLD);
        	//e.getPlayer().teleport(config.SPAWN_ZOMBIE_HOLD);
        }
        final Player p = e.getPlayer();
        p.sendMessage(config.ZOMBIE_TEXT + "You will respawn in " + config.ZOMBIE_RESPAWN + " seconds.");
        Runnable respawn = new Runnable() { 
            public void run() {
                    plugin.forceJoinZombie(p);
            }
        };
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, respawn, (long)config.ZOMBIE_RESPAWN*20);
    }
    
    @EventHandler
    public void lockTeamWoolMove(InventoryClickEvent e)
    {
        //Cancel event if player is human and item clicked is red wool (default)
        //OR player is zombie and item clicked is lime green wool (default)

        //NOTE: this is a cast to Player from HumanEntity - no catching of extraordinary circumstances is being done
        //at the moment

        //At the moment this code is duplicating items - the wool in the hotbar is restored but the item picked up in the cursor may be tosse
        Player p = (Player)e.getWhoClicked();
        ItemStack inv = e.getCurrentItem();
        ItemStack cur = e.getCursor();
        //there's a null pointer exception firing on the next line - probably needs handling of null items (eg. air)
        if ((utils.isHuman(p) && inv.getType().equals(Material.WOOL) && inv.getDurability() == config.HUMAN_WOOL) || (utils.isZombie(p) && inv.getType().equals(Material.WOOL) && inv.getDurability() == config.ZOMBIE_WOOL))
        {
            e.setCursor(cur);
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void preventTeamWoolDrop(PlayerDropItemEvent e)
    {
        ItemStack b = e.getItemDrop().getItemStack();
        if (b.getType().equals(Material.WOOL))
        {
            if ((b.getDurability() == config.HUMAN_WOOL && utils.isHuman(e.getPlayer())) || (b.getDurability() == config.ZOMBIE_WOOL && utils.isZombie(e.getPlayer())))
            {
                    //something here to detect color of the wool placed
                e.setCancelled(true);
            }
        }
    }
    
    //This method prevents players from placing wool blocks of their team color
    @EventHandler
    public void preventWoolBuild(BlockPlaceEvent e)
    {
        Block b = e.getBlock();
        if (b.getType().equals(Material.WOOL))
        {
            if ((b.getData() == config.HUMAN_WOOL && utils.isHuman(e.getPlayer())) || (b.getData() == config.ZOMBIE_WOOL && utils.isZombie(e.getPlayer())))
            {
                     e.setCancelled(true);
            }
        }
    }
}