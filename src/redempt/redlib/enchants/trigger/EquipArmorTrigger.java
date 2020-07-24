package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.enchants.events.PlayerChangedArmorEvent;
import redempt.redlib.misc.EventListener;

class EquipArmorTrigger implements EnchantTrigger<PlayerChangedArmorEvent> {
	
	@Override
	public void register(CustomEnchant<PlayerChangedArmorEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerChangedArmorEvent.class, e -> {
			for (int i = 0; i < e.getPreviousArmor().length; i++) {
				ItemStack prev = e.getPreviousArmor()[i];
				ItemStack current = e.getNewArmor()[i];
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
			}
		});
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerQuitEvent.class, e -> {
			for (ItemStack item : e.getPlayer().getInventory().getArmorContents()) {
				int level = ench.getLevel(item);
				if (level != 0 && ench.appliesTo(item.getType())) {
					ench.deactivate(new PlayerChangedArmorEvent(e.getPlayer(), e.getPlayer().getInventory().getArmorContents(), new ItemStack[4]), level);
				}
			}
		});
		new EventListener<>(ench.getRegistry().getPlugin(), PlayerJoinEvent.class, e -> {
			for (ItemStack item : e.getPlayer().getInventory().getArmorContents()) {
				int level = ench.getLevel(item);
				if (level != 0 && ench.appliesTo(item.getType())) {
					ench.activate(new PlayerChangedArmorEvent(e.getPlayer(), new ItemStack[4], e.getPlayer().getInventory().getArmorContents()), level);
				}
			}
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		String str = type.toString();
		return str.endsWith("_BOOTS") || str.endsWith("_CHESTPLATE") || str.endsWith("_LEGGINGS") || str.endsWith("_HELMET");
	}
	
}
