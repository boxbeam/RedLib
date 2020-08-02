package redempt.redlib.blockdata.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import redempt.redlib.blockdata.DataBlock;

/**
 * Called when a DataBlock is destroyed by something other than a player
 * @author Redempt
 */
public class DataBlockDestroyEvent extends BlockEvent implements Cancellable {
	
	private static HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private DataBlock db;
	private DestroyCause cause;
	private Player player;
	private Event parent;
	private boolean cancelled = false;
	
	/**
	 * Construct a DataBlockDestroyEvent
	 * @param db The DataBlock that was destroyed
	 * @param player The player that broke the block, or null
	 * @param cause Why it was destroyed
	 * @param parent The event that caused this event
	 */
	public DataBlockDestroyEvent(DataBlock db, Player player, DestroyCause cause, Event parent) {
		super(db.getBlock());
		this.db = db;
		this.player = player;
		this.cause = cause;
		this.parent = parent;
	}
	
	/**
	 * @return The event that caused this one
	 */
	public Event getParent() {
		return parent;
	}
	
	/**
	 * @return The Player who broke the DataBlock, or null if it was not a Player.
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * @return The DataBlock that was destroyed
	 */
	public DataBlock getDataBlock() {
		return db;
	}
	
	/**
	 * @return The DestroyCause representing why the DataBlock was destroyed
	 */
	public DestroyCause getCause() {
		return cause;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	/**
	 * @return Whether this event is cancelled
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
	
	public enum DestroyCause {
		
		EXPLOSION,
		FIRE,
		PLAYER;
		
	}
	
}
