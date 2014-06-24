package com.gmail.buzziespy.MCInfection;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

public final class MCInfection extends JavaPlugin implements Listener{
	
	/* TODO:
	 * To do:
	 * Fix dead human players not teleporting to the hold cells for respawn time
	 * v Clear player inventories upon death
	 * Prevent players from moving or placing the team wool indicators
	 * v Fix zombie players not being removed from zombie team upon game end (they appear as both Zombies and Waiting)
	 * v Fix non-ops not being able to use /teams, /joingame, /leavegame
	 * v Fix players leaving the game twice
	 * Allow console to remove offline players from teams
	 * v Allow console or ops to stop an active game
	 * v Set up ability to determine game length using /infection-start [time in seconds]
	 * v Teleport players to waitspawn on game end
	 * v Teleport the last dead player to waitspawn on game end (eg. the last dead human)
	 * v Schedule a Bukkit task to declare human victory if time runs out and at least one human is alive
	 * v Cancel that task if zombies win
	 */
	
	public final Configuration config = new Configuration(this);
        public final Utils utils = new Utils(this);
	public BukkitTask gameEnd;
        public boolean gameActive = false;
        
	@Override
	public void onEnable()
	{
            getLogger().info("Loading config file...");
            File configFile = new File(getDataFolder(), "config.yml");
            if(!configFile.exists()) {
                getConfig().options().copyDefaults(true);
                saveConfig();
            }
            config.load();
            (new InfectionListener(this)).registerEvents();
	}
	
	@Override
	public void onDisable()
	{
            //save the config file
            getLogger().info("Saving config!");
            config.save();
	}
	
	/*
	 * On initial sorting of teams, by order of increasing number of players:
	 * 1 zombie, 1 human
	 * 1 zombie, 2 humans
	 * 1 zombie, 3 humans
	 * 1 zombie, 4 humans
	 * 1 zombie, 5 humans
	 * 2 zombies, 6 humans
	 * 2 zombies, 7 humans
	 * 2 zombies, 8 humans
	 * ...
	 * 
	 * zombies = (total number of players)/6 (integer division) + 1
	 * 
	 * Humans and zombies have respawn locations saved in the config.  They will be saved in string form as:
	 * <X>,<Y>,<Z>;<pitch>,<yaw>
	 * It is expected to have multiple of these respawn locations per team.  These can be set via commands.
	 * The program will randomly choose a place and send the player there.
	 */
        
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
            /*The player has joined the game if the player is listed in any of the four "teams" below.
             * Each team has spawn points saved in-game and recorded in the config file
             * 
             * Spectator: Only spectates the game; spawn point is in the stands(?)
             * Waiting: Spectates the game but is sorted into human/zombie team when the round starts again; 
             *          spawn point is shared with spectator team
             * Human: Most of the players start out as this; their goal is to stay alive until time runs out.  Humans who die will
             *        turn into zombies and be moved to the zombie side.
             *        Humans have a few spawn points on the map itself.
             * Zombie: A few players start out as this; their goal is to kill humans and convert all of them into zombies.
             *         Zombies have their own spawn points on the other side of the map.
             */

