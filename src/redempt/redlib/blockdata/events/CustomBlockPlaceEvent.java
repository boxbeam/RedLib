package redempt.redlib.blockdata.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import redempt.redlib.blockdata.CustomBlock;
import redempt.redlib.blockdata.CustomBlockType;

/**
 * Called when a CustomBlock is placed by a Player
 */
public class CustomBlockPlaceEvent extends BlockEvent implements Cancellable {
	
	private static HandlerList handlers = new HandlerList();
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private boolean cancelled = false;
	private CustomBlockType<?> type;
	private Player player;
	
	/**
	 * Constructs a new CustomBlockPlaceEvent
	 * @param block The block that was placed
	 * @param type The type of CustomBlock that is being placed
	 * @param player The Player that placed the block
	 */
	public CustomBlockPlaceEvent(Block block, CustomBlockType<?> type, Player player) {
		super(block);
		this.type = type;
		this.player = player;
	}
	
	/**
	 * @return The Player that placed the CustomBlock
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * @return The CustomBlockType that is being placed
	 */
	public CustomBlockType<?> getCustomBlockType() {
		return type;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
}
