package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.misc.EventListener;

class MineBlockTrigger implements EnchantTrigger<BlockBreakEvent> {
	
	@Override
	public void register(CustomEnchant<BlockBreakEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), BlockBreakEvent.class, EventPriority.MONITOR, (e) -> {
			if (e.isCancelled()) {
				return;
			}
			int level = ench.getLevel(e.getPlayer().getItemInHand());
			if (level > 0 && ench.appliesTo(e.getPlayer().getItemInHand().getType())) {
				ench.activate(e, level);
			}
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type.toString().endsWith("_PICKAXE");
	}
	
}
