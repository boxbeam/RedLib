package redempt.redlib.nms;

import org.bukkit.entity.Player;

/**
 * Functional interface which handles packets
 * @author Redempt
 */
public interface PacketHandler {
	
	/**
	 * Handles a packet
	 * @param player The player the packet was sent to or received from
	 * @param packet The packet
	 * @return True if this packet should be relayed, false if it should be cancelled
	 */
	public boolean handle(Player player, NMSObject packet);
	
}
