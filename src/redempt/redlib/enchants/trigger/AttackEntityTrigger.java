package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.misc.EventListener;

class AttackEntityTrigger implements EnchantTrigger<EntityDamageByEntityEvent> {
	
	@Override
	public void register(CustomEnchant<EntityDamageByEntityEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), EntityDamageByEntityEvent.class, EventPriority.MONITOR, (e) -> {
			if (e.isCancelled()) {
				return;
			}
			if (!(e.getDamager() instanceof Player)) {
				return;
			}
			Player player = (Player) e.getDamager();
			int level = ench.getLevel(player.getItemInHand());
			if (level == 0 || !ench.appliesTo(player.getItemInHand().getType())) {
				return;
			}
			ench.activate(e, level);
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type.toString().endsWith("_SWORD");
	}
	
}
