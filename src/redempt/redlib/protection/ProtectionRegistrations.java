package redempt.redlib.protection;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Dispenser;
import redempt.redlib.RedLib;
import redempt.redlib.protection.ProtectionPolicy.ProtectionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ProtectionRegistrations {

    public static void registerProtections() {
        ProtectionListener.protect(BlockBreakEvent.class, ProtectionType.BREAK_BLOCK, e -> e.getPlayer(), e -> e.getBlock());
        ProtectionListener.protect(BlockPlaceEvent.class, ProtectionType.PLACE_BLOCK, e -> e.getPlayer(), e -> e.getBlock());
        ProtectionListener.protect(PlayerInteractEvent.class, ProtectionType.INTERACT, e -> e.getPlayer(), e -> {
            if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || (RedLib.MID_VERSION >= 13 && !e.getClickedBlock().getType().isInteractable())) {
                return null;
            }
            if (e.getClickedBlock().getState() instanceof InventoryHolder && !e.getPlayer().isSneaking()) {
                return null;
            }
            return e.getClickedBlock();
        });
        Set<String> farmlandNames = new HashSet<>();
        Collections.addAll(farmlandNames, "FARMLAND", "SOIL");
        ProtectionListener.protect(PlayerInteractEvent.class, ProtectionType.TRAMPLE, e -> e.getPlayer(), e -> {
           if (e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null && farmlandNames.contains(e.getClickedBlock().getType().toString())) {
               return e.getClickedBlock();
           }
           return null;
        });
        ProtectionListener.protect(InventoryOpenEvent.class, ProtectionType.CONTAINER_ACCESS, e -> (Player) e.getPlayer(), e -> getBlock(e.getInventory()));
        ProtectionListener.protectMultiBlock(EntityExplodeEvent.class, ProtectionType.ENTITY_EXPLOSION, e -> null, (e, b) -> e.blockList().remove(b), e -> e.blockList());
        ProtectionListener.protectMultiBlock(BlockExplodeEvent.class, ProtectionType.BLOCK_EXPLOSION, e -> null, (e, b) -> e.blockList().remove(b), e -> e.blockList());
        ProtectionListener.protect(PlayerBucketFillEvent.class, ProtectionType.USE_BUCKETS, e -> e.getPlayer(), e -> e.getBlockClicked());
        ProtectionListener.protect(PlayerBucketEmptyEvent.class, ProtectionType.USE_BUCKETS, e -> e.getPlayer(), e -> e.getBlockClicked());
        ProtectionListener.protectMultiBlock(BlockPistonExtendEvent.class, ProtectionType.PISTONS, e -> null, (e, b) -> e.setCancelled(true), e -> {
            List<Block> blocks = new ArrayList<>(e.getBlocks());
            blocks.add(e.getBlock());
            return blocks;
        });
        ProtectionListener.protectMultiBlock(BlockPistonRetractEvent.class, ProtectionType.PISTONS, e -> null, (e, b) -> e.setCancelled(true), e -> {
            List<Block> blocks = new ArrayList<>(e.getBlocks());
            blocks.add(e.getBlock());
            return blocks;
        });
        ProtectionListener.protectNonCancellable(BlockRedstoneEvent.class, ProtectionType.REDSTONE, e -> null, e -> e.setNewCurrent(e.getOldCurrent()), e -> e.getBlock());
        ProtectionListener.protect(EntityChangeBlockEvent.class, ProtectionType.FALLING_BLOCK, e -> null, e -> {
            if (!(e.getEntity() instanceof FallingBlock)) {
                return null;
            }
            return e.getBlock();
        });
        ProtectionListener.protect(EntityChangeBlockEvent.class, ProtectionType.SILVERFISH, e -> null, e -> {
            if (!(e.getEntity() instanceof Silverfish)) {
                return null;
            }
            return e.getBlock();
        });
        ProtectionListener.protect(EntityChangeBlockEvent.class, ProtectionType.WITHER, e -> null, e -> {
            if (!(e.getEntity() instanceof Wither)) {
                return null;
            }
            return e.getBlock();
        });
        ProtectionListener.protect(BlockGrowEvent.class, ProtectionType.GROWTH, e -> null, e -> e.getBlock());
        ProtectionListener.protect(BlockSpreadEvent.class, ProtectionType.GROWTH, e -> null, e -> e.getNewState().getType() == Material.FIRE ? null : e.getBlock());
        ProtectionListener.protect(BlockFormEvent.class, ProtectionType.GROWTH, e -> null, e -> e.getBlock());
        ProtectionListener.protect(BlockFadeEvent.class, ProtectionType.FADE, e -> null, e -> e.getBlock());
        ProtectionListener.protect(BlockFromToEvent.class, ProtectionType.FLOW, e -> null, e -> e.getBlock(), e -> e.getToBlock());
        ProtectionListener.protect(BlockBurnEvent.class, ProtectionType.FIRE, e -> null, e -> e.getBlock());
        ProtectionListener.protect(BlockSpreadEvent.class, ProtectionType.FIRE, e -> null, e -> e.getNewState().getType() == Material.FIRE ? e.getBlock() : null);
        ProtectionListener.protect(CreatureSpawnEvent.class, ProtectionType.MOB_SPAWN, e -> null, e -> {
            if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
                return null;
            }
            return e.getEntity().getLocation().getBlock();
        });
        ProtectionListener.protectMultiBlock(PortalCreateEvent.class, ProtectionType.PORTAL_PAIRING, e -> null, (e, b) -> e.setCancelled(true), e -> {
            if (e.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR) {
                return null;
            }
            return e.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toList());
        });
        ProtectionListener.protectDirectional(BlockFromToEvent.class, ProtectionType.FLOW_IN, e -> null, e -> e.getBlock(), e -> Collections.singletonList(e.getToBlock()));
        ProtectionListener.protectDirectional(BlockPistonExtendEvent.class, ProtectionType.PISTONS_IN, e -> null, e -> e.getBlock(), e -> e.getBlocks().stream().map(b -> b.getRelative(e.getDirection())).collect(Collectors.toList()));
        ProtectionListener.protectDirectional(BlockPistonRetractEvent.class, ProtectionType.PISTONS_IN, e -> null, e -> e.getBlock(), e -> e.getBlocks());
        ProtectionListener.protectMultiBlock(StructureGrowEvent.class, ProtectionType.STRUCTURE_GROWTH, e -> null, (e, b) -> e.setCancelled(true), e -> e.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toList()));
        ProtectionListener.protectDirectional(StructureGrowEvent.class, ProtectionType.STRUCTURE_GROWTH_IN, e -> null, e -> e.getLocation().getBlock(), e -> e.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toList()));
        ProtectionListener.protect(EntityBlockFormEvent.class, ProtectionType.ENTITY_FORM_BLOCK, e -> e.getEntity() instanceof Player ? (Player) e.getEntity() : null, e -> e.getBlock());
        ProtectionListener.protectDirectional(InventoryMoveItemEvent.class, ProtectionType.CONTAINER_ACCESS, e -> null, e -> getBlock(e.getSource()), e -> Collections.singletonList(getBlock(e.getDestination())));
        ProtectionListener.protect(PlayerInteractEntityEvent.class, ProtectionType.INTERACT_ENTITY, PlayerEvent::getPlayer, e -> e.getRightClicked() instanceof StorageMinecart ? null : e.getRightClicked().getLocation().getBlock());
        ProtectionListener.protect(PlayerInteractEntityEvent.class, ProtectionType.CONTAINER_ACCESS, PlayerEvent::getPlayer, e -> e.getRightClicked() instanceof StorageMinecart ? e.getRightClicked().getLocation().getBlock() : null);
        ProtectionListener.protect(PlayerArmorStandManipulateEvent.class, ProtectionType.INTERACT_ENTITY, PlayerEvent::getPlayer, e -> e.getRightClicked().getLocation().getBlock());
        ProtectionListener.protect(HangingPlaceEvent.class, ProtectionType.PLACE_ENTITY, HangingPlaceEvent::getPlayer, HangingPlaceEvent::getBlock);
        ProtectionListener.protect(PlayerInteractEvent.class, ProtectionType.PLACE_ENTITY, e -> e.getPlayer(), e -> {
            if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null) {
                return null;
            }
            Material type = e.getItem().getType();
            if (type.toString().endsWith("BOAT") || type.toString().endsWith("MINECART") || type == Material.ARMOR_STAND) {
                return e.getClickedBlock();
            }
            return null;
        });
        ProtectionListener.protect(HangingBreakByEntityEvent.class, ProtectionType.INTERACT_ENTITY, e -> e.getRemover() instanceof Player ? (Player) e.getRemover() : null, e -> e.getCause() == HangingBreakEvent.RemoveCause.ENTITY ? getBlock(e.getEntity()) : null);
        ProtectionListener.protect(HangingBreakByEntityEvent.class, ProtectionType.ENTITY_EXPLOSION, e -> null, e -> e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION ? getBlock(e.getEntity()) : null);
        Set<Material> bannedDispenserItems = EnumSet.of(Material.FLINT_AND_STEEL, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.JACK_O_LANTERN, Material.PUMPKIN, Material.ARMOR_STAND);
        Arrays.stream(Material.values()).filter(m -> m.toString().endsWith("MINECART") || m.toString().endsWith("BOAT") || m.toString().endsWith("_BUCKET")).forEach(bannedDispenserItems::add);
        ProtectionListener.protect(BlockDispenseEvent.class, ProtectionType.DISPENSER_PLACE, e -> null, e -> bannedDispenserItems.contains(e.getItem().getType()) ? e.getBlock() : null);
        ProtectionListener.protectDirectional(BlockDispenseEvent.class, ProtectionType.DISPENSER_PLACE_IN, e -> null, e -> e.getBlock(), e -> {
           if (!bannedDispenserItems.contains(e.getItem().getType())) {
               return null;
           }
           Block rel = e.getBlock().getRelative(getFace(e.getBlock()));
           return Collections.singletonList(rel);
        });
        ProtectionListener.protectMultiBlock(BlockMultiPlaceEvent.class, ProtectionType.PLACE_BLOCK, e -> e.getPlayer(), (e, b) -> e.setCancelled(true), e -> e.getReplacedBlockStates().stream().map(BlockState::getBlock).collect(Collectors.toList()));
    }

    private static BlockFace getFace(Block dispenser) {
        if (RedLib.MID_VERSION >= 13) {
            Directional d = (Directional) dispenser.getBlockData();
            return d.getFacing();
        }
        Dispenser disp = (Dispenser) dispenser.getState().getData();
        return disp.getFacing();
    }

    private static Block getBlock(Hanging hanging) {
        return hanging.getLocation().getBlock().getRelative(hanging.getAttachedFace());
    }

    private static Block getBlock(Inventory inv) {
        InventoryHolder holder = inv.getHolder();
        if (holder instanceof Minecart) {
            return ((Minecart) holder).getLocation().getBlock();
        }
        if (holder instanceof DoubleChest) {
            return ((DoubleChest) holder).getLocation().getBlock();
        }
        if (holder instanceof BlockState) {
            return ((BlockState) holder).getBlock();
        }
        if (holder instanceof BlockInventoryHolder) {
            return ((BlockInventoryHolder) holder).getBlock();
        }
        return null;
    }

}
