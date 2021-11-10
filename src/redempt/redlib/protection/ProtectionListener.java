package redempt.redlib.protection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;
import redempt.redlib.protection.ProtectionPolicy.ProtectionType;
import redempt.redlib.region.CuboidRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class ProtectionListener {
	
	private static boolean testAll(Block block, ProtectionType type, Player player) {
		for (ProtectionPolicy policy : ProtectionPolicy.globalPolicies) {
			if (!policy.allow(block, type, player)) {
				return false;
			}
		}
		Set<ProtectionPolicy> applicable = ProtectionPolicy.regionMap.get(block.getLocation());
		for (ProtectionPolicy policy : applicable) {
			if (!policy.allow(block, type, player)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean testAll(Block outside, List<Block> inside, ProtectionType type, Player player) {
		Location min = outside.getLocation();
		Location max = outside.getLocation();
		for (Block block : inside) {
			max.setX(Math.max(max.getX(), block.getX()));
			max.setY(Math.max(max.getY(), block.getY()));
			max.setZ(Math.max(max.getZ(), block.getZ()));
			
			min.setX(Math.min(min.getX(), block.getX()));
			min.setY(Math.min(min.getY(), block.getY()));
			min.setZ(Math.min(min.getZ(), block.getZ()));
		}
		CuboidRegion region = new CuboidRegion(min, max);
		int radius = (int) Arrays.stream(region.getDimensions()).max().getAsDouble();
		Set<ProtectionPolicy> applicable = ProtectionPolicy.regionMap.getNearby(region.getCenter(), radius);
		for (ProtectionPolicy policy : applicable) {
			if (policy.allow(outside, type, player) && inside.stream().anyMatch(b -> !policy.allow(b, type, player))) {
				return false;
			}
		}
		return true;
	}
	
	protected static <T extends Event & Cancellable> void protect(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Function<T, Block>... getBlocks) {
		new EventListener<>(RedLib.getInstance(), clazz, EventPriority.HIGHEST, e -> {
			if (e.isCancelled()) {
				return;
			}
			Player player = null;
			if (getPlayer != null) {
				player = getPlayer.apply(e);
			}
			for (Function<T, Block> func : getBlocks) {
				Block block = null;
				try {
					block = func.apply(e);
				} catch (Exception ex) {
					continue;
				}
				if (block == null) {
					continue;
				}
				if (!testAll(block, type, player)) {
					e.setCancelled(true);
				}
			}
		});
	}
	
	protected static <T extends Event> void protectMultiBlock(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, BiConsumer<T, Block> cancel, Function<T, List<Block>>... getBlocks) {
		new EventListener<>(RedLib.getInstance(), clazz, EventPriority.HIGHEST, e -> {
			Player player = null;
			if (getPlayer != null) {
				player = getPlayer.apply(e);
			}
			for (Function<T, List<Block>> func : getBlocks) {
				List<Block> blocks = null;
				try {
					blocks = func.apply(e);
				} catch (NullPointerException ex) {
					continue;
				}
				if (blocks == null) {
					continue;
				}
				blocks = new ArrayList<>(blocks);
				for (Block block : blocks) {
					if (!testAll(block, type, player)) {
						cancel.accept(e, block);
					}
				}
			}
		});
	}
	
	protected static <T extends Event & Cancellable> void protectDirectional(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Function<T, Block> getBaseBlock, Function<T, List<Block>> getProtectedBlocks) {
		new EventListener<>(RedLib.getInstance(), clazz, EventPriority.HIGHEST, e -> {
			Player player = null;
			if (getPlayer != null) {
				player = getPlayer.apply(e);
			}
			if (!testAll(getBaseBlock.apply(e), getProtectedBlocks.apply(e), type, player)) {
				e.setCancelled(true);
			}
		});
	}
	
	protected static <T extends Event> void protectNonCancellable(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Consumer<T> cancel, Function<T, Block>... getBlocks) {
		new EventListener<>(RedLib.getInstance(), clazz, EventPriority.HIGHEST, e -> {
			Player player = null;
			if (getPlayer != null) {
				player = getPlayer.apply(e);
			}
			for (Function<T, Block> func : getBlocks) {
				Block block = func.apply(e);
				if (!testAll(block, type, player)) {
					cancel.accept(e);
				}
			}
		});
	}

}
