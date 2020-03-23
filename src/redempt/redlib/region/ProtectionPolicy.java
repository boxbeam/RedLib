package redempt.redlib.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import redempt.redlib.RedLib;

/**
 * Represents a set of rules in the form of {@link ProtectionType}s protecting a set of blocks, which can have bypasses
 * @author Redempt
 *
 */
public class ProtectionPolicy implements Listener {
	
	private List<BiPredicate<Player, ProtectionType>> bypassPolicies = new ArrayList<>();
	private Set<ProtectionType> protections = new HashSet<>();
	private Predicate<Block> protectionCheck;
	
	protected ProtectionPolicy(Predicate<Block> protectionCheck, ProtectionType... protections) {
		Arrays.stream(protections).forEach(this.protections::add);
		Bukkit.getPluginManager().registerEvents(this, RedLib.plugin);
		this.protectionCheck = protectionCheck;
	}
	
	/**
	 * Sets the ProtectionTypes to be used
	 * @param protections The ProtectionTypes
	 */
	public void setProtectionTypes(ProtectionType... protections) {
		this.protections.clear();
		Arrays.stream(protections).forEach(this.protections::add);
	}
	
	/**
	 * Disables all protections for this ProtectionPolicy
	 */
	public void disable() {
		HandlerList.unregisterAll(this);
	}
	
	/**
	 * Enables all protections specified for this ProtectionPolicy
	 */
	public void enable() {
		HandlerList.unregisterAll(this);
		Bukkit.getPluginManager().registerEvents(this, RedLib.plugin);
	}
	
	/**
	 * Adds a bypass policy, which allows certain players to bypass certain protection types
	 * @param bypassPolicy The {@link BiPredicate} to determine bypasses by player and protection type
	 */
	public void addBypassPolicy(BiPredicate<Player, ProtectionType> bypassPolicy) {
		bypassPolicies.add(bypassPolicy);
	}
	
	/**
	 * Removes all bypass policies
	 */
	public void clearBypassPolicies() {
		bypassPolicies.clear();
	}
	
