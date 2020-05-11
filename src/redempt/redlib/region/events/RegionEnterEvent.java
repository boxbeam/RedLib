package redempt.redlib.region.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import redempt.redlib.region.Region;

/**
 * Called when a player enters a region with events enabled
 * @author Redempt
 *
 */
public class RegionEnterEvent extends Event {
	
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
	
	/**
	 * Constructs a new RegionEnterEvent
	 * @param player The player who entered the region
	 * @param region The region that was entered
	 * @param cause What caused the player to enter the region
	 */
	public RegionEnterEvent(Player player, Region region, EnterCause cause) {
		this.region = region;
		this.player = player;
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
