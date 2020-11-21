package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.misc.EventListener;

public class KillEntityTrigger implements EnchantTrigger<EntityDeathEvent> {
	
	@Override
	public void register(CustomEnchant<EntityDeathEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), EntityDeathEvent.class, e -> {
			if (e.getEntity().getKiller() == null || e.getEntity().getLastDamageCause().getCause() != DamageCause.ENTITY_ATTACK) {
				return;
			}
			Player player = e.getEntity().getKiller();
			EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) e.getEntity().getLastDamageCause();
			if (!ev.getDamager().equals(player)) {
				return;
			}
			ItemStack held = player.getItemInHand();
			int level = ench.getLevel(held);
			if (level > 0 && ench.appliesTo(held.getType())) {
				ench.activate(e, level);
			}
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type.toString().endsWith("_SWORD");
	}
	
}