            //These fail silently for command senders that are not players
            if(cmd.getName().equalsIgnoreCase("infect")) {
                if(args.length == 0) {
                    sender.sendMessage(config.printCurrentSettings());
                } else {
                    if(args[0].equalsIgnoreCase("reload")) {
                        config.reload();
                        sender.sendMessage(ChatColor.GREEN + "[MCInfection] Config Reloaded");
                    } else
                        return false;
                }
                return true;
            }
            else if (cmd.getName().equalsIgnoreCase("joinwait"))
            {
                if(!utils.isGameSetUp()) {
                    sender.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return true;
                }
                Player p = null;
                if (sender instanceof Player && args.length == 0)
                {
                    p = (Player)sender;
                }
                else if (args.length == 1 && sender.hasPermission("infect.admin"))
                {
                    p = (Player)sender.getServer().getOfflinePlayer(args[0]);
                }
                joinWaiting(p);
                return true;
            }
            else if (cmd.getName().equalsIgnoreCase("joinhum"))
            {
                if(!utils.isGameSetUp()) {
                    sender.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return true;
                }
                Player p = null;
                if (sender instanceof Player && args.length == 0)
                {
                    p = (Player)sender;
                }
                else if (args.length == 1 && sender.hasPermission("infect.admin"))
                {
                    p = (Player)sender.getServer().getOfflinePlayer(args[0]);
                }
                joinHuman(p);
                return true;
            }
            else if (cmd.getName().equalsIgnoreCase("joinzom"))
            {
                if(!utils.isGameSetUp()) {
                    sender.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return true;
                }
                Player p = null;
                if (sender instanceof Player && args.length == 0)
                {
                    p = (Player)sender;
                }
                else if (args.length == 1 && sender.hasPermission("infect.admin"))
                {
                    p = (Player)sender.getServer().getOfflinePlayer(args[0]);
                }
                joinZombie(p);
                return true;
            }
            else if (cmd.getName().equalsIgnoreCase("leavegame"))
            {
                if(!utils.isGameSetUp()) {
                    sender.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return true;
                }
                Player p = null;
                if (sender instanceof Player && args.length == 0)
                {
                    p = (Player)sender;
                    leaveGame(p);
                }
                else if (args.length == 1 && sender.hasPermission("infect.admin"))
                {
                    if (sender.getServer().getOfflinePlayer(args[0]).isOnline())
                    {
                        p = (Player)sender.getServer().getOfflinePlayer(args[0]);
                        leaveGame(p);
                    }
                    else //if player in argument is not online
                    {
                        OfflinePlayer op = sender.getServer().getOfflinePlayer(args[0]);
                        removePlayerFromGame(op);
                    }
                }
                return true;
            }

            //shows current teams to the player
            else if (cmd.getName().equalsIgnoreCase("teams"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    utils.playerRosterReport(p);
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    utils.rosterReport();
                }
                return true;
            }

