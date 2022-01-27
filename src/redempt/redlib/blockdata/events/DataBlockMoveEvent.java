package redempt.redlib.blockdata.events;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import redempt.redlib.blockdata.DataBlock;

/**
 * Called when a DataBlock is moved by pistons
 * @author Redempt
 */
public class DataBlockMoveEvent extends Event implements Cancellable {

	private static HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private DataBlock db;
	private Block destination;
	private Event parent;
	private boolean cancelled = false;
	
	/**
	 * Creates a DataBlockMoveEvent
	 * @param db The DataBlock being moved
	 * @param destination The Block it is being moved to
	 * @param parent The event which caused this one
	 */
	public DataBlockMoveEvent(DataBlock db, Block destination, Event parent) {
		this.db = db;
		this.parent = parent;
		this.destination = destination;
	}
	
	/**
	 * @return The event which caused this one
	 */
	public Event getParent() {
		return parent;
	}
	
	/**
	 * @return The Block the data is being moved to
	 */
	public Block getDestination() {
		return destination;
	}
	
	/**
	 * @return The DataBlock being moved
	 */
	public DataBlock getDataBlock() {
		return db;
	}
	
	public Block getBlock() {
		return db.getBlock();
	}
	
	/**
	 * Sets whether to move the data to the new Block
	 * @param cancelled True to cancel the moving of data, false otherwise
	 */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	/**
	 * Cancels the blocks from being moved altogether
	 */
	public void cancelParent() {
		if (parent instanceof Cancellable) {
			((Cancellable) parent).setCancelled(true);
		}
	}
	
	/**
	 * @return Whether the event is cancelled
	 */
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
