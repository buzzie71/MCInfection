package com.gmail.buzziespy.MCInfection;

//import java.util.Collection;
import java.util.Arrays;
import java.util.List;
//import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
//import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
//import org.bukkit.scheduler.BukkitTask;
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
	
	
	private int DEFAULT_GAME_TIME = 60; //one minute in seconds
	private BukkitTask gameEnd;
	
	@Override
	public void onEnable()
	{
		getLogger().info("Loading config file...");
		//load up the config file
		this.getConfig();
		//save copy
		this.saveDefaultConfig();
		//enable the listener
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable()
	{
		//save the config file
		getLogger().info("Saving config!");
		this.saveConfig();
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
	
	public boolean gameActive = false;
	//public int HUMAN_RESPAWN = 5;
	public int ZOMBIE_RESPAWN = 5;
	
	public int GAME_TIME = 60;
	public boolean NO_FRIENDLY_FIRE = true;
	
	
	public short HUMAN_WOOL_COLOR = (short)14; //14 = red
	public short ZOMBIE_WOOL_COLOR = (short)5; //5 = lime green
	
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e)
	{
		if (e.getDamager() instanceof Player && e.getEntity() instanceof Player)
		{
			//Handle PvP damage here
			Player attacker = (Player)e.getDamager();
			Player defender = (Player)e.getEntity();
			
			//prevent friendly fire if toggled
			if (NO_FRIENDLY_FIRE && (isHuman(attacker) && isHuman(defender))||(isZombie(attacker) && isZombie(defender)))
			{
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		//Prevent players in game from dropping items on death
		if (isHuman(e.getEntity()) || isZombie(e.getEntity()))
		{
			e.getDrops().clear();
		}
		
		if (isHuman(e.getEntity()))
		{
			//Set the dead player's spawn to a pre-designated location in the config to wait five seconds before respawning on the map.
			//In gameplay, this would likely be some sort of bedrock holding cell outside of the map.
			
			//joinZombie(e.getEntity());
			
			//check if there are any humans left alive; if not, declare game over
			getServer().broadcastMessage(ChatColor.GREEN + e.getEntity().getName() + " has been infected!");
			if (gameActive && this.getConfig().getStringList("team.human").size() == 1 && this.getConfig().getStringList("team.human").get(0).equals(e.getEntity().getName()))
			{
				getServer().broadcastMessage(ChatColor.GREEN + "The zombies have won! Game over!");
				//End the game
				gameActive = false;
				gameEnd.cancel();
				//move all players back to waiting list
				resetPlayers();
			}
		}
		if (isZombie(e.getEntity()))
		{
			
		}
	}
	
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		//do nothing if player is not in game
		if (!isInGame(e.getPlayer()))
		{
			return;
		}
		
		//Send all in-game players players to the wait spawn if game is over
		if (!gameActive && isInGame(e.getPlayer()))
		{
			Location l = getLocFromString(this.getConfig().getString("spawn.wait"), e.getPlayer());
			if (l == null)
			{
				e.setRespawnLocation(e.getPlayer().getBedSpawnLocation());
			}
			else
			{
				e.setRespawnLocation(l);
			}
			e.getPlayer().sendMessage(ChatColor.YELLOW + "You are now waiting to play!");
			return;
		}
		
		if (isHuman(e.getPlayer()) && gameActive)
		{
			e.setRespawnLocation(getHoldLoc("spawn.humanhold", e.getPlayer()));
		}
		
		if (isZombie(e.getPlayer()) && gameActive)
		{
			e.setRespawnLocation(getHoldLoc("spawn.zombiehold", e.getPlayer()));
		}
		final Player p = e.getPlayer();
		p.sendMessage(ChatColor.GREEN + "You will respawn in " + ZOMBIE_RESPAWN + " seconds.");
		Runnable respawn = new Runnable() { 
			 public void run() {
				 quietLeaveGame(p);
				 joinZombie(p);
			 }
		};
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, respawn, (long)ZOMBIE_RESPAWN*20);
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
		if ((isHuman(p) && inv.getType().equals(Material.WOOL) && inv.getDurability() == HUMAN_WOOL_COLOR) || (isZombie(p) && inv.getType().equals(Material.WOOL) && inv.getDurability() == ZOMBIE_WOOL_COLOR))
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
			if ((b.getDurability() == HUMAN_WOOL_COLOR && isZombie(e.getPlayer())) || (b.getDurability() == HUMAN_WOOL_COLOR && isZombie(e.getPlayer())))
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
			if ((b.getData() == HUMAN_WOOL_COLOR && isZombie(e.getPlayer())) || (b.getData() == HUMAN_WOOL_COLOR && isZombie(e.getPlayer())))
			{
				 e.setCancelled(true);
			}
		}
	}
	
	
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
		if (cmd.getName().equalsIgnoreCase("joinspec"))
		{
			Player p = null;
			if (sender instanceof Player && args.length == 0)
			{
				p = (Player)sender;
			}
			else if (args.length == 1 && sender.hasPermission("team.changer"))
			{
				p = (Player)sender.getServer().getOfflinePlayer(args[0]);
			}
			joinSpectator(p);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("joinwait"))
		{
			Player p = null;
			if (sender instanceof Player && args.length == 0)
			{
				p = (Player)sender;
			}
			else if (args.length == 1 && sender.hasPermission("team.changer"))
			{
				p = (Player)sender.getServer().getOfflinePlayer(args[0]);
			}
			joinWaiting(p);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("joinhum"))
		{
			Player p = null;
			if (sender instanceof Player && args.length == 0)
			{
				p = (Player)sender;
			}
			else if (args.length == 1 && sender.hasPermission("team.changer"))
			{
				p = (Player)sender.getServer().getOfflinePlayer(args[0]);
			}
			joinHuman(p);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("joinzom"))
		{
			Player p = null;
			if (sender instanceof Player && args.length == 0)
			{
				p = (Player)sender;
			}
			else if (args.length == 1 && sender.hasPermission("team.changer"))
			{
				p = (Player)sender.getServer().getOfflinePlayer(args[0]);
			}
			joinZombie(p);
			return true;
		}
		else if (cmd.getName().equalsIgnoreCase("leavegame"))
		{
			Player p = null;
			if (sender instanceof Player && args.length == 0)
			{
				p = (Player)sender;
				leaveGame(p);
			}
			else if (args.length == 1 && sender.hasPermission("team.changer"))
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
				playerRosterReport(p);
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				rosterReport();
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
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;
				
				//store to spawn location list
				addToList("spawn.human", spawnloc);
				sender.sendMessage(ChatColor.RED + "Human spawn set! " + (int)x + "," + (int)y + "," + (int)z);
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
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;
				
				//store to spawn location list
				addToList("spawn.zombie", spawnloc);
				sender.sendMessage(ChatColor.GREEN + "Zombie spawn set! " + (int)x + "," + (int)y + "," + (int)z);
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				getLogger().info("You must be in-game to run this!");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("human-spawn"))
		{
			List<String> spawnloc = this.getConfig().getStringList("spawn.human");
			sender.sendMessage(ChatColor.RED + "Human spawns:");
			for (String loc: spawnloc)
			{
				int cutoff = loc.indexOf(";");
				String coord = loc.substring(0, cutoff);
				sender.sendMessage(coord);
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("zombie-spawn"))
		{
			List<String> spawnloc = this.getConfig().getStringList("spawn.zombie");
			sender.sendMessage(ChatColor.GREEN + "Zombie spawns:");
			for (String loc: spawnloc)
			{
				int cutoff = loc.indexOf(";");
				String coord = loc.substring(0, cutoff);
				sender.sendMessage(coord);
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("clear-human-spawn"))
		{
			List<String> spawnloc = null;
			this.getConfig().set("spawn.human", spawnloc);
			sender.sendMessage(ChatColor.RED + "Human spawns cleared!");
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("clear-zombie-spawn"))
		{
			List<String> spawnloc = null;
			this.getConfig().set("spawn.zombie", spawnloc);
			sender.sendMessage(ChatColor.GREEN + "Zombie spawns cleared!");
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
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;

				this.getConfig().set("spawn.humanhold", spawnloc);
				sender.sendMessage(ChatColor.RED + "Human hold set! " + (int)x + "," + (int)y + "," + (int)z);
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
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;
				
				this.getConfig().set("spawn.zombiehold", spawnloc);
				sender.sendMessage(ChatColor.GREEN + "Zombie hold set! " + (int)x + "," + (int)y + "," + (int)z);
				//getLogger().info("pitch: " + l.getPitch());
				//getLogger().info("yaw: " + l.getYaw());
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				getLogger().info("You must be in-game to run this!");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("human-hold"))
		{
			String spawnloc = this.getConfig().getString("spawn.humanhold");
			sender.sendMessage(ChatColor.RED + "Human hold cell:");
			int cutoff = spawnloc.indexOf(";");
			String coord = spawnloc.substring(0, cutoff);
			sender.sendMessage(coord);
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("zombie-hold"))
		{
			String spawnloc = this.getConfig().getString("spawn.zombiehold");
			sender.sendMessage(ChatColor.GREEN + "Zombie hold cell:");
			int cutoff = spawnloc.indexOf(";");
			String coord = spawnloc.substring(0, cutoff);
			sender.sendMessage(coord);
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("clear-human-hold"))
		{
			List<String> spawnloc = null;
			this.getConfig().set("spawn.humanhold", spawnloc);
			sender.sendMessage(ChatColor.RED + "Human hold cell cleared!");
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("clear-zombie-hold"))
		{
			List<String> spawnloc = null;
			this.getConfig().set("spawn.zombiehold", spawnloc);
			sender.sendMessage(ChatColor.GREEN + "Zombie hold cell cleared!");
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
				clearList("inventory.human");
				List<ItemStack> inventory = Arrays.asList(p.getInventory().getContents());
				this.getConfig().set("inventory.human", inventory);
				getLogger().info(this.getConfig().getList("inventory.human").toString());
				
				//save armor
				clearList("armor.human");
				List<ItemStack> armor = Arrays.asList(p.getInventory().getArmorContents());
				this.getConfig().set("armor.human", armor);
				getLogger().info(this.getConfig().getList("armor.human").toString());
				
				
				//clear existing loadout
				clearList("potion.human");
				this.getConfig().set("potion.human", p.getActivePotionEffects());
				
				p.sendMessage(ChatColor.GREEN + "Human loadout saved!");
				
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
				clearList("inventory.zombie");
				List<ItemStack> inventory = Arrays.asList(p.getInventory().getContents());
				this.getConfig().set("inventory.zombie", inventory);
				
				//save armor
				clearList("armor.zombie");
				List<ItemStack> armor = Arrays.asList(p.getInventory().getArmorContents());
				this.getConfig().set("armor.zombie", armor);
				getLogger().info(this.getConfig().getList("armor.zombie").toString());
				
				clearList("potion.zombie");
				this.getConfig().set("potion.zombie", p.getActivePotionEffects());
				
				p.sendMessage(ChatColor.GREEN + "Zombie loadout saved!");
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				getLogger().info("You must be in-game to run this!");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("human-loadout"))
		{
			sender.sendMessage(ChatColor.RED + "Human inventory:");
			int index = 0;
			while (index < 36)
			{
				ItemStack i = (ItemStack)(this.getConfig().getList("inventory.human").get(index));
				if (i != null)
				{
					sender.sendMessage(ChatColor.LIGHT_PURPLE + i.getType().toString() + " x" + i.getAmount());
				}
				index++;
			}
			sender.sendMessage(ChatColor.RED + "Human armor:");
			index = 0;
			while (index < 4)
			{
				ItemStack i = (ItemStack)(this.getConfig().getList("armor.human").get(index));
				if (i != null)
				{
					sender.sendMessage(ChatColor.LIGHT_PURPLE + i.getType().toString());
				}
				index++;
			}
			sender.sendMessage(ChatColor.RED + "Human potion effects:");
			//assuming all entries are PotionEffects
			for (Object entry: this.getConfig().getList("potion.human"))
			{
				PotionEffect pe = (PotionEffect)entry;
				sender.sendMessage(ChatColor.LIGHT_PURPLE + pe.getType().getName() + " " + (pe.getAmplifier()+1));
			}
			return true;
		}

		else if (cmd.getName().equalsIgnoreCase("zombie-loadout"))
		{
			sender.sendMessage(ChatColor.GREEN + "Zombie inventory:");
			int index = 0;
			while (index < 36)
			{
				ItemStack i = (ItemStack)this.getConfig().getList("inventory.zombie").get(index);
				if (i != null)
				{
					sender.sendMessage(ChatColor.AQUA + i.getType().toString() + " x" + i.getAmount());
				}
				index++;
			}
			sender.sendMessage(ChatColor.RED + "Zombie armor:");
			index = 0;
			while (index < 4)
			{
				ItemStack i = (ItemStack)(this.getConfig().getList("armor.zombie").get(index));
				if (i != null)
				{
					sender.sendMessage(ChatColor.LIGHT_PURPLE + i.getType().toString());
				}
				index++;
			}
			sender.sendMessage(ChatColor.GREEN + "Zombie potion effects:");
			for (Object entry: this.getConfig().getList("potion.zombie"))
			{
				PotionEffect pe = (PotionEffect)entry;
				sender.sendMessage(ChatColor.LIGHT_PURPLE + pe.getType().getName() + " " + (pe.getAmplifier()+1));
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("save-leavespawn"))
		{
			if (sender instanceof Player)
			{
				Player p = (Player)sender;
				Location l = p.getLocation();
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;

				this.getConfig().set("spawn.leave", spawnloc);
				
				//List<String> loc = this.getConfig().getStringList("spawn.wait");
				//loc.clear();
				//loc.add(spawnloc);
				sender.sendMessage(ChatColor.RED + "Leave point set! " + (int)x + "," + (int)y + "," + (int)z);
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				getLogger().info("You must be in-game to run this!");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("leavespawn"))
		{
			String spawnloc = this.getConfig().getString("spawn.leave");
			sender.sendMessage(ChatColor.RED + "Leave point:");
			int cutoff = spawnloc.indexOf(";");
			String coord = spawnloc.substring(0, cutoff);
			sender.sendMessage(coord);
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("save-waitspawn"))
		{
			if (sender instanceof Player)
			{
				Player p = (Player)sender;
				Location l = p.getLocation();
				double x = l.getBlockX() + 0.5;
				double y = l.getBlockY();
				double z = l.getBlockZ() + 0.5;
				float pitch = l.getPitch();
				float yaw = l.getYaw();
				String spawnloc = x + "," + y + "," + z + ";" + pitch + "," + yaw;
				this.getConfig().set("spawn.wait", spawnloc);

				//List<String> loc = this.getConfig().getStringList("spawn.wait");
				//loc.clear();
				//loc.add(spawnloc);
				sender.sendMessage(ChatColor.RED + "Wait point set! " + (int)x + "," + (int)y + "," + (int)z);
			}
			else if (sender instanceof ConsoleCommandSender)
			{
				getLogger().info("You must be in-game to run this!");
			}
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("waitspawn"))
		{
			String spawnloc = this.getConfig().getString("spawn.wait");
			sender.sendMessage(ChatColor.RED + "Wait point:");
			int cutoff = spawnloc.indexOf(";");
			String coord = spawnloc.substring(0, cutoff);
			sender.sendMessage(coord);
			return true;
		}
		
		else if (cmd.getName().equalsIgnoreCase("joingame"))
		{
			if (sender instanceof Player)
			{
				Player p = (Player)sender;
				if (!isInGame(p))
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
					String locstring = this.getConfig().getString("spawn.wait");
					if (locstring == null || locstring.length() == 0)
					{
						p.sendMessage(ChatColor.RED + "Tell your admin to save a wait point first!");
					}
					else
					{
						Location l = getLocFromString(locstring, p);
						p.teleport(l);
						joinWaiting(p);
					}
				}
				else
				{
					p.sendMessage(ChatColor.YELLOW + "You're already on a team!");
					playerRosterReport(p);
				}
				return true;
			}
			
		}
		
		else if (cmd.getName().equalsIgnoreCase("infection-start"))
		{
			if (args.length != 0 && args.length != 1)
			{
				sender.sendMessage(ChatColor.RED + "Usage: /infection-start [gameTime]");
				return true;
			}
			if (args.length == 0)
			{
				startGame(sender, DEFAULT_GAME_TIME);  //default game time is 1 minute
				getLogger().info("Starting new Infection game for " + DEFAULT_GAME_TIME + " seconds.");
			}
			else if (args.length == 1)
			{
				startGame(sender, Integer.parseInt(args[0])); //NOTE: This breaks if argument is not an integer!

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
	
	public void rosterReport()
	{
		getLogger().info("Spectator team");
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			getLogger().info(name);
		}
		getLogger().info("Waiting team");
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			getLogger().info(name);
		}
		getLogger().info("Human team");
		for (String name: this.getConfig().getStringList("team.human"))
		{
			getLogger().info(name);
		}
		getLogger().info("Zombie team");
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			getLogger().info(name);
		}
	}
	
	public void playerRosterReport(Player p)
	{
		String s = "";
		s += ChatColor.RED + "Humans: " + ChatColor.LIGHT_PURPLE;
		for (String name: this.getConfig().getStringList("team.human"))
		{
			s += name + " ";
		}
		s += "\n";
		
		s += ChatColor.GREEN + "Zombies: " + ChatColor.AQUA;
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			s += name + " ";
		}
		s += "\n";
		
		s += ChatColor.BLUE + "Spectators: " + ChatColor.GRAY;
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			s += name + " ";
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			s += name + " ";
		}
		s += "\n";
		
		
		
		//"You are on the <> team"
		if (isHuman(p))
		{
			s += ChatColor.RED + "You are on the HUMAN team.";
		}
		else if (isZombie(p))
		{
			s += ChatColor.GREEN + "You are on the ZOMBIE team.";
		}
		else if (isSpectator(p))
		{
			s += ChatColor.BLUE + "You are on the SPECTATOR team.";
		}
		else if (isWaiting(p))
		{
			s += ChatColor.RED + "You are on the SPECTATOR team and " + ChatColor.YELLOW + "WAITING TO PLAY.";
		}
		
		
		
		p.sendMessage(s);
	}
	
	public boolean isSpectator(Player p)
	{
		if (this.getConfig().getStringList("team.spectator").contains(p.getName()))
		{
			return true;
		}
		return false;
	}
	
	public boolean isWaiting(Player p)
	{
		if (this.getConfig().getStringList("team.waiting").contains(p.getName()))
		{
			return true;
		}
		return false;
	}
	
	public boolean isHuman(Player p)
	{
		if (this.getConfig().getStringList("team.human").contains(p.getName()))
		{
			return true;
		}
		return false;
	}
	
	public boolean isZombie(Player p)
	{
		if (this.getConfig().getStringList("team.zombie").contains(p.getName()))
		{
			return true;
		}
		return false;
	}
	
	public boolean isInGame(Player p)
	{
		return (isZombie(p)||isHuman(p)||isSpectator(p)||isWaiting(p));
	}
	
	
	//Code below regarding searching and updating lists is not very optimized for all four of these
	public void joinSpectator(Player p)
	{
		if (isSpectator(p))
		{
			p.sendMessage(ChatColor.RED + "You are already on the spectator team!");
			return;
		}
		
		for (String name: this.getConfig().getStringList("team.human"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.human", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.zombie", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.waiting", name);
			}
		}
		
		addToList("team.spectator", p.getName());
		p.sendMessage(ChatColor.BLUE + "You have joined the Spectators!");
	}
	
	public void joinHuman(Player p)
	{
		if (isHuman(p))
		{
			p.sendMessage(ChatColor.RED + "You are already on the human team!");
			return;
		}
		
		Location l = getSpawn("spawn.human", p);
		if (l == null)
		{
			p.sendMessage("ERROR: Tell your admin to save human spawn points!");
			p.teleport((Location)this.getConfig().get("spawn.wait"));
		}
		else
		{
			p.teleport(l);
		}
		
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.spectator", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.zombie", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.waiting", name);
			}
		}
		addToList("team.human", p.getName());
		applyHumanLoadout(p);
		p.sendMessage(ChatColor.RED + "You have joined the Humans!");
	}
	
	public void joinWaiting(Player p)
	{
		silentJoinWaiting(p);
		p.sendMessage(ChatColor.YELLOW + "You are now waiting to play!");
	}
	
	public void silentJoinWaiting(Player p)
	{
		if (isWaiting(p))
		{
			p.sendMessage(ChatColor.RED + "You are already waiting to play!");
			return;
		}
		
		for (String name: this.getConfig().getStringList("team.human"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.human", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.zombie", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.spectator", name);
			}
		}
		
		addToList("team.waiting", p.getName());
	}
	
	public void joinZombie(Player p)
	{
		if (isZombie(p))
		{
			p.sendMessage(ChatColor.RED + "You are already on the zombie team!");
			return;
		}
		
		Location l = getSpawn("spawn.zombie", p);
		if (l == null)
		{
			p.sendMessage("ERROR: Tell your admin to save human spawn points!");
			p.teleport((Location)this.getConfig().get("spawn.wait"));
		}
		else
		{
			p.teleport(l);
		}
		
		for (String name: this.getConfig().getStringList("team.human"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.human", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.spectator", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.waiting", name);
			}
		}
		
		addToList("team.zombie", p.getName());
		applyZombieLoadout(p);
		p.sendMessage(ChatColor.GREEN + "You have joined the Zombies!");
	}
	
	public void quietLeaveGame(Player p)
	{
		if (!(isZombie(p)||isHuman(p)||isSpectator(p)||isWaiting(p)))
		{
			p.sendMessage(ChatColor.RED + "You are currently not in the game!");
			return;
		}
		
		for (String name: this.getConfig().getStringList("team.human"))
		{
			if (name.equals(p.getName()))
			{
				p.getInventory().clear();
				removeFromList("team.human", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.spectator", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			if (name.equals(p.getName()))
			{
				removeFromList("team.waiting", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			if (name.equals(p.getName()))
			{
				p.getInventory().clear();
				removeFromList("team.zombie", name);
			}
		}
	}
	
	public void leaveGame(Player p)
	{
		quietLeaveGame(p);
		String locstring = this.getConfig().getString("spawn.leave");
		if (locstring == null || locstring.length() == 0)
		{
			p.sendMessage(ChatColor.RED + "Sent to your spawn point.  Tell your admin to save a leave point!");
			p.teleport(p.getBedSpawnLocation());
		}
		else
		{
			Location l = getLocFromString(locstring, p);
			p.teleport(l);
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
		for (String name: this.getConfig().getStringList("team.human"))
		{
			if (name.equals(op.getName()))
			{
				removeFromList("team.human", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.spectator"))
		{
			if (name.equals(op.getName()))
			{
				removeFromList("team.spectator", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
			if (name.equals(op.getName()))
			{
				removeFromList("team.waiting", name);
			}
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			if (name.equals(op.getName()))
			{
				removeFromList("team.zombie", name);
			}
		}
	}
	
	public void addToList(String path, String o)
	{
		List<String> newlist = this.getConfig().getStringList(path);
		newlist.add(o);
		this.getConfig().set(path, newlist);
	}
	public void removeFromList(String path, String o)
	{
		List<String> newlist = this.getConfig().getStringList(path);
		newlist.remove(o);
		this.getConfig().set(path, newlist);
	}
	public void clearList(String path)
	{
		List<String> newlist = this.getConfig().getStringList(path);
		newlist.clear();
		this.getConfig().set(path, newlist);
	}
	
	public void setList(String path, String o)
	{
		this.getConfig().getStringList(path).clear();
		addToList(path, o);
	}
	
	//DEPRECATED
	public void addPotionEffect(String path, PotionEffect o)
	{
		//Modify this code to deal with PotionEffects directly
		//List<PotionEffect> newlist = (List<PotionEffect>)this.getConfig().getList(path);
		//newlist.add(o);
		//this.getConfig().set(path, newlist);
	}
	
	//converts a coordinate string as stored in config into a Location
	public Location getLocFromString(String locstring, Player p)
	{
		int div = locstring.indexOf(";");
		String space = locstring.substring(0, div);
		String dir = locstring.substring(div+1, locstring.length());
		
		int first = space.indexOf(",");
		int last = space.lastIndexOf(",");
		double x = Double.parseDouble(space.substring(0, first));
		double y = Double.parseDouble(space.substring(first+1, last));
		double z = Double.parseDouble(space.substring(last+1, space.length()));
		
		int first1 = dir.indexOf(",");
		float pitch = Float.parseFloat(dir.substring(0, first1));
		float yaw = Float.parseFloat(dir.substring(first1+1, dir.length()));
		//getLogger().info("pitch: " + dir.substring(0, first1));
		//getLogger().info("yaw: " + dir.substring(first1+1, dir.length()));
		
		return new Location(p.getWorld(), x, y, z, yaw, pitch);
		
	}
	
	public Location getHoldLoc(String path, Player p)
	{
		return getLocFromString(this.getConfig().getString(path), p);
	}
	
	public Location getSpawn(String path, Player p)
	{
		int listsize = this.getConfig().getStringList(path).size();
		if (listsize == 0)
		{
			getLogger().info("ERROR: No spawns listed for path " + path + "!");
			return null;
		}
		
		int index = (int)(Math.random()*listsize);
		String loc = this.getConfig().getStringList(path).get(index);
		return getLocFromString(loc, p);
	}
	
	public String getPotionEffectName(String s)
	{
		String t = s;
		t = t.toLowerCase();
		t = t.replace('_', ' ');
		return t;
	}
	
	public void applyHumanLoadout(Player p)
	{
		p.getInventory().setContents((ItemStack[])this.getConfig().getList("inventory.human").toArray(new ItemStack[p.getInventory().getContents().length]));
		p.getInventory().setArmorContents((ItemStack[])this.getConfig().getList("armor.human").toArray(new ItemStack[p.getInventory().getArmorContents().length]));
		/*
		p.getInventory().clear();
		int index = 0;
		while (index < 36)
		{
			if (this.getConfig().getList("inventory.human") == null)
			{
				p.sendMessage(ChatColor.RED + "You don't have a kit saved!  Clearing inventory...");
				p.getInventory().clear();
				break;
			}
			ItemStack i = (ItemStack)this.getConfig().getList("inventory.human").get(index);
			if (i != null)
			{
				p.getInventory().setItem(index, i);
			}
			index++;
		}
		*/
		
		//assuming all entries are PotionEffects
		if (this.getConfig().getList("potion.human") != null)
		{
			for (Object entry: this.getConfig().getList("potion.zombie"))
			{
				PotionEffect pe = (PotionEffect)entry;
				PotionEffect newpot = new PotionEffect(pe.getType(), 7200*20, pe.getAmplifier());
				p.addPotionEffect((PotionEffect)newpot);
			}
		}
	}
	
	public void applyZombieLoadout(Player p)
	{
		p.getInventory().setContents((ItemStack[])this.getConfig().getList("inventory.zombie").toArray(new ItemStack[p.getInventory().getContents().length]));
		p.getInventory().setArmorContents((ItemStack[])this.getConfig().getList("armor.zombie").toArray(new ItemStack[p.getInventory().getArmorContents().length]));
		/*
		p.getInventory().clear();
		int index = 0;
		while (index < 36)
		{
			if (this.getConfig().getList("inventory.zombie") == null)
			{
				p.sendMessage(ChatColor.RED + "You don't have a kit saved!  Clearing inventory...");
				p.getInventory().clear();
				break;
			}
			ItemStack i = (ItemStack)this.getConfig().getList("inventory.zombie").get(index);
			if (i != null)
			{
				p.getInventory().setItem(index, i);
			}
			index++;
		}*/
		
		//assuming all entries are PotionEffects
		if (this.getConfig().getList("potion.zombie") != null)
		{
			for (Object entry: this.getConfig().getList("potion.zombie"))
			{
				PotionEffect pe = (PotionEffect)entry;
				PotionEffect newpot = new PotionEffect(pe.getType(), 7200*20, pe.getAmplifier());
				p.addPotionEffect((PotionEffect)newpot);
			}
		}
	}
	
	public void startGame(CommandSender cs, int gameTimeSeconds)
	{
		
		if (gameActive)
		{
			cs.sendMessage("The game is already in progress!");
			return;
		}
		else
		{
			List<String> playersin = this.getConfig().getStringList("team.waiting");
			for (String name: this.getConfig().getStringList("team.zombie"))
			{
				playersin.add(name);
			}
			clearList("team.zombie");
			for (String name: this.getConfig().getStringList("team.human"))
			{
				playersin.add(name);
			}
			clearList("team.human");
			this.getConfig().set("team.waiting", playersin);
			
			//check that spawn points exist
			if (this.getConfig().getStringList("spawn.human").size() == 0 || this.getConfig().getStringList("spawn.zombie").size() == 0)
			{
				getServer().broadcastMessage(ChatColor.DARK_RED + "ERROR: Human or Zombie spawn points have not been set!  The game cannot start.");
				return;
			}
			if (this.getConfig().get("spawn.humanhold") == null || this.getConfig().get("spawn.zombiehold") == null)
			{
				getServer().broadcastMessage(ChatColor.DARK_RED + "ERROR: Human or Zombie hold points have not been set!  The game cannot start.");
				return;
			}
			if (this.getConfig().get("inventory.human") == null)
			{
				getServer().broadcastMessage(ChatColor.DARK_RED + "ERROR: Human inventory has not been set.  The game cannot start.");
				return;
			}
			if (this.getConfig().get("inventory.zombie") == null)
			{
				getServer().broadcastMessage(ChatColor.DARK_RED + "ERROR: Zombie inventory has not been set.  The game cannot start.");
				return;
			}
			
			
			//Team sorting
			int num = this.getConfig().getStringList("team.waiting").size();
			int zombienum = num / 6 + 1;
			List<String> players = this.getConfig().getStringList("team.waiting");
			List<String> zombies = this.getConfig().getStringList("team.zombie");
			while (zombienum != 0)
			{
				//Randomly choose an index given the size of the list of players
				int index = (int)(Math.random()*playersin.size());
				zombies.add(players.get(index));
				players.remove(players.get(index));
				zombienum--;
			}
			
			for (String name: players)
			{
				Player p = (Player)this.getServer().getOfflinePlayer(name);
				joinHuman(p);
			}
			for (String name: zombies)
			{
				Player p = (Player)this.getServer().getOfflinePlayer(name);
				joinZombie(p);
			}
			
			
			final FileConfiguration config = this.getConfig();
			Runnable scheduleGameEnd = new Runnable(){
				
				public void run(){
					if (config.getStringList("team.human").size() > 0)
					{
						getServer().broadcastMessage(ChatColor.RED + "The humans have won! Game over!");
					}
					else
					{
						getServer().broadcastMessage(ChatColor.GREEN + "The zombies have won! Game over!");
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
		for (String name: this.getConfig().getStringList("team.human"))
		{
			removeFromList("team.human", name);
			addToList("team.waiting", name);
		}
		for (String name: this.getConfig().getStringList("team.zombie"))
		{
			removeFromList("team.zombie", name);
			addToList("team.waiting", name);
		}
		for (String name: this.getConfig().getStringList("team.waiting"))
		{
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
				quietLeaveGame(p);
				silentJoinWaiting(p);
				Location l = getLocFromString(this.getConfig().getString("spawn.wait"), p);
				p.teleport(l);
			}
		}
	}
}
