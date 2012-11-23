package com.github.etsija;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;

public class MagicTouch extends JavaPlugin {

	private Logger _log = Logger.getLogger("Minecraft");
	private Consumer lbConsumer = null;
	
	// Params in config.yml
	private int magicalTool;
	private Boolean useWG;
	private Boolean useLB;
	private Boolean debug;
	private Boolean rotateLogs;
	private Boolean rotateStairs;
	private Boolean rotatePistons;
	private Boolean rotateChests;
	private Boolean rotateSlabs;
	private Boolean rotateDiodes;
	
	// Connect to WG plugin to respect build-restricted areas
	private WorldGuardPlugin getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
		// WorldGuard may not be loaded
		if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
			return null; // Maybe you want throw an exception instead
		}
		return (WorldGuardPlugin) plugin;
	}
	
	public void onEnable() {
		// Register the listener for the tool to use
		getServer().getPluginManager().registerEvents(new pListener(), this);
		// Register the block listener which cancels block damage when clicking
		getServer().getPluginManager().registerEvents(new bListener(), this);
		
		// Configure the plugin
		processConfigFile();
		
		final PluginManager pm = getServer().getPluginManager();
		final Plugin plugin = pm.getPlugin("LogBlock");
		if (plugin != null) {
			lbConsumer = ((LogBlock)plugin).getConsumer();
			_log.info("[MagicTouch] hooked into LogBlock");
		}
		
		_log.info("[MagicTouch] has been enabled!");
	}
	 
	public void onDisable() { 
		_log.info("[MagicTouch] has been disabled!");
	}

	public void processConfigFile() {

		final Map<String, Object> defParams = new HashMap<String, Object>();
		FileConfiguration config = this.getConfig();
		config.options().copyDefaults(true);
		
		// This is the default configuration
		defParams.put("general.magicaltool", 58);
		defParams.put("general.use_worldguard", "true");
		defParams.put("general.use_logblock", "true");
		defParams.put("general.debug", "true");
		defParams.put("blocks.logs", "true");
		defParams.put("blocks.stairs", "true");
		defParams.put("blocks.pistons", "false");
		defParams.put("blocks.chests", "true");
		defParams.put("blocks.slabs", "true");
		defParams.put("blocks.diodes", "true");
		
		// If config does not include a default parameter, add it
		for (final Entry<String, Object> e : defParams.entrySet())
			if (!config.contains(e.getKey()))
				config.set(e.getKey(), e.getValue());
		
		// Save default values to config.yml in datadirectory
		this.saveConfig();
		
		magicalTool = getConfig().getInt("general.magicaltool");
		useWG = getConfig().getBoolean("general.use_worldguard");
		useLB = getConfig().getBoolean("general.use_logblock");
		debug = getConfig().getBoolean("general.debug");
		rotateLogs = getConfig().getBoolean("blocks.logs");
		rotateStairs = getConfig().getBoolean("blocks.stairs");
		rotatePistons = getConfig().getBoolean("blocks.pistons");
		rotateChests = getConfig().getBoolean("blocks.chests");
		rotateSlabs = getConfig().getBoolean("blocks.slabs");
		rotateDiodes = getConfig().getBoolean("blocks.diodes");
	}
	
	// This is the block listener which cancels block damage when clicking with the magical tool
	public class bListener implements Listener {

		//private int magicalTool = getConfig().getInt("general.magicaltool");
		private List<String> brPlayersList = new ArrayList<String>();
		
		@EventHandler
		public void onBlockBreak(BlockBreakEvent event) {
			
			if (event.isCancelled()) return;
			
			Player player = event.getPlayer();
			int itemInHand = player.getItemInHand().getTypeId();
			
			if (itemInHand == magicalTool) {
				
				_log.info("BlockBreakEvent detected!");
				event.setCancelled(true);
			}
		}
	}
	
	// This is the listener which listens to right-click of the chosen tool
	public class pListener implements Listener {

		private boolean shiftClick = false;
		
		// Read configuration params from config.yml

		@EventHandler
		public void onPlayerInteract(PlayerInteractEvent event) {
			
			if (event.isCancelled()) return;
			
			// Only left-click does something
			final Action action = event.getAction();
			if (action != Action.LEFT_CLICK_BLOCK) {
				return;
			}

			// Main loop to handle left-clicking with the magic tool
			Player player = event.getPlayer();
			int itemInHand = player.getItemInHand().getTypeId();
			
			if (itemInHand == magicalTool) {
				
				// Did the player shift-click?
				shiftClick = player.isSneaking();
				
				Block block = event.getClickedBlock();
				Material material = block.getType();
				
				// Can you actually build in this area? Let WG decide
				if (useWG) {
					if (!getWorldGuard().canBuild(player, block)) {
						player.sendMessage(ChatColor.RED + "[MagicTouch] You cannot build in this area!");
						return;
					}
				}
				
				if (debug)
					_log.info("Type = " + material);
				
				// ----------------------------------------------------------------------
				// Main loop for different clickable materials
				// ----------------------------------------------------------------------
				
				Boolean validBlock = false;
				
				// LOGS:
				// U/D -> E/W -> N/S -> U/D...
				if (rotateLogs && 
				   (material == Material.LOG)) {
					validBlock = true;
					byte data = block.getData();
					if (data < 4) {
						block.setData((byte)(data + 4));
					} else if (data < 8) {
	                    block.setData((byte)(data + 4));
	                } else if (data < 12){
	                    block.setData((byte)(data - 8));
	                }
					
				// STAIRS: 
				// N(3) -> E(0) -> S(2) -> W(1) -> N(3)...
				// With shift-click, flip stairs upside down
				} else if (rotateStairs && (
						  (material == Material.WOOD_STAIRS) ||
						  (material == Material.SPRUCE_WOOD_STAIRS) ||
						  (material == Material.BIRCH_WOOD_STAIRS) ||
						  (material == Material.JUNGLE_WOOD_STAIRS) ||
						  (material == Material.COBBLESTONE_STAIRS) ||
						  (material == Material.SMOOTH_STAIRS) ||
						  (material == Material.SANDSTONE_STAIRS) ||
						  (material == Material.BRICK_STAIRS) ||
						  (material == Material.NETHER_BRICK_STAIRS))) {
					validBlock = true;
					byte data = block.getData();
                    int flipStatus = (data & 0x4) >> 2; // Save bit 3 (0x3)
					int orientation = data & 0x3;
                    if (shiftClick) {
                        // Flip the block upside down
                        flipStatus = ~flipStatus;
                    } else {
                    	// Rotate the block
                        switch (orientation) {
                            case 3:
                                orientation = 0;
                                break;
                            case 0:
                                orientation = 2;
                                break;
                            case 2:
                                orientation = 1;
                                break;
                            case 1:
                                orientation = 3;
                                break;
                        }
                    }
                    // Combine flipStatus (bit 3) and orientation (bits 2-0)
                    data = (byte) ((flipStatus << 2) | orientation);
					block.setData(data);

				// PISTONS:
				// N(2) -> E(5) -> S(3) -> W(4) -> U(1) -> D(0) -> N(2)...
				} else if (rotatePistons && (
						  (material == Material.PISTON_BASE) ||
						  (material == Material.PISTON_STICKY_BASE))) {
					validBlock = true;
					byte data = block.getData();
					int isExtended = (data & 0x8) >> 3; // Save bit 4 (0x8) = extension status
					int orientation = data & 0x7;
					switch (orientation) {
						case 2:
							orientation = 5;
							break;
						case 5:
							orientation = 3;
							break;
						case 3:
							orientation = 4;
							break;
						case 4:
							orientation = 1;
							break;
						case 1:
							orientation = 0;
							break;
						case 0:
							orientation = 2;
							break;
					}
					// Combine extension status (bit 4) and orientation (bits 3-0)
					data = (byte) ((isExtended << 3) | orientation);
					block.setData(data);
		
				// CHESTS, FURNACES, DISPENSERS:
				// N(2) -> E(5) -> S(3) -> W(4) -> N(2)...
				} else if (rotateChests && (
						  (material == Material.CHEST) ||
						  (material == Material.ENDER_CHEST) ||
						  (material == Material.FURNACE) ||
						  (material == Material.DISPENSER))) {
					validBlock = true;
					byte data = block.getData();
					switch (data) {
						case 2:
							block.setData((byte) 5);
							break;
						case 5:
							block.setData((byte) 3);
							break;
						case 3:
							block.setData((byte) 4);
							break;
						case 4:
							block.setData((byte) 2);
							break;
					}
				
				// SLABS:
				// right-side up -> upside down -> ...
				} else if (rotateSlabs && (material == Material.STEP)) {
					validBlock = true;
					byte data = block.getData();
					if (data < 8) {
						block.setData((byte)(data + 8));
					} else {
	                    block.setData((byte)(data - 8));
	                }

				// DIODES:
				// N(0) -> E(1) -> S(2) -> W(3) -> N(0)...
				} else if (rotateDiodes && (
						  (material == Material.DIODE_BLOCK_OFF) ||
						  (material == Material.DIODE_BLOCK_ON))) {
					validBlock = true;
					byte data = block.getData();
					block.setData((byte)((data + 1) % 4));
				}				
				
				// LogBlock logging in use and Player has clicked a valid block to rotate
				// so log your change with LogBlock
				if (validBlock && useLB) {
					int typeAfter = 0;
					byte dataAfter = 0;
					lbConsumer.queueBlockReplace(player.getName(), block.getState(), typeAfter, dataAfter);
				}
			}
		}
	}
}