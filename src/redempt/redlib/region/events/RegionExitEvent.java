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
	
	/**
	 * Constructs a new RegionExitEvent
	 * @param player The player that exited the region
	 * @param region The region that was exited
	 */
	public RegionExitEvent(Player player, Region region) {
		this.region = region;
		this.player = player;
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

}