	private boolean canBypass(Player player, ProtectionType type) {
		return bypassPolicies.stream().anyMatch(p -> p.test(player, type));
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBreakBlock(BlockBreakEvent e) {
		if (protections.contains(ProtectionType.BREAK_BLOCK) && protectionCheck.test(e.getBlock())) {
			if (canBypass(e.getPlayer(), ProtectionType.BREAK_BLOCK)) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (protections.contains(ProtectionType.PLACE_BLOCK) && protectionCheck.test(e.getBlock())) {
			if (canBypass(e.getPlayer(), ProtectionType.PLACE_BLOCK)) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !protectionCheck.test(e.getClickedBlock())) {
			return;
		}
		if (e.getClickedBlock().getType().toString().endsWith("ANVIL") && protections.contains(ProtectionType.ANVIL_BREAK)) {
			if (RedLib.midVersion >= 13) {
				BlockData data = e.getClickedBlock().getBlockData();
				String s = data.getAsString();
				s = s.substring(s.indexOf("anvil"));
				data = Bukkit.createBlockData(s);
				e.getClickedBlock().setBlockData(data);
			}
		}
		ProtectionType type = e.getClickedBlock().getState() instanceof InventoryHolder ? ProtectionType.CONTAINER_ACCESS : ProtectionType.INTERACT;
		if (protections.contains(type)) {
			e.getClickedBlock();
			if (canBypass(e.getPlayer(), type)) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent e) {
		if (protections.contains(ProtectionType.ENTITY_EXPLOSION)) {
			e.blockList().removeIf(protectionCheck::test);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockExplode(BlockExplodeEvent e) {
		if (protections.contains(ProtectionType.BLOCK_EXPLOSION)) {
			e.blockList().removeIf(protectionCheck::test);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPistonPush(BlockPistonExtendEvent e) {
		if (!protections.contains(ProtectionType.PISTONS)) {
			return;
		}
		if (protectionCheck.test(e.getBlock()) || e.getBlocks().stream().anyMatch(protectionCheck::test)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPistonPull(BlockPistonRetractEvent e) {
		if (!protections.contains(ProtectionType.PISTONS)) {
			return;
		}
		if (protectionCheck.test(e.getBlock()) || e.getBlocks().stream().anyMatch(protectionCheck::test)) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRedstone(BlockRedstoneEvent e) {
		if (protections.contains(ProtectionType.REDSTONE) && protectionCheck.test(e.getBlock())) {
			e.setNewCurrent(e.getOldCurrent());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		if (e.getEntityType() == EntityType.FALLING_BLOCK && protections.contains(ProtectionType.FALLING_BLOCK) && protectionCheck.test(e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onGrow(BlockGrowEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpread(BlockSpreadEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onForm(BlockFormEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockFade(BlockFadeEvent e) {
		if (protections.contains(ProtectionType.FADE) && protectionCheck.test(e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFlow(BlockFromToEvent e) {
		if (protections.contains(ProtectionType.FLOW) && protectionCheck.test(e.getToBlock())) {
			e.setCancelled(true);
		}
	}
	
	public static enum ProtectionType {
		
		/**
		 * Players breaking blocks
		 */
		BREAK_BLOCK,
		/**
		 * Players placing blocks
		 */
		PLACE_BLOCK,
		/**
		 * Players interacting with non-container blocks
		 */
		INTERACT,
		/**
		 * Players opening containers
		 */
		CONTAINER_ACCESS,
		/**
		 * Entities exploding
		 */
		ENTITY_EXPLOSION,
		/**
		 * Blocks exploding
		 */
		BLOCK_EXPLOSION,
		/**
		 * Pistons pushing and pulling blocks
		 */
		PISTONS,
		/**
		 * Redstone wires and components
		 */
		REDSTONE,
		/**
		 * FallingBlock entities turning into solid blocks (sand, gravel)
		 */
		FALLING_BLOCK,
		/**
		 * Crop growth and block spreading/formation
		 */
		GROWTH,
		/**
		 * Blocks fading
		 */
		FADE,
		/**
		 * Lava and water flowing
		 */
		FLOW,
		/**
		 * Players using an anvil damaging it (1.13+ only)
		 */
		ANVIL_BREAK;
		
		/**
		 * Every protection type
		 */
		public static final ProtectionType[] ALL = values();
		/**
		 * All protection types relating to actions taken directly by players - Breaking, placing, and interacting with blocks
		 */
		public static final ProtectionType[] DIRECT_PLAYERS = {BREAK_BLOCK, PLACE_BLOCK, INTERACT, CONTAINER_ACCESS};
		/**
		 * All protection types relating to actions usually taken by players which indirectly affect blocks - Pistons, redstone, explosions, and falling blocks
		 */
		public static final ProtectionType[] INDIRECT_PLAYERS = {PISTONS, REDSTONE, ENTITY_EXPLOSION, BLOCK_EXPLOSION, FALLING_BLOCK};
		/**
		 * All protection types relating to natural processes not caused by players
		 */
		public static final ProtectionType[] NATURAL = {GROWTH, FADE, FLOW};
		
		/**
		 * Gets all protection types except those specified
		 * @param types The protection types to exclude
		 * @return All protection types except those specified
		 */
		public static ProtectionType[] allExcept(ProtectionType... types) {
			Set<ProtectionType> list = new HashSet<>();
			Arrays.stream(ALL).forEach(list::add);
			Arrays.stream(types).forEach(list::remove);
			return list.toArray(new ProtectionType[list.size()]);
		}
		
		/**
		 * Combines arrays of protection types
		 * @param types The arrays to combine
		 * @return The combined arrays
		 */
		public static ProtectionType[] and(ProtectionType[]... types) {
			Set<ProtectionType> list = new HashSet<>();
			for (ProtectionType[] arr : types) {
				for (ProtectionType type : arr) {
					list.add(type);
				}
			}
			return list.toArray(new ProtectionType[list.size()]);
		}
		
	}
	
}