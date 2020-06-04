package redempt.redlib.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.InventoryHolder;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.RedLib;

/**
 * Represents a set of rules in the form of {@link ProtectionType}s protecting a set of blocks, which can have bypasses
 * @author Redempt
 *
 */
public class ProtectionPolicy implements Listener {
	
	private List<BypassPolicy> bypassPolicies = new ArrayList<>();
	private Set<ProtectionType> protections = new HashSet<>();
	private Map<ProtectionType, String> messages = new HashMap<>();
	private Predicate<Block> protectionCheck;
	private Plugin plugin;
	
	/**
	 * Create a ProtectionPolicy to protect blocks
	 * @param protectionCheck A predicate which will be used to check whether blocks are protected by this ProtectionPolicy
	 * @param protections The types of actions to protect against
	 */
	public ProtectionPolicy(Predicate<Block> protectionCheck, ProtectionType... protections) {
		Arrays.stream(protections).forEach(this.protections::add);
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
		this.protectionCheck = protectionCheck;
		Exception e = new Exception();
		for (int i = 0; i < 10 && i < e.getStackTrace().length; i++) {
			try {
				Plugin p = JavaPlugin.getProvidingPlugin(Class.forName(e.getStackTrace()[i].getClassName()));
				if (p != null && !p.getName().equals("RedLib")) {
					plugin = p;
					break;
				}
			} catch (ClassNotFoundException classNotFoundException) {
				classNotFoundException.printStackTrace();
			}
		}
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
		Bukkit.getPluginManager().registerEvents(this, RedLib.getInstance());
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
	
	private boolean canBypass(Player player, ProtectionType type, Block block) {
		return bypassPolicies.stream().anyMatch(p -> p.canBypass(player, type, block));
	}
	
	private void sendMessage(Player player, ProtectionType type) {
		String message = messages.get(type);
		if (message != null) {
			player.sendMessage(message);
		}
	}
	
	@EventHandler
	public void onDisablePlugin(PluginDisableEvent e) {
		if (e.getPlugin().equals(plugin)) {
			disable();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBreakBlock(BlockBreakEvent e) {
		if (protections.contains(ProtectionType.BREAK_BLOCK) && protectionCheck.test(e.getBlock())) {
			if (canBypass(e.getPlayer(), ProtectionType.BREAK_BLOCK, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
			sendMessage(e.getPlayer(), ProtectionType.BREAK_BLOCK);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent e) {
		if (protections.contains(ProtectionType.PLACE_BLOCK) && protectionCheck.test(e.getBlock())) {
			if (canBypass(e.getPlayer(), ProtectionType.PLACE_BLOCK, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
			sendMessage(e.getPlayer(), ProtectionType.PLACE_BLOCK);
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
		if (type == ProtectionType.INTERACT && !e.getClickedBlock().getType().isInteractable()) {
			return;
		}
		if (protections.contains(type)) {
			e.getClickedBlock();
			if (canBypass(e.getPlayer(), type, e.getClickedBlock())) {
				return;
			}
			e.setCancelled(true);
			sendMessage(e.getPlayer(), type);
		}
	}
	
	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent e) {
		Block block = e.getBlockClicked().getRelative(e.getBlockFace());
		if (protections.contains(ProtectionType.USE_BUCKETS)
				&& protectionCheck.test(block)
				&& !canBypass(e.getPlayer(), ProtectionType.USE_BUCKETS, block)) {
			e.setCancelled(true);
			sendMessage(e.getPlayer(), ProtectionType.USE_BUCKETS);
		}
	}
	
	@EventHandler
	public void onBucketFill(PlayerBucketFillEvent e) {
		Block block = e.getBlockClicked().getRelative(e.getBlockFace());
		if (protections.contains(ProtectionType.USE_BUCKETS)
				&& protectionCheck.test(block)
				&& !canBypass(e.getPlayer(), ProtectionType.USE_BUCKETS, block)) {
			e.setCancelled(true);
			sendMessage(e.getPlayer(), ProtectionType.USE_BUCKETS);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent e) {
		if (protections.contains(ProtectionType.ENTITY_EXPLOSION)) {
			e.blockList().removeIf(b -> protectionCheck.test(b) && !canBypass(null, ProtectionType.ENTITY_EXPLOSION, b));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockExplode(BlockExplodeEvent e) {
		if (protections.contains(ProtectionType.BLOCK_EXPLOSION)) {
			e.blockList().removeIf(b -> protectionCheck.test(b) && !canBypass(null, ProtectionType.BLOCK_EXPLOSION, b));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPistonPush(BlockPistonExtendEvent e) {
		if (!protections.contains(ProtectionType.PISTONS)) {
			return;
		}
		if (protectionCheck.test(e.getBlock()) || e.getBlocks().stream().anyMatch(protectionCheck::test)) {
			if (canBypass(null, ProtectionType.PISTONS, e.getBlock()) && e.getBlocks().stream().allMatch(b -> canBypass(null, ProtectionType.PISTONS, b))) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPistonPull(BlockPistonRetractEvent e) {
		if (!protections.contains(ProtectionType.PISTONS)) {
			return;
		}
		if (protectionCheck.test(e.getBlock()) || e.getBlocks().stream().anyMatch(protectionCheck::test)) {
			if (canBypass(null, ProtectionType.PISTONS, e.getBlock()) && e.getBlocks().stream().allMatch(b -> canBypass(null, ProtectionType.PISTONS, b))) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRedstone(BlockRedstoneEvent e) {
		if (protections.contains(ProtectionType.REDSTONE) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.REDSTONE, e.getBlock())) {
				return;
			}
			e.setNewCurrent(e.getOldCurrent());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		if (e.getEntityType() == EntityType.FALLING_BLOCK && protections.contains(ProtectionType.FALLING_BLOCK) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.FALLING_BLOCK, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		} else if (e.getEntityType() == EntityType.SILVERFISH && protectionCheck.test(e.getBlock()) && protections.contains(ProtectionType.SILVERFISH)
				&& !canBypass(null, ProtectionType.SILVERFISH, e.getBlock())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onGrow(BlockGrowEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.GROWTH, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpread(BlockSpreadEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.GROWTH, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onForm(BlockFormEvent e) {
		if (protections.contains(ProtectionType.GROWTH) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.GROWTH, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockFade(BlockFadeEvent e) {
		if (protections.contains(ProtectionType.FADE) && protectionCheck.test(e.getBlock())) {
			if (canBypass(null, ProtectionType.FADE, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFlow(BlockFromToEvent e) {
		if (protections.contains(ProtectionType.FLOW) && protectionCheck.test(e.getToBlock())) {
			if (canBypass(null, ProtectionType.FLOW, e.getBlock())) {
				return;
			}
			e.setCancelled(true);
		}
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
		SILVERFISH;
		
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
		public static final ProtectionType[] INDIRECT_PLAYERS = {PISTONS, REDSTONE, ENTITY_EXPLOSION, BLOCK_EXPLOSION, FALLING_BLOCK};
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