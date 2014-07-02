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
	 * 
	 * - !! REWRITE AND SIMPLIFY CODE !! (Just need to look through Listener now)
	 * 
	 * - v Fix zombies not respawning (going to hold, waiting 5 seconds, coming back)
	 *   - problem arises from using joinZombies() on a Zombie member, need to find a way to force-join the team
	 *   - originally this was done by leaveGame() then joinZombies(), but maybe an additional joinZombies()-related
	 *   - helper method is in order
	 * - Fix holds not used(?)
	 *   - problem seems to fix itself once I specifically set the hold points
	 *   - maybe hold location information does not persist across restarts/reloads?
	 * - v Fix waiting players not being healed/fed on game start 
	 *   - need code to feed players to max (hunger + saturation)
	 * - v Fix offline players in the waiting list bugging game start
	 */
	
	public final Configuration config = new Configuration(this);
        public final Utils utils = new Utils(this);
	public BukkitTask gameEnd;
	public BukkitTask game30;
	public BukkitTask game10;
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
        
	//TODO: Go through commands and make command names more consistent (set vs save, etc.)
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
                utils.rosterReport(sender);
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

            else if (cmd.getName().equalsIgnoreCase("save-leave-point"))
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

            else if (cmd.getName().equalsIgnoreCase("leave-point"))
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

            else if (cmd.getName().equalsIgnoreCase("save-wait-point"))
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

            else if (cmd.getName().equalsIgnoreCase("wait-point"))
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

            //This command should only send players to the waiting team - all pre-game checks are done with /infection-start
            else if (cmd.getName().equalsIgnoreCase("joingame"))
            {
                if (sender instanceof Player)
                {
                	Player p = (Player)sender;
                    if (!utils.isInGame(p))
                    {
                    	//pre-game inventory/potion effect clearing
                    	//may be a good idea to move these to loadout application?
                    	p.getInventory().clear();
                        if (p.getActivePotionEffects() != null)
                        {
                            for (PotionEffect pe: p.getActivePotionEffects())
                            {
                                p.removePotionEffect(pe.getType());
                            }
                        }

                        //send player to wait point
                        //config.SPAWN_WAIT is now a Location object so checking its length becomes redundant
                        if (config.SPAWN_WAIT == null)// || config.SPAWN_WAIT.length() == 0)
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
                        utils.rosterReport(p);
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
                    getServer().broadcastMessage(ChatColor.RED + "The game has been ended early!");
                    gameActive = false;
                    //Stop game end/time announcements from happening as scheduled if the game has force-ended
                    gameEnd.cancel();
                    game30.cancel();
                    game10.cancel();
                    resetPlayers();
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "The game is already over!");
                }
                return true;
            }
            
            else if (cmd.getName().equalsIgnoreCase("ff"))
            {
            	String status = "";
            	if (config.FRIENDLY_FIRE == true)
            	{
            		status = ChatColor.RED + "ON";
            	}
            	else //if false
            	{
            		status = ChatColor.GREEN + "OFF";
            	}
            	sender.sendMessage(ChatColor.GOLD + "Friendly fire is " + status + ChatColor.GOLD + ".");
            	return true;
            }
            
            else if (cmd.getName().equalsIgnoreCase("ffon"))
            {
            	if (config.FRIENDLY_FIRE == true)
            	{
            		sender.sendMessage(ChatColor.RED + "Friendly fire is already on!");
            	}
            	else
            	{
            		config.FRIENDLY_FIRE = true;
            		getServer().broadcastMessage(ChatColor.GOLD + "Friendly fire is now " + ChatColor.RED + "ON");
            	}
            	return true;
            }
            
            else if (cmd.getName().equalsIgnoreCase("ffoff"))
            {
            	if (config.FRIENDLY_FIRE == false)
            	{
            		sender.sendMessage(ChatColor.RED + "Friendly fire is already off!");
            	}
            	else
            	{
            		config.FRIENDLY_FIRE = false;
            		getServer().broadcastMessage(ChatColor.GOLD + "Friendly fire is now " + ChatColor.GREEN + "OFF");
            	}
            	return true;
            }

            return false;
	}
	
	//TODO
	//Methods below, commands above 
	
	//First checks that spawn points are valid (ie, they exist)
	//Removes player from Zombies or Waiting team if applicable
	//adds player to the human team list, gives them the loadout.
	public void joinHuman(Player p)
	{
		//Check if player is on the team already
            if (utils.isHuman(p))
            {
                p.sendMessage(ChatColor.RED + "You are already on the human team!");
                return;
            }

          //Attempt to teleport player to the spawn point
            Location l = utils.getRandomSpawn(config.SPAWN_HUMAN);
            if (l == null)
            {
                p.sendMessage("ERROR: No human spawn points saved!  Use /set-human-spawn to set some.");
                //p.teleport(config.SPAWN_WAIT);
            }
            else
            {
                p.teleport(l);
            }

          //Remove name from other teams if applicable
            config.TEAM_ZOMBIE.remove(p.getName());
            config.TEAM_WAITING.remove(p.getName());
            
            //Add name to team list and adjust inventory accordingly
            config.TEAM_HUMAN.add(p.getName());
            utils.applyHumanLoadout(p);
            p.sendMessage(config.HUMAN_TEXT + "You have joined the Humans!");
            
            //heal/feed player
            p.setHealth(p.getMaxHealth());
            p.setSaturation(20);
            p.setExhaustion(0);
            p.setFoodLevel(20);
	}
	
	//Puts player on the waiting team but does not notify them of it
        public void silentJoinWaiting(Player p)
        {
        	//Check if player is on the team already
            if (utils.isWaiting(p))
            {
                p.sendMessage(ChatColor.RED + "You are already waiting to play!");
                return;
            }

            //Attempt to teleport player to the spawn point
            Location l = config.SPAWN_WAIT;
            if (l == null)
            {
                p.sendMessage("ERROR: No wait point saved!  Use /save-waitspawn to set one.");
            }
            else
            {
                p.teleport(l);
            }
            
            //Remove name from other teams if applicable
            config.TEAM_HUMAN.remove(p.getName());
            
            config.TEAM_ZOMBIE.remove(p.getName());
            
            //Add name to team list and adjust inventory accordingly
            config.TEAM_WAITING.add(p.getName());
            p.getInventory().clear();
        	p.getInventory().setArmorContents(null);
            for (PotionEffect pe: p.getActivePotionEffects())
            {
            	p.removePotionEffect(pe.getType());
            }
            
            p.setHealth(p.getMaxHealth());
            p.setSaturation(20);
            p.setExhaustion(0);
            p.setFoodLevel(20);
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

            forceJoinZombie(p);
            
            config.TEAM_WAITING.remove(p.getName());
	}
	
	//This should only be used to put players on the zombie team after respawning in the hold;
	//some loops and other things can be thrown out for simplicity
	public void forceJoinZombie(Player p)
	{
            Location l = utils.getRandomSpawn(config.SPAWN_ZOMBIE);
            p.teleport(l);
            
            /*Iterator<String> humanIterator = config.TEAM_HUMAN.iterator();
            while(humanIterator.hasNext()) {
                String name = humanIterator.next();
                if(name.equals(p.getName())) {
                    humanIterator.remove();
                }
            }*/
            config.TEAM_HUMAN.remove(p.getName());

            //don't add a zombie to the zombie team list again
            if (!config.TEAM_ZOMBIE.contains(p.getName()))
            {
            	config.TEAM_ZOMBIE.add(p.getName());
            }
            utils.applyZombieLoadout(p);
            p.sendMessage(config.ZOMBIE_TEXT + "You have joined the Zombies!");
            
            //heal/feed player
            p.setHealth(p.getMaxHealth());
            p.setSaturation(20);
            p.setExhaustion(0);
            p.setFoodLevel(20);
	}
	
	//Not merged, but has been rewritten as analogue to existing human/zombie joining methods
	//Silent version does not notify players of the team change
	public void quietLeaveGame(Player p)
	{
            //Check if player is on the team already
			if (!(utils.isZombie(p)||utils.isHuman(p)||utils.isWaiting(p)))
            {
                p.sendMessage(ChatColor.RED + "You are currently not in the game!");
                return;
            }
            
			//Attempt to teleport player to the spawn point
			if (config.SPAWN_LEAVE == null)
            {
                p.sendMessage(ChatColor.RED + "Sent to your spawn point.  Tell your admin to save a leave point!");
                p.teleport(p.getBedSpawnLocation());
            }
            else
            {
                p.teleport(config.SPAWN_LEAVE);
            }
			
			//Remove name from other teams if applicable
            config.TEAM_HUMAN.remove(p.getName());
            config.TEAM_WAITING.remove(p.getName());
            config.TEAM_ZOMBIE.remove(p.getName());
            
            //Add name to team list (nothing for leaving) and adjust inventory accordingly
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            if (p.getActivePotionEffects() != null)
            {
                for (PotionEffect pe: p.getActivePotionEffects())
                {
                    p.removePotionEffect(pe.getType());
                }
            }
            
            p.setHealth(p.getMaxHealth());
            p.setSaturation(20);
            p.setExhaustion(0);
            p.setFoodLevel(20);
	}
	
	public void leaveGame(Player p)
	{
            quietLeaveGame(p);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "You have left the game!");
	}
	
	//This is used to remove offline players from teams
	//Throws exception but still works well enough
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


                //TODO: Check this
                //First go through the list and throw out any offline players
                Object[] waitList = config.TEAM_WAITING.toArray();
                for (Object name: waitList)
                {
                	OfflinePlayer op = getServer().getOfflinePlayer((String)name);
                	if (!op.isOnline())
                	{
                		config.TEAM_WAITING.remove((String)name);
                	}
                }
                
                //Team sorting
                int num = config.TEAM_WAITING.size();
                int zombienum = num / 6 + 1; //NOTE: If only one player has joined the game, this player will be sorted as Zombie
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

                //scheduled event that ends the game
                Runnable scheduleGameEnd = new Runnable(){
                        public void run(){
                            if (config.TEAM_HUMAN.size() > 0)
                            {
                                getServer().broadcastMessage(config.HUMAN_TEXT + "The humans have won! Game over!");
                            }
                            else //just in case this happens
                            {
                                getServer().broadcastMessage(config.ZOMBIE_TEXT + "The zombies have won! Game over!");
                            }
                            gameActive = false;
                            //move all players back to waiting list
                            resetPlayers();
                        }
                };
                
                //scheduled event that announces 30 seconds left - could pull from config instead?
                if (gameTimeSeconds - 30 > 0)
                {
                	Runnable schedule30 = new Runnable(){
                        public void run(){
                            getServer().broadcastMessage(ChatColor.GOLD + "30 seconds left in the game!");
                        }
                	};
                	game30 = Bukkit.getScheduler().runTaskLater(this, schedule30, (long)((gameTimeSeconds-30) * 20));
                }
                
                //scheduled event that announces 10 seconds left
                if (gameTimeSeconds - 10 > 0)
                {
                	Runnable schedule10 = new Runnable(){
                        public void run(){
                            getServer().broadcastMessage(ChatColor.GOLD + "10 seconds left in the game!");
                        }
                	};
                	game10 = Bukkit.getScheduler().runTaskLater(this, schedule10, (long)((gameTimeSeconds-10) * 20));
                }

                gameEnd = Bukkit.getScheduler().runTaskLater(this, scheduleGameEnd, (long)(gameTimeSeconds * 20));
                gameActive = true;
            }
	}
	
	//TODO: Is it possible to merge human/zombie team list together and then go through that list by itself?
	//Sends everyone back to waiting team - assume there are humans and zombies in case humans win
	//Players who are offline at time of reset are simply removed from the team list
	public void resetPlayers()
	{
            //Move zombies and humans to waiting team
		/*
			Iterator<String> zombieIterator = config.TEAM_ZOMBIE.iterator();
            while(zombieIterator.hasNext()) {
                String name = zombieIterator.next();
                config.TEAM_WAITING.add(name);
                zombieIterator.remove();
            }*/
		//Create a duplicate team list and run silentJoinWaiting() on the players obtained from that list
		//to avoid ConcurrentModificationException - currently probably uses more memory than necessary
		
		Object[] zomList = config.TEAM_ZOMBIE.toArray();
		for (Object name: zomList)
		{
			OfflinePlayer op = getServer().getOfflinePlayer((String)name);
			if (op.isOnline())
			{
				Player p = op.getPlayer();
				silentJoinWaiting(p);
			}
			else
			{
				removePlayerFromGame(op);
			}
		}
            
		Object[] humList = config.TEAM_HUMAN.toArray();
		for (Object name: humList)
		{
			OfflinePlayer op = getServer().getOfflinePlayer((String)name);
			if (op.isOnline())
			{
				Player p = op.getPlayer();
				silentJoinWaiting(p);
			}
			else
			{
				removePlayerFromGame(op);
			}
		}
	}
}