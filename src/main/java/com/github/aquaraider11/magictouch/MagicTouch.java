package com.github.aquaraider11.magictouch;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class MagicTouch extends JavaPlugin {

    private Logger _log = Logger.getLogger("Minecraft");

    @Override
    public void onEnable() {
        getLogger().info("onEnable has been invoked!");
        //getServer().getPluginManager().registerEvents(new EventHandler(), this);
        getServer().getPluginManager().registerEvents(new bListener(), this);
        getServer().getPluginManager().registerEvents(new pListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable has been invoked!");
    }

    public class bListener implements Listener {

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {

            if (event.isCancelled()) return;

            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.getType().equals(Material.CRAFTING_TABLE)) {
                Block block = event.getBlock();

                if (wgCantBuild(player, block))
                    return;
                if (!isSpecialBlock(block))
                    return;
                if (!player.hasPermission("magically.touch.things"))
                    return;
                event.setCancelled(true);

            }
        }

    }

    private boolean isSpecialBlock(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Bisected)
            return true;
        if (blockData instanceof Directional)
            return true;
        if (blockData instanceof Slab)
            return true;
        if (blockData instanceof Orientable)
            return true;
        else
            return false;
    }

    private boolean wgCantBuild(Player player, Block block) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        Location loc = BukkitAdapter.adapt(block.getLocation());

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        boolean canBypass = WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
        if (canBypass)
            return false;
        return !query.testState(loc, localPlayer, Flags.BUILD);
    }


    public class pListener implements Listener {

        private boolean shiftClick = false;

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Only left-click does something
            final Action action = event.getAction();
            if (action != Action.LEFT_CLICK_BLOCK) {
                return;
            }

            // Main loop to handle left-clicking with the magic tool
            Player player = event.getPlayer();
            if (!player.hasPermission("magically.touch.things"))
                return;
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.getType().equals(Material.CRAFTING_TABLE)) {

                // Did the player shift-click?
                shiftClick = player.isSneaking();

            } else return;


            Block block = event.getClickedBlock();
            BlockData blockData = block.getBlockData();
            BlockFace blockFace = event.getBlockFace();

            if (wgCantBuild(player, block))
                return;

            if (!shiftClick) {
                if (blockData instanceof Orientable)
                    block.setBlockData(orientableFunc((Orientable) blockData, player, shiftClick));
                if (blockData instanceof Directional)
                    block.setBlockData(directionalFunc((Directional) blockData));
            } else {
                if (blockData instanceof Directional && !(blockData instanceof Bisected)) {
                    ((Directional) blockData).setFacing(blockFace);
                    block.setBlockData(blockData);
                }
                if (blockData instanceof Orientable)
                    block.setBlockData(orientableFunc((Orientable) blockData, player, shiftClick));
                if (blockData instanceof Slab)
                    block.setBlockData(slabFunc((Slab) blockData));
                if (blockData instanceof Bisected)
                    block.setBlockData(bisectedFunc((Bisected) blockData));
            }
        }

        private BlockData bisectedFunc(Bisected blockData) {
            try {
                Bisected.Half h = blockData.getHalf();
                if (h.equals(Bisected.Half.BOTTOM)) {
                    blockData.setHalf(Bisected.Half.TOP);
                }
                if (h.equals(Bisected.Half.TOP)) {
                    blockData.setHalf(Bisected.Half.BOTTOM);
                }

            } catch (Exception e) {
                _log.info("ERROR: in Bisect Function"+e.toString());
            }
            return blockData;
        }

        private BlockData slabFunc(Slab blockData) {
            try {
                Slab.Type st = blockData.getType();
                if (st.equals(Slab.Type.BOTTOM)) {
                    blockData.setType(Slab.Type.TOP);
                }
                if (st.equals(Slab.Type.TOP)) {
                    blockData.setType(Slab.Type.BOTTOM);
                }

            } catch (Exception e) {
                _log.info("ERROR: in Slab Function"+e.toString());
            }
            return blockData;
        }

        private BlockData directionalFunc(Directional blockData) {
            try {
                BlockFace target;
                target = BlockFace.NORTH;
                ArrayList<BlockFace> faceSet = new ArrayList<>(blockData.getFaces());
                BlockFace prevFace = blockData.getFacing();


                try {
                    for (int i = 0; i < faceSet.size(); i++) {
                        BlockFace bf = faceSet.get(i);
                        if (bf.equals(prevFace) && i != faceSet.size() - 1)
                            target = faceSet.get(i + 1);
                        else if (bf.equals(prevFace) && i == faceSet.size() - 1)
                            target = faceSet.get(0);
                    }
                } catch (Exception f) {
                    _log.info("ERROR: in Directional Function"+f.toString());
                }

                try {
                    blockData.setFacing(target);
                } catch (Exception e) {
                    _log.info("ERROR: in Directional Function"+e.toString());
                }
            } catch (Exception g) {
                _log.info("ERROR: in Directional Function"+g.toString());
            }
            return blockData;
        }

        private BlockData orientableFunc(Orientable blockData, Player player, boolean shiftClick) {
            try {
                Axis a = blockData.getAxis();
                if (!shiftClick) {
                    switch (a) {
                        case X:
                            blockData.setAxis(Axis.Y);
                            break;
                        case Y:
                            blockData.setAxis(Axis.Z);
                            break;
                        case Z:
                            blockData.setAxis(Axis.X);
                            break;
                    }
                } else
                    blockData.setAxis(convertBlockFaceToAxis(getBlockFace(player)));
            } catch (Exception e) {
                _log.info("Error in Orientable: " + e);
            }
            return blockData;
        }


        private Axis convertBlockFaceToAxis(BlockFace face) {
            switch (face) {
                case NORTH:
                case SOUTH:
                    return Axis.Z;
                case EAST:
                case WEST:
                    return Axis.X;
                case UP:
                case DOWN:
                    return Axis.Y;
                default:
                    return Axis.X;
            }
        }

        /**
         * Gets the BlockFace of the block the player is currently targeting.
         *
         * @param player the player's whos targeted blocks BlockFace is to be checked.
         * @return the BlockFace of the targeted block, or null if the targeted block is non-occluding.
         */
        public BlockFace getBlockFace(Player player) {
            List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 100);
            if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding()) return null;
            Block targetBlock = lastTwoTargetBlocks.get(1);
            Block adjacentBlock = lastTwoTargetBlocks.get(0);
            return targetBlock.getFace(adjacentBlock);
        }
    }
}



