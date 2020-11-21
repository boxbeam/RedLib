package redempt.redlib.enchants.trigger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.enchants.events.PlayerChangedHeldItemEvent;
import redempt.redlib.misc.EventListener;

class HoldItemTrigger implements EnchantTrigger<PlayerChangedHeldItemEvent> {
	
	@Override
	public void register(CustomEnchant<PlayerChangedHeldItemEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerChangedHeldItemEvent.class, e -> {
			ItemStack prev = e.getPreviousItem();
			ItemStack current = e.getNewItem();
			int flevel = ench.getLevel(prev);
			int slevel = ench.getLevel(current);
			if (flevel != slevel) {
				if (flevel != 0 && ench.appliesTo(prev.getType())) {
					ench.deactivate(e, flevel);
				}
				if (slevel != 0 && ench.appliesTo(current.getType())) {
					ench.activate(e, slevel);
				}
			}
		});
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerQuitEvent.class, e -> {
			ItemStack item = e.getPlayer().getItemInHand();
			int level = ench.getLevel(item);
			if (level != 0 && ench.appliesTo(item.getType())) {
				ench.deactivate(new PlayerChangedHeldItemEvent(e.getPlayer(), e.getPlayer().getItemInHand(), null), level);
			}
		});
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerJoinEvent.class, e -> {
			ItemStack item = e.getPlayer().getItemInHand();
			int level = ench.getLevel(item);
			if (level != 0 && ench.appliesTo(item.getType())) {
				ench.activate(new PlayerChangedHeldItemEvent(e.getPlayer(), null, e.getPlayer().getItemInHand()), level);
			}
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		String str = type.toString();
		return str.endsWith("_PICKAXE");
	}
	
}
