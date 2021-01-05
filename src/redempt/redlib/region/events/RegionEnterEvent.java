package redempt.redlib.region.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import redempt.redlib.region.Region;

/**
 * Called when a player enters a region with events enabled
 * @author Redempt
 *
 */
public class RegionEnterEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	private Region region;
	private Player player;
	private EnterCause cause;
	private Cancellable parent;
	
	/**
	 * Constructs a new RegionEnterEvent
	 * @param player The player who entered the region
	 * @param region The region that was entered
	 * @param cause What caused the player to enter the region
	 * @param parent The event which caused this RegionEnterEvent to fire
	 */
	public RegionEnterEvent(Player player, Region region, EnterCause cause, Cancellable parent) {
		this.region = region;
		this.player = player;
		this.cause = cause;
		this.parent = parent;
	}
	
	/**
	 * @return The player who entered the region
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * @return The region that was entered
	 */
	public Region getRegion() {
		return region;
	}
	
	/**
	 * @return What caused the player to enter the region
	 */
	public EnterCause getCause() {
		return cause;
	}
	
	/**
	 * @return Whether or not the event has been cancelled. Always false if the parent event cannot be cancelled.
	 */
	@Override
	public boolean isCancelled() {
		if (parent == null) {
			return false;
		}
		return parent.isCancelled();
	}
	
	/**
	 * Set whether or not to cancel the player entering the Region.
	 * Not all causes can be cancelled - check {@link RegionEnterEvent#getCause()} first,
	 * you can't cancel a player joining
	 * @param cancel Whether to cancel this event
	 */
	@Override
	public void setCancelled(boolean cancel) {
		if (parent != null) {
			parent.setCancelled(cancel);
		}
	}
	
	public static enum EnterCause {
		/**
		 * When a player moves into a region
		 */
		MOVE,
		/**
		 * When a player teleports into a region
		 */
		TELEPORT,
		/**
		 * When a player joins into a region
		 */
		JOIN
	}

}
