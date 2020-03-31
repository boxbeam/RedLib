package redempt.redlib.region.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import redempt.redlib.region.Region;

/**
 * Called when a player exits a region with events enabled
 * @author Redempt
 *
 */
public class RegionExitEvent extends Event {
	
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
	
	/**
	 * Constructs a new RegionExitEvent
	 * @param player The player that exited the region
	 * @param region The region that was exited
	 */
	public RegionExitEvent(Player player, Region region, ExitCause cause) {
		this.region = region;
		this.player = player;
		this.cause = cause;
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
		QUIT
	}
	
}
