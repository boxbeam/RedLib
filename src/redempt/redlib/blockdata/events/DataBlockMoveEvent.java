package redempt.redlib.blockdata.events;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import redempt.redlib.blockdata.DataBlock;

/**
 * Called when a DataBlock is moved
 */
public class DataBlockMoveEvent extends BlockEvent implements Cancellable {
	
	private static HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private Location loc;
	private boolean cancelled = false;
	private DataBlock db;
	
	/**
	 * Construct a DataBlockMoveEvent
	 * @param db The DataBlock
	 * @param loc The location the DataBlock is being moved to
	 */
	public DataBlockMoveEvent(DataBlock db, Location loc) {
		super(db.getBlock());
		this.loc = loc;
		this.db = db;
	}
	
	/**
	 * @return The DataBlock that is moving
	 */
	public DataBlock getDataBlock() {
		return db;
	}
	
	/**
	 * @return The location the DataBlock will be moved to
	 */
	public Location getTo() {
		return loc;
	}
	
	/**
	 * Gets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins
	 *
	 * @return true if this event is cancelled
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
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
