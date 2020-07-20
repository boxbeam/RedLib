package redempt.redlib.blockdata.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import redempt.redlib.blockdata.DataBlock;

/**
 * Called when a DataBlock is broken by a Player
 * @author Redempt
 */
public class DataBlockBreakEvent extends BlockEvent implements Cancellable {
	
	private static HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private boolean cancelled = false;
	private BlockBreakEvent parent;
	private Player player;
	private DataBlock db;
	
	/**
	 * Construct a DataBlockBreakEvent
	 * @param parent The parent BlockBreakEvent that caused this event
	 * @param db The DataBlock that was broken
	 */
	public DataBlockBreakEvent(BlockBreakEvent parent, DataBlock db) {
		super(parent.getBlock());
		player = parent.getPlayer();
		this.db = db;
		this.parent = parent;
	}
	
	/**
	 * @return The parent BlockBreeakEvent which caused this event
	 */
	public BlockBreakEvent getParent() {
		return parent;
	}
	
	/**
	 * @return The DataBlock that was broken
	 */
	public DataBlock getDataBlock() {
		return db;
	}
	
	/**
	 * @return The Player that broke the DataBlock
	 */
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	/**
	 * @return Whether this event has been cancelled
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Sets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins.
	 *
	 * @param cancel true if you wish to cancel this event
	 */
	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
	
}
