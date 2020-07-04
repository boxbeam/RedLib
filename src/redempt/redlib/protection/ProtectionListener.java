package redempt.redlib.protection;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import redempt.redlib.RedLib;
import redempt.redlib.misc.EventListener;
import redempt.redlib.protection.ProtectionPolicy.ProtectionType;

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
	
	protected static <T extends Event & Cancellable> void protect(Class<T> clazz, ProtectionType type, Function<T, Player> getPlayer, Function<T, Block>... getBlocks) {
		new EventListener<>(RedLib.getInstance(), clazz, EventPriority.HIGHEST, e -> {
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
				} catch (Exception ex) {
					continue;
				}
				if (blocks == null) {
					continue;
				}
				for (Block block : blocks) {
					if (!testAll(block, type, player)) {
						cancel.accept(e, block);
					}
				}
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
