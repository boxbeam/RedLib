package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.misc.EventListener;

class TakeDamageTrigger implements EnchantTrigger<EntityDamageEvent> {
	
	@Override
	public void register(CustomEnchant<EntityDamageEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), EntityDamageEvent.class, EventPriority.MONITOR, (e) -> {
			if (e.isCancelled()) {
				return;
			}
			if (!(e.getEntity() instanceof Player)) {
				return;
			}
			Player player = (Player) e.getEntity();
			for (ItemStack item : player.getInventory().getArmorContents()) {
				int level = ench.getLevel(item);
				if (level == 0 || !ench.appliesTo(item.getType())) {
					continue;
				}
				ench.activate(e, level);
			}
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		String str = type.toString();
		return str.endsWith("_BOOTS") || str.endsWith("_CHESTPLATE") || str.endsWith("_LEGGINGS") || str.endsWith("_HELMET");
	}
	
}
