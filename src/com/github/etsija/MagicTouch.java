package com.github.etsija;

import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class MagicTouch extends JavaPlugin {

	private Logger _log = Logger.getLogger("Minecraft");
	
	// Connect to WG plugin to respect build-restricted areas
	private WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        return null; // Maybe you want throw an exception instead
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
	
	public void onEnable(){
		// Register the listener for the tool to use
		getServer().getPluginManager().registerEvents(new pListener(), this);
		_log.info("[MagicTouch] Has been enabled!");
	}
	 
	public void onDisable(){ 
		_log.info("[MagicTouch] Has been disabled!");
	}

	// This is the listener which listens to right-click of the chosen tool
	public class pListener implements Listener {

		@EventHandler
		public void onPlayerInteract(PlayerInteractEvent event) {
			
			if(event.isCancelled()) return;
			
			// Only left-click does something
			final Action action = event.getAction();
			if (action != Action.LEFT_CLICK_BLOCK) {
				return;
			}

			// Main loop to handle left-clicking with the magic tool
			Player player = event.getPlayer();
			int itemInHand = player.getItemInHand().getTypeId();
			if (itemInHand == 58) {
				
				Block block = event.getClickedBlock();
				Material material = block.getType();
				
				// Can you actually build in this area? Let WG decide
				if (!getWorldGuard().canBuild(player, block)) {
					player.sendMessage(ChatColor.RED + "[MagicTouch] You cannot build in this area!");
					return;
				}
				
				//_log.info("Type = " + material);
				
				// Main loop where different clicking actions could be put
				
				// Flip logs: U/D -> E/W -> N/S -> U/D...
				if (material == Material.LOG) {
					byte data = block.getData();
					if (data < 4) {
						block.setData((byte)(data + 4));
					} else if (data < 8) {
	                    block.setData((byte)(data + 4));
	                } else if (data < 12){
	                    block.setData((byte)(data - 8));
	                }
					
				// Flip stairs so they are ascending to
				//      normal: N(3) -> E(0) -> S(2) -> W(1) -> 
				// upside down: N(7) -> E(4) -> S(6) -> W(5) -> normal N(3)...
				} else if ((material == Material.WOOD_STAIRS) ||
						   (material == Material.SPRUCE_WOOD_STAIRS) ||
						   (material == Material.BIRCH_WOOD_STAIRS) ||
						   (material == Material.JUNGLE_WOOD_STAIRS) ||
						   (material == Material.COBBLESTONE_STAIRS) ||
						   (material == Material.SMOOTH_STAIRS) ||
						   (material == Material.SANDSTONE_STAIRS) ||
						   (material == Material.BRICK_STAIRS) ||
						   (material == Material.NETHER_BRICK_STAIRS)) {
					byte data = block.getData();
					switch (data) {
						case 3: // 
							block.setData((byte) 0);
							break;
						case 0:
							block.setData((byte) 2);
							break;
						case 2:
							block.setData((byte) 1);
							break;
						case 1:
							block.setData((byte) 7);
							break;
						case 7:
							block.setData((byte) 4);
							break;
						case 4:
							block.setData((byte) 6);
							break;
						case 6:
							block.setData((byte) 5);
							break;
						case 5:
							block.setData((byte) 3);
							break;
					}
				
				// Flip pistons: N(2) -> E(5) -> S(3) -> W(4) -> U(1) -> D(0) -> N(2)...
				//} else if ((material == Material.PISTON_BASE) ||
				//		   (material == Material.PISTON_STICKY_BASE)) {
				} else if (false) {
					byte data = block.getData();
					int isExtended = data & 8; // Save bit 4 (0x8) = extension status
					int orientation = data & 7;
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
					data = (byte) (isExtended | orientation);
					block.setData(data);
					
				// Flip chests, furnaces, dispensers: N(2) -> E(5) -> S(3) -> W(4) -> N(2)...
				} else if ((material == Material.CHEST) ||
						   (material == Material.ENDER_CHEST) ||
						   (material == Material.FURNACE) ||
						   (material == Material.DISPENSER)) {
					byte data = block.getData();
					switch (data) {
						case 2: // 
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
				
				// Flip slabs: right-side up -> upside down -> ...
				} else if (material == Material.STEP) {
					byte data = block.getData();
					if (data < 8) {
						block.setData((byte)(data + 8));
					} else {
	                    block.setData((byte)(data - 8));
	                }
				}
			}
		}
	}
}