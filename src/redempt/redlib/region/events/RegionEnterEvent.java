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
	
	/**
	 * Constructs a new RegionEnterEvent
	 * @param player The player who entered the region
	 * @param region The region that was entered
	 */
	public RegionEnterEvent(Player player, Region region) {
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

}
