package redempt.redlib.dev;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.itemutils.ItemUtils;

public class ItemHelper {
	
	@SuppressWarnings("deprecation")
	@CommandHook("setName")
	public void setName(Player player, String name) {
		ItemStack item = player.getItemInHand();
		item = ItemUtils.rename(item, ChatColor.translateAlternateColorCodes('&', name));
		player.setItemInHand(item);
	}
	
	@SuppressWarnings("deprecation")
	@CommandHook("addLore")
	public void addLore(Player player, String lore) {
		ItemStack item = player.getItemInHand();
		item = ItemUtils.addLore(item, ChatColor.translateAlternateColorCodes('&', lore));
		player.setItemInHand(item);
	}
	
}
