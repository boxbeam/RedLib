package redempt.redlib.region.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import redempt.redlib.region.CuboidRegion;
import redempt.redlib.region.Region;

/**
 * Called when a player exits a region with events enabled
 * @author Redempt
 *
 */
public class RegionExitEvent extends Event implements Cancellable {
	
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
	private ExitCause cause;
	private Cancellable parent;
	
	/**
	 * Constructs a new RegionExitEvent
	 * @param player The player that exited the region
	 * @param region The region that was exited
	 * @param cause What caused the player to enter the region
	 * @param parent The event that caused this RegionExitEvent to fire
	 */
	public RegionExitEvent(Player player, Region region, ExitCause cause, Cancellable parent) {
		this.region = region;
		this.player = player;
		this.cause = cause;
		this.parent = parent;
	}
	
	/**
	 * @return The player who exited the region
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * @return The region that was exited
	 */
	public Region getRegion() {
		return region;
	}
	
	/**
	 * @return What caused the player to exit the region
	 */
	public ExitCause getCause() {
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
	 * Not all causes can be cancelled - check {@link RegionExitEvent#getCause()} first,
	 * you can't cancel a player leaving
	 * @param cancel Whether to cancel this event
	 */
	@Override
	public void setCancelled(boolean cancel) {
		if (parent != null) {
			parent.setCancelled(cancel);
		}
	}
	
	public static enum ExitCause {
		/**
		 * When a player moves out of a region
		 */
		MOVE,
		/**
		 * When a player teleports out of a region
		 */
		TELEPORT,
		/**
		 * When a player leaves the game whilst in a region
		 */
		QUIT,
		/**
		 * When a player dies in a region
		 */
		DEATH
	}
	
}
