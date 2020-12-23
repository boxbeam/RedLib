package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import redempt.redlib.enchants.EventItems;

class MineBlockTrigger extends EnchantTrigger<BlockBreakEvent> {
	
	@Override
	protected void register() {
		addListener(BlockBreakEvent.class, e -> new EventItems(e, e.getPlayer().getItemInHand()));
	}
	
	@Override
	public EventPriority getPriority() {
		return EventPriority.MONITOR;
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type.toString().endsWith("_PICKAXE");
	}
	
}
