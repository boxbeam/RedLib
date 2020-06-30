package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.Event;
import redempt.redlib.enchants.CustomEnchant;

class NoneTrigger implements EnchantTrigger<Event> {
	
	@Override
	public void register(CustomEnchant<Event> ench) {
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return false;
	}
	
}
