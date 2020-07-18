package redempt.redlib.blockdata;

import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Represents an instance of a CustomBlockType. Effectively a wrapper for DataBlock.
 */
public class CustomBlock {
	
	private CustomBlockType<?> type;
	private DataBlock db;
	
	protected CustomBlock(CustomBlockType<?> type, DataBlock db) {
		this.type = type;
		this.db = db;
	}
	
	/**
	 * @return The DataBlock this CustomBlock wraps
	 */
	public DataBlock getDataBlock() {
		return db;
	}
	
	/**
	 * @return The Block this CustomBlock is at
	 */
	public Block getBlock() {
		return db.getBlock();
	}
	
	/**
	 * @return The CustomBlockType which created this CustomBlock
	 */
	public CustomBlockType<?> getType() {
		return type;
	}
	
	/**
	 * Called when this CustomBlock is clicked. Does nothing by default, override to define custom behavior.
	 * @param e The event
	 */
	public void click(PlayerInteractEvent e) {}
	
}
