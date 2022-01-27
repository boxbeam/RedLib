package redempt.redlib.blockdata;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent;
import redempt.redlib.blockdata.events.DataBlockDestroyEvent.DestroyCause;
import redempt.redlib.blockdata.events.DataBlockMoveEvent;
import redempt.redlib.json.JSONMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BlockDataListener implements Listener {

	private BlockDataManager manager;
	
	public BlockDataListener(BlockDataManager manager, Plugin plugin) {
		this.manager = manager;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	private void fireDestroy(DataBlock db, Event parent, DestroyCause cause) {
		if (db == null) {
			return;
		}
		DataBlockDestroyEvent ev = new DataBlockDestroyEvent(db, parent, cause);
		Bukkit.getPluginManager().callEvent(ev);
		if (!ev.isCancelled()) {
			manager.remove(db);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e) {
		DataBlock db = manager.getDataBlock(e.getBlock(), false);
		fireDestroy(db, e, DestroyCause.PLAYER_BREAK);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExplode(BlockExplodeEvent e) {
		handleExplosion(e.blockList(), e);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent e) {
		handleExplosion(e.blockList(), e);
	}
	
	private void handleExplosion(List<Block> blocks, Cancellable e) {
		List<DataBlock> toRemove = new ArrayList<>();
		blocks.forEach(b -> {
			DataBlock db = manager.getDataBlock(b);
			if (db == null) {
				return;
			}
			DataBlockDestroyEvent ev = new DataBlockDestroyEvent(db, (Event) e, DestroyCause.EXPLOSION);
			Bukkit.getPluginManager().callEvent(ev);
			if (!ev.isCancelled()) {
				toRemove.add(db);
			}
		});
		if (e.isCancelled()) {
			return;
		}
		toRemove.forEach(manager::remove);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCombust(BlockBurnEvent e) {
		DataBlock db = manager.getDataBlock(e.getBlock());
		fireDestroy(db, e, DestroyCause.COMBUST);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent e) {
		handlePiston(e.getBlocks(), e);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent e) {
		handlePiston(e.getBlocks(), e);
	}
	
	private void handlePiston(List<Block> blocks, BlockPistonEvent e) {
		List<DataBlock> toMove = new ArrayList<>();
		blocks.forEach(b -> {
			DataBlock db = manager.getDataBlock(b);
			if (db == null) {
				return;
			}
			Block destination = db.getBlock().getRelative(e.getDirection());
			DataBlockMoveEvent ev = new DataBlockMoveEvent(db, destination, e);
			Bukkit.getPluginManager().callEvent(ev);
			if (!ev.isCancelled()) {
				toMove.add(db);
			}
		});
		if (e.isCancelled()) {
			return;
		}
		Map<Block, JSONMap> moved = new HashMap<>();
		toMove.forEach(db -> {
			Block destination = db.getBlock().getRelative(e.getDirection());
			moved.put(destination, db.data);
		});
		toMove.forEach(manager::remove);
		moved.forEach((block, data) -> {
			manager.getDataBlock(block).data = data;
		});
	}
	
}
