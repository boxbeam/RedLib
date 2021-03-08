package redempt.redlib.protection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Wither;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.InventoryHolder;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;
import redempt.redlib.region.CuboidRegion;
import redempt.redlib.region.RegionMap;

/**
 * Represents a set of rules in the form of {@link ProtectionType}s protecting a set of blocks, which can have bypasses
 * @author Redempt
 *
 */
public class ProtectionPolicy implements Listener {
	
	protected static Set<ProtectionPolicy> globalPolicies = new HashSet<>();
	protected static RegionMap<ProtectionPolicy> regionMap = new RegionMap<>();
	private static boolean registered = false;
	
	public static void registerProtections() {
		if (registered) {
			return;
		}
		registered = true;
		ProtectionListener.protect(BlockBreakEvent.class, ProtectionType.BREAK_BLOCK, e -> e.getPlayer(), e -> e.getBlock());
		ProtectionListener.protect(BlockPlaceEvent.class, ProtectionType.PLACE_BLOCK, e -> e.getPlayer(), e -> e.getBlock());
		ProtectionListener.protect(PlayerInteractEvent.class, ProtectionType.INTERACT, e -> e.getPlayer(), e -> {
			if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || (RedLib.MID_VERSION >= 13 && !e.getClickedBlock().getType().isInteractable())) {
				return null;
			}
			return e.getClickedBlock();
		});
		ProtectionListener.protect(InventoryOpenEvent.class, ProtectionType.CONTAINER_ACCESS, e -> (Player) e.getPlayer(), e -> {
			InventoryHolder holder = e.getInventory().getHolder();
			if (holder instanceof BlockState) {
				return ((BlockState) holder).getBlock();
			}
			return null;
		});
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
		ProtectionListener.protect(BlockSpreadEvent.class, ProtectionType.GROWTH, e -> null, e -> e.getBlock());
		ProtectionListener.protect(BlockFormEvent.class, ProtectionType.GROWTH, e -> null, e -> e.getBlock());
		ProtectionListener.protect(BlockFadeEvent.class, ProtectionType.FADE, e -> null, e -> e.getBlock());
		ProtectionListener.protect(BlockFromToEvent.class, ProtectionType.FLOW, e -> null, e -> e.getBlock(), e -> e.getToBlock());
		ProtectionListener.protect(BlockBurnEvent.class, ProtectionType.FIRE, e -> null, e -> e.getBlock());
		ProtectionListener.protect(CreatureSpawnEvent.class, ProtectionType.MOB_SPAWN, e -> null, e -> {
			if (e.getSpawnReason() == SpawnReason.CUSTOM) {
				return null;
			}
			return e.getEntity().getLocation().getBlock();
		});
		ProtectionListener.protectMultiBlock(PortalCreateEvent.class, ProtectionType.PORTAL_PAIRING, e -> null, (e, b) -> e.setCancelled(true), e -> {
			if (e.getReason() != CreateReason.NETHER_PAIR) {
				return null;
			}
			return e.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toList());
		});
	}
	
	/**
	 * Registers a custom event to be protected using a specific ProtectionType
	 * @param clazz The event class of an event which implements {@link Cancellable}
	 * @param type The ProtectionType to protect against this event
	 * @param getPlayer A function to get the player associated with the event - can return null
	 * @param getBlocks A vararg of functions to get blocks associated with the event
	 * @param <T> The event type
	 */
	public static <T extends Event & Cancellable> void registerProtection(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Function<T, Block>... getBlocks) {
		ProtectionListener.protect(clazz, type, getPlayer, getBlocks);
	}
	
	/**
	 * Registers a custom event that cannot be cancelled using {@link Cancellable#setCancelled(boolean)} using a specific ProtectionType
	 * @param clazz The event class
	 * @param type The ProtectionType to protect against this event
	 * @param getPlayer A function to get the player associated with the event - can return null
	 * @param cancel A consumer to cancel the event
	 * @param getBlocks A vararg of functions to get the blocks associated with this event
	 * @param <T> The event type
	 */
	public static <T extends Event> void registerProtectionNonCancellable(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Consumer<T> cancel, Function<T, Block>... getBlocks) {
		ProtectionListener.protectNonCancellable(clazz, type, getPlayer, cancel, getBlocks);
	}
	
	private List<BypassPolicy> bypassPolicies = new ArrayList<>();
	private Set<ProtectionType> protections = EnumSet.noneOf(ProtectionType.class);
	private Map<ProtectionType, String> messages = new HashMap<>();
	private Predicate<Block> protectionCheck;
	private CuboidRegion bounds;
	private Plugin plugin;
	
	{
		new EventListener<>(RedLib.getInstance(), PluginDisableEvent.class, (l, e) -> {
			if (e.getPlugin().equals(plugin)) {
				disable();
				l.unregister();
			}
		});
	}
	
	protected ProtectionPolicy(Plugin plugin, CuboidRegion bounds, Predicate<Block> protectionCheck, ProtectionType... protections) {
		this.plugin = plugin;
		this.bounds = bounds;
		Arrays.stream(protections).forEach(this.protections::add);
		this.protectionCheck = protectionCheck;
		regionMap.set(bounds, this);
	}
	
	/**
	 * Create a ProtectionPolicy to protect blocks
	 * @param bounds A region that defines the bounds inside which this ProtectionPolicy protects blocks
	 * @param protectionCheck A predicate which will be used to check whether blocks are protected by this ProtectionPolicy
	 * @param protections The types of actions to protect against
	 */
	public ProtectionPolicy(CuboidRegion bounds, Predicate<Block> protectionCheck, ProtectionType... protections) {
		plugin = RedLib.getCallingPlugin();
		this.bounds = bounds;
		Arrays.stream(protections).forEach(this.protections::add);
		this.protectionCheck = protectionCheck;
		regionMap.set(bounds, this);
	}
	
	/**
	 * Create a ProtectionPolicy to protect blocks. Prefer {@link ProtectionPolicy#ProtectionPolicy(CuboidRegion, Predicate, ProtectionType...)},
	 * as it will improve performance
	 * @param protectionCheck A predicate which will be used to check whether blocks are protected by this ProtectionPolicy
	 * @param protections The types of actions to protect against
	 */
	public ProtectionPolicy(Predicate<Block> protectionCheck, ProtectionType... protections) {
		plugin = RedLib.getCallingPlugin();
		this.protectionCheck = protectionCheck;
		Arrays.stream(protections).forEach(this.protections::add);
		globalPolicies.add(this);
	}
	
	/**
	 * Sets the ProtectionTypes to be used
	 * @param protections The ProtectionTypes
	 */
	public void setProtectionTypes(ProtectionType... protections) {
		this.protections.clear();
		addProtectionTypes(protections);
	}
	
	/**
	 * Adds ProtectionTypes to this ProtectionPolicy
	 * @param protections The ProtectionTypes to add
	 */
	public void addProtectionTypes(ProtectionType... protections) {
		Collections.addAll(this.protections, protections);
	}
	
	/**
	 * Removes ProtectionTypes from this ProtectionPolicy
	 * @param protections The ProtectionTypes to remove
	 */
	public void removeProtectionTypes(ProtectionType... protections) {
		Arrays.stream(protections).forEach(this.protections::remove);
	}
	
	/**
	 * Disables all protections for this ProtectionPolicy
	 */
	public void disable() {
		if (bounds == null) {
			globalPolicies.remove(this);
			return;
		}
		regionMap.remove(bounds, this);
	}
	
	/**
	 * Enables all protections specified for this ProtectionPolicy
	 */
	public void enable() {
		if (bounds == null) {
			globalPolicies.add(this);
			return;
		}
		regionMap.set(bounds, this);
	}
	
	/**
	 * Adds a bypass policy, which allows certain players to bypass certain protection types
	 * @param bypassPolicy The {@link BiPredicate} to determine bypasses by player and protection type
	 */
	public void addBypassPolicy(BiPredicate<Player, ProtectionType> bypassPolicy) {
		bypassPolicies.add((p, t, b) -> bypassPolicy.test(p, t));
	}
	
	/**
	 * Adds a bypass policy, which allows certain players to bypass certain protection types
	 * @param bypassPolicy The {@link BypassPolicy} to determine bypasses by player and protection type
	 */
	public void addBypassPolicy(BypassPolicy bypassPolicy) {
		bypassPolicies.add(bypassPolicy);
	}
	
	/**
	 * Removes all bypass policies
	 */
	public void clearBypassPolicies() {
		bypassPolicies.clear();
	}
	
	/**
	 * Sets the message to be shown to a player when they attempt to do an action which is protected again
	 * @param type The type of action the message corresponds to
	 * @param message The message players should be shown when this type of action is denied
	 */
	public void setDenyMessage(ProtectionType type, String message) {
		messages.put(type, message);
	}
	
	/**
	 * Sets the message to be shown to a player when they attempt to do an action which is protected again
	 * @param filter A filter for which types to set the message for
	 * @param message The message players should be shown when these types of actions are denied
	 */
	public void setDenyMessage(Predicate<ProtectionType> filter, String message) {
		Arrays.stream(ProtectionType.values()).filter(filter).forEach(t -> messages.put(t, message));
	}
	
	/**
	 * Clear all deny messages
	 */
	public void clearDenyMessages() {
		messages.clear();
	}
	
	/**
	 * @return The cuboid bounds of this ProtectionPolicy, or null if it is a global policy
	 */
	public CuboidRegion getBounds() {
		return bounds;
	}
	
	private boolean canBypass(Player player, ProtectionType type, Block block) {
		return bypassPolicies.stream().anyMatch(p -> p.canBypass(player, type, block));
	}
	
	private void sendMessage(Player player, ProtectionType type) {
		String message = messages.get(type);
		if (message != null) {
			player.sendMessage(message);
		}
	}
	
	public boolean allow(Block block, ProtectionType type, Player player) {
		if (protections.contains(type) && protectionCheck.test(block)) {
			if (canBypass(player, type, block)) {
				return true;
			}
			if (player != null) {
				sendMessage(player, type);
			}
			return false;
		}
		return true;
	}
	
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if (e.getSpawnReason() == SpawnReason.CUSTOM) {
			return;
		}
		if (protections.contains(ProtectionType.MOB_SPAWN) && protectionCheck.test(e.getLocation().getBlock())) {
			if (canBypass(null, ProtectionType.MOB_SPAWN, e.getLocation().getBlock())) {
				return;
			}
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
		 * Players using buckets to place or collect liquids
		 */
		USE_BUCKETS,
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
		ANVIL_BREAK,
		/**
		 * Mobs spawning
		 */
		MOB_SPAWN,
		/**
		 * Silverfish infesting or breaking blocks
		 */
		SILVERFISH,
		/**
		 * Wither spawning in and breaking blocks around it
		 */
		WITHER,
		/**
		 * Fire destroying blocks
		 */
		FIRE,
		/**
		 * Portals being created from another dimension
		 */
		PORTAL_PAIRING,
		/**
		 * Does nothing by default, but other plugins can register their events to be protected against by this type.
		 */
		MISCELLANEOUS;
		
		/**
		 * Every protection type
		 */
		public static final ProtectionType[] ALL = values();
		/**
		 * All protection types relating to actions taken directly by players - Breaking, placing, and interacting with blocks
		 */
		public static final ProtectionType[] DIRECT_PLAYERS = {BREAK_BLOCK, PLACE_BLOCK, INTERACT, CONTAINER_ACCESS, USE_BUCKETS};
		/**
		 * All protection types relating to actions usually taken by players which indirectly affect blocks - Pistons, redstone, explosions, and falling blocks
		 */
		public static final ProtectionType[] INDIRECT_PLAYERS = {PISTONS, REDSTONE, ENTITY_EXPLOSION, BLOCK_EXPLOSION, FALLING_BLOCK, FIRE, PORTAL_PAIRING, WITHER};
		/**
		 * All protection types relating to natural processes not caused by players
		 */
		public static final ProtectionType[] NATURAL = {GROWTH, FADE, FLOW, MOB_SPAWN};
		
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