package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import redempt.redlib.enchants.EventItems;

public class KillEntityTrigger extends EnchantTrigger<EntityDeathEvent> {
	
	@Override
	protected void register() {
		addListener(EntityDeathEvent.class, e -> {
			if (e.getEntity().getKiller() == null) {
				return null;
			}
			return new EventItems(e, e.getEntity().getKiller().getItemInHand());
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type.toString().endsWith("_SWORD");
	}
	
}