            //set a spawn point for humans at the player's location
            //this should also save the player's camera orientation, so the player should take
            //care not to stare at walls when running this!
            else if (cmd.getName().equalsIgnoreCase("set-human-spawn"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();

                    //store to spawn location list
                    config.SPAWN_HUMAN.add(l);
                    sender.sendMessage(config.HUMAN_TEXT + "Human spawn set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            //set a spawn point for zombies at the player's location
            //this should also save the player's camera orientation, so the player should take
            //care not to stare at walls when running this!
            else if (cmd.getName().equalsIgnoreCase("set-zombie-spawn"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();

                    //store to spawn location list
                    config.SPAWN_ZOMBIE.add(l);
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie spawn set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("human-spawn"))
            {       
                if(config.SPAWN_HUMAN != null && config.SPAWN_HUMAN.size() > 0) {
                    sender.sendMessage(config.HUMAN_TEXT + "Human spawns:");
                    for (Location loc : config.SPAWN_HUMAN)
                    {
                        sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                    }
                } else {
                    sender.sendMessage(config.HUMAN_TEXT + "No Human spawns are set. Use /set-human-spawn to add one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("zombie-spawn"))
            {
                if(config.SPAWN_ZOMBIE != null && config.SPAWN_ZOMBIE.size() > 0) {
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie spawns:");
                    for (Location loc: config.SPAWN_ZOMBIE)
                    {
                        sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                    }
                } else {
                    sender.sendMessage(config.ZOMBIE_TEXT + "No Zombie spawns are set. Use /set-zombie-spawn to add one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("clear-human-spawn"))
            {
                config.SPAWN_HUMAN.clear();
                sender.sendMessage(config.HUMAN_TEXT + "Human spawns cleared!");
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("clear-zombie-spawn"))
            {
                config.SPAWN_ZOMBIE.clear();
                sender.sendMessage(config.ZOMBIE_TEXT + "Zombie spawns cleared!");
                return true;
            }

            //Set the location of a holding cell for players once they are killed.
            //They will sit in here for the duration of a respawn timer before
            //being returned to the map.
            else if (cmd.getName().equalsIgnoreCase("set-human-hold"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();
                    config.SPAWN_HUMAN_HOLD = l;
                    sender.sendMessage(config.HUMAN_TEXT + "Human hold set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            //set a spawn point for zombies at the player's location
            //this should also save the player's camera orientation, so the player should take
            //care not to stare at walls when running this!
            else if (cmd.getName().equalsIgnoreCase("set-zombie-hold"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();
                    config.SPAWN_ZOMBIE_HOLD = l;
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie hold set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("human-hold"))
            {
                if(config.SPAWN_HUMAN_HOLD != null) {
                    sender.sendMessage(config.HUMAN_TEXT + "Human hold cell:");
                    Location loc = config.SPAWN_HUMAN_HOLD;
                    sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                } else {
                    sender.sendMessage(config.HUMAN_TEXT + "No Human hold cells are set. Use /set-human-hold to set one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("zombie-hold"))
            {
                if(config.SPAWN_ZOMBIE_HOLD != null) {
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie hold cell:");
                    Location loc = config.SPAWN_ZOMBIE_HOLD;
                    sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                } else {
                    sender.sendMessage(config.ZOMBIE_TEXT + "No Zombie hold cells are set. Use /set-zombie-hold to set one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("clear-human-hold"))
            {
                config.SPAWN_HUMAN_HOLD = null;
                sender.sendMessage(config.HUMAN_TEXT + "Human hold cell cleared!");
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("clear-zombie-hold"))
            {
                config.SPAWN_ZOMBIE_HOLD = null;
                sender.sendMessage(config.ZOMBIE_TEXT + "Zombie hold cell cleared!");
                return true;
            }

            //Saves human loadout.  Inventory is saved as a string <armor1>,<armor2>,<armor3>,<armor4>;<1>,<2>,...,<36>.
            //Potion effects are assumed to be permanent, so the player's existing effects will be saved and given a length
            //of a very long time - say, 7200 seconds (2 hours).  The effect numbers and strength themselves are listed
            //under potion.human and potion.zombie: <effect number>,<strength> (duration assumed to be a long time (2 hours))
            else if (cmd.getName().equalsIgnoreCase("save-human-loadout"))
            {
                if (sender instanceof Player)
                {
                    //save inventory
                    Player p = (Player)sender;
                    config.LOADOUT_HUMAN_INVEN = Arrays.asList(p.getInventory().getContents());

                    //save armor
                    config.LOADOUT_HUMAN_ARMOR = Arrays.asList(p.getInventory().getArmorContents());

                    config.LOADOUT_HUMAN_POTIONS = (List<PotionEffect>)p.getActivePotionEffects();

                    p.sendMessage(config.HUMAN_TEXT + "Human loadout saved!");
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }
            else if (cmd.getName().equalsIgnoreCase("save-zombie-loadout"))
            {
                if (sender instanceof Player)
                {
                    //save inventory
                    Player p = (Player)sender;
                    config.LOADOUT_ZOMBIE_INVEN = Arrays.asList(p.getInventory().getContents());

                    //save armor
                    config.LOADOUT_ZOMBIE_ARMOR = Arrays.asList(p.getInventory().getArmorContents());

                    config.LOADOUT_ZOMBIE_POTIONS = (List<PotionEffect>)p.getActivePotionEffects();

                    p.sendMessage(config.ZOMBIE_TEXT + "Zombie loadout saved!");
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("human-loadout"))
            {
                if(config.LOADOUT_HUMAN_INVEN != null && config.LOADOUT_HUMAN_INVEN.size() > 0) {
                    sender.sendMessage(config.HUMAN_TEXT + "Human inventory:");
                    for(ItemStack item : config.LOADOUT_HUMAN_INVEN) {
                        if(item != null )
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + item.getType().name() + " x" + item.getAmount());
                    }
                } else {
                    sender.sendMessage(config.HUMAN_TEXT + "No Human inventory is set. Use /save-human-loadout to set one.");
                }
                if(config.LOADOUT_HUMAN_ARMOR != null && config.LOADOUT_HUMAN_ARMOR.size() > 0) {
                    sender.sendMessage(config.HUMAN_TEXT + "Human armor:");
                    for(ItemStack item : config.LOADOUT_HUMAN_ARMOR) {
                        if(item != null)
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + item.getType().name());
                    }
                } else {
                    sender.sendMessage(config.HUMAN_TEXT + "No Human armor is set. Use /save-human-loadout to set one.");
                }
                if(config.LOADOUT_HUMAN_POTIONS != null && config.LOADOUT_HUMAN_POTIONS.size() > 0) {
                    sender.sendMessage(config.HUMAN_TEXT + "Human potion effects:");
                    //assuming all entries are PotionEffects
                    for(PotionEffect effect : config.LOADOUT_HUMAN_POTIONS) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + effect.getType().getName() + " " + (effect.getAmplifier()+1));
                    }
                } else {
                    sender.sendMessage(config.HUMAN_TEXT + "No Human potions are set. Use /save-human-loadout to set one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("zombie-loadout"))
            {
                if(config.LOADOUT_ZOMBIE_INVEN != null && config.LOADOUT_ZOMBIE_INVEN.size() > 0) {
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie inventory:");
                    for(ItemStack item : config.LOADOUT_ZOMBIE_INVEN) {
                        if(item != null)
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + item.getType().name() + " x" + item.getAmount());
                    }
                } else {
                    sender.sendMessage(config.ZOMBIE_TEXT + "No Zombie inventory is set. Use /save-zombie-loadout to set one.");
                }
                if(config.LOADOUT_ZOMBIE_ARMOR != null && config.LOADOUT_ZOMBIE_ARMOR.size() > 0) {
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie armor:");
                    for(ItemStack item : config.LOADOUT_ZOMBIE_ARMOR) {
                        if(item != null)
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + item.getType().name());
                    }
                } else {
                    sender.sendMessage(config.ZOMBIE_TEXT + "No Zombie armor is set. Use /save-zombie-loadout to set one.");
                }
                if(config.LOADOUT_ZOMBIE_POTIONS != null && config.LOADOUT_ZOMBIE_POTIONS.size() > 0) {
                    sender.sendMessage(config.ZOMBIE_TEXT + "Zombie potion effects:");
                    //assuming all entries are PotionEffects
                    for(PotionEffect effect : config.LOADOUT_ZOMBIE_POTIONS) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + effect.getType().getName() + " " + (effect.getAmplifier()+1));
                    }
                } else {
                    sender.sendMessage(config.ZOMBIE_TEXT + "No Zombie potions are set. Use /save-zombie-loadout to set one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("save-leavespawn"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();
                    config.SPAWN_LEAVE = l;

                    sender.sendMessage(ChatColor.RED + "Leave point set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("leavespawn"))
            {
                if(config.SPAWN_LEAVE != null) {
                    sender.sendMessage(ChatColor.RED + "Leave point:");
                    Location loc = config.SPAWN_LEAVE;
                    sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                } else {
                    sender.sendMessage(ChatColor.RED + "No Leave point has been set. Use /save-leavespawn to set one");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("save-waitspawn"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Location l = p.getLocation();
                    config.SPAWN_WAIT = l;
                    sender.sendMessage(ChatColor.RED + "Wait point set! " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                else if (sender instanceof ConsoleCommandSender)
                {
                    getLogger().info("You must be in-game to run this!");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("waitspawn"))
            {
                if(config.SPAWN_WAIT != null) {
                    sender.sendMessage(ChatColor.RED + "Wait point:");
                    Location loc = config.SPAWN_WAIT;
                    sender.sendMessage(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
                } else {
                    sender.sendMessage(ChatColor.RED + "No Wait point has been set. Use /save-waitspawn to set one.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("joingame"))
            {
                if (sender instanceof Player)
                {
                    Player p = (Player)sender;
                    if (!utils.isInGame(p))
                    {
                        p.getInventory().clear();
                        if (p.getActivePotionEffects() != null)
                        {
                            for (PotionEffect pe: p.getActivePotionEffects())
                            {
                                p.removePotionEffect(pe.getType());
                            }
                        }

                        //send player to wait point
                        if (config.SPAWN_WAIT == null || config.SPAWN_WAIT.length() == 0)
                        {
                            p.sendMessage(ChatColor.RED + "Tell your admin to save a wait point first!");
                        }
                        else
                        {
                            p.teleport(config.SPAWN_WAIT);
                            joinWaiting(p);
                        }
                    }
                    else
                    {
                        p.sendMessage(ChatColor.YELLOW + "You're already on a team!");
                        utils.playerRosterReport(p);
                    }
                    return true;
                }
            }

            else if (cmd.getName().equalsIgnoreCase("infection-start"))
            {
                if (args.length != 0 && args.length != 1) {
                    return false;
                } else if(!utils.isGameSetUp()) {
                    sender.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return true;
                } else if(!utils.isLoadoutSetUp()) {
                    sender.sendMessage(ChatColor.GOLD + "WARNING: Team loadouts are not completely set up. Run /infect to see current settings.");
                } else if (args.length == 0) {
                    startGame(sender, config.GAME_TIME);  //default game time is 1 minute
                    getLogger().info("Starting new Infection game for " + config.GAME_TIME + " seconds.");
                } else if (args.length == 1) {
                    try {
                        startGame(sender, Integer.parseInt(args[0]));
                    } catch(NumberFormatException e) {
                        sender.sendMessage("Game time specified must be a valid integer.");
                        return false;
                    }

                    getLogger().info("Starting new Infection game for " + args[0] + " seconds.");
                }
                return true;
            }

            else if (cmd.getName().equalsIgnoreCase("infection-end"))
            {
                if (gameActive)
                {
                    getServer().broadcastMessage(ChatColor.RED + "The game has been ended!");
                    gameActive = false;
                    //Stop game from ending as scheduled if the game has force-ended
                    gameEnd.cancel();
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "The game is already over!");
                }
                return true;
            }

            return false;
	}
	
	//TODO
	//Methods below, commands above 
	
	public void joinHuman(Player p)
	{
            if (utils.isHuman(p))
            {
                p.sendMessage(ChatColor.RED + "You are already on the human team!");
                return;
            }

            Location l = utils.getRandomSpawn(config.SPAWN_HUMAN);
            if (l == null)
            {
                p.sendMessage("ERROR: Tell your admin to save human spawn points!");
                p.teleport(config.SPAWN_WAIT);
            }
            else
            {
                p.teleport(l);
            }

            Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                if(name.equals(p.getName())) {
                    zombieIterator.remove();
                }
            }
            
            Iterator<String> waitingIterator = config.TEAM_WAITING.iterator();
            while(waitingIterator.hasNext()) {
                String name = waitingIterator.next();
                if(name.equals(p.getName())) {
                    waitingIterator.remove();
                }
            }
            config.TEAM_HUMAN.add(p.getName());
            utils.applyHumanLoadout(p);
            p.sendMessage(config.HUMAN_TEXT + "You have joined the Humans!");
	}
        
        public void silentJoinWaiting(Player p)
        {
            if (utils.isWaiting(p))
            {
                p.sendMessage(ChatColor.RED + "You are already waiting to play!");
                return;
            }
            
            Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                if(name.equals(p.getName())) {
                    humanIterator.remove();
                }
            }
            
            Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                if(name.equals(p.getName())) {
                    zombieIterator.remove();
                }
            }
            
            config.TEAM_WAITING.add(p.getName());
            
        }
	
	public void joinWaiting(Player p)
	{
            silentJoinWaiting(p);
            p.sendMessage(ChatColor.YELLOW + "You are now waiting to play!");
        }
	
	public void joinZombie(Player p)
	{
            if (utils.isZombie(p))
            {
                p.sendMessage(ChatColor.RED + "You are already on the zombie team!");
                return;
            }

            Location l = utils.getRandomSpawn(config.SPAWN_ZOMBIE);
            if (l == null)
            {
                p.sendMessage("ERROR: Tell your admin to save human spawn points!");
                p.teleport(config.SPAWN_WAIT);
            }
            else
            {
                p.teleport(l);
            }
            
            Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                if(name.equals(p.getName())) {
                    humanIterator.remove();
                }
            }
            
            Iterator<String> waitingIterator = config.TEAM_WAITING.iterator();
            while(waitingIterator.hasNext()) {
                String name = waitingIterator.next();
                if(name.equals(p.getName())) {
                    waitingIterator.remove();
                }
            }

            config.TEAM_ZOMBIE.add(p.getName());
            utils.applyZombieLoadout(p);
            p.sendMessage(config.ZOMBIE_TEXT + "You have joined the Zombies!");
	}
	
	public void quietLeaveGame(Player p)
	{
            if (!(utils.isZombie(p)||utils.isHuman(p)||utils.isWaiting(p)))
            {
                p.sendMessage(ChatColor.RED + "You are currently not in the game!");
                return;
            }
            
            Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                if(name.equals(p.getName())) {
                    humanIterator.remove();
                }
            }
            
            Iterator<String> waitingIterator = config.TEAM_WAITING.iterator();
            while(waitingIterator.hasNext()) {
                String name = waitingIterator.next();
                if(name.equals(p.getName())) {
                    waitingIterator.remove();
                }
            }
            Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                if(name.equals(p.getName())) {
                    zombieIterator.remove();
                }
            }		
	}
	
	public void leaveGame(Player p)
	{
            quietLeaveGame(p);
            if (config.SPAWN_LEAVE == null)
            {
                p.sendMessage(ChatColor.RED + "Sent to your spawn point.  Tell your admin to save a leave point!");
                p.teleport(p.getBedSpawnLocation());
            }
            else
            {
                p.teleport(config.SPAWN_LEAVE);
            }
            if (p.getActivePotionEffects() != null)
            {
                for (PotionEffect pe: p.getActivePotionEffects())
                {
                    p.removePotionEffect(pe.getType());
                }
            }
            p.sendMessage(ChatColor.LIGHT_PURPLE + "You have left the game!");
	}
	
	public void removePlayerFromGame(OfflinePlayer op) //
	{
            Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                if(name.equals(op.getName())) {
                    humanIterator.remove();
                }
            }
            
            Iterator<String> waitingIterator = config.TEAM_WAITING.iterator();
            while(waitingIterator.hasNext()) {
                String name = waitingIterator.next();
                if(name.equals(op.getName())) {
                    waitingIterator.remove();
                }
            }
            Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                if(name.equals(op.getName())) {
                    zombieIterator.remove();
                }
            }
	}

	public void startGame(CommandSender cs, int gameTimeSeconds)
        {	
            if (gameActive)
            {
                    cs.sendMessage("The game is already in progress!");
            }
            else
            {
                Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
                while(zombieIterator.hasNext()) {
                    String name = zombieIterator.next();
                    config.TEAM_WAITING.add(name);
                    zombieIterator.remove();
                }
                
                Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
                while(humanIterator.hasNext()) {
                    String name = humanIterator.next();
                    config.TEAM_WAITING.add(name);
                    humanIterator.remove();
                }

                //check that spawn points exist
                if(!utils.isGameSetUp()) {
                    cs.sendMessage(ChatColor.RED + "Game setup has not been completed. Run /infect to see current settings.");
                    return;
                }
                if(!utils.isLoadoutSetUp()) {
                    cs.sendMessage(ChatColor.GOLD + "WARNING: Team loadouts are not completely set up. Run /infect to see current settings.");
                }


                //Team sorting
                int num = config.TEAM_WAITING.size();
                int zombienum = num / 6 + 1;
                while (zombienum != 0)
                {
                    //Randomly choose an index given the size of the list of players
                    int index = (int)(Math.random()*num);
                    String name = config.TEAM_WAITING.get(index);
                    Player p = (Player)this.getServer().getOfflinePlayer(name);
                    joinZombie(p);
                    zombienum--;
                }

                while(!config.TEAM_WAITING.isEmpty())
                {
                    String name = config.TEAM_WAITING.get(0);
                    Player p = (Player)this.getServer().getOfflinePlayer(name);
                    joinHuman(p);
                }

                Runnable scheduleGameEnd = new Runnable(){
                        public void run(){
                            if (config.TEAM_HUMAN.size() > 0)
                            {
                                getServer().broadcastMessage(config.HUMAN_TEXT + "The humans have won! Game over!");
                            }
                            else
                            {
                                getServer().broadcastMessage(config.ZOMBIE_TEXT + "The zombies have won! Game over!");
                            }
                            gameActive = false;
                            //move all players back to waiting list
                            resetPlayers();
                        }


                };

                gameEnd = Bukkit.getScheduler().runTaskLater(this, scheduleGameEnd, (long)(gameTimeSeconds * 20));
                gameActive = true;
            }
	}
	
	public void resetPlayers()
	{
            Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                config.TEAM_WAITING.add(name);
                zombieIterator.remove();
            }

            Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                config.TEAM_WAITING.add(name);
                humanIterator.remove();
            }
            
            Iterator<String> waitingIterator = config.TEAM_WAITING.iterator();
            while(waitingIterator.hasNext())
            {
                String name = waitingIterator.next();
                OfflinePlayer op = getServer().getOfflinePlayer(name);
                if (op.isOnline())
                {
                    //clear inventory 
                    Player p = (Player)op;
                    p.getInventory().clear();
                    if (p.getActivePotionEffects() != null)
                    {
                            for (PotionEffect pe: p.getActivePotionEffects())
                            {
                                    p.removePotionEffect(pe.getType());
                            }
                    }
                    p.teleport(config.SPAWN_WAIT);
                }
            }
	}
}