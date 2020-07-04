package redempt.redlib.protection;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import redempt.redlib.protection.ProtectionPolicy.ProtectionType;

/**
 * Represents a policy that allows players to bypass certain protection types for certain blocks
 * @author Redempt
 *
 */
public interface BypassPolicy {
	
	/**
	 * Checks whether a player can bypass the given protection type for the given block
	 * @param player The player attempting an action
	 * @param type The type of action being attempted
	 * @param block The block the action is being performed on
	 * @return Whether this player can bypass the protection type for the given block
	 */
	public boolean canBypass(Player player, ProtectionType type, Block block);
	
}
