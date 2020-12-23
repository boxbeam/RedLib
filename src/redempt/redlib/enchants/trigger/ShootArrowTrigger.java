package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import redempt.redlib.enchants.EventItems;

class ShootArrowTrigger extends EnchantTrigger<ProjectileLaunchEvent> {
	
	@Override
	protected void register() {
		addListener(ProjectileLaunchEvent.class, e -> {
			if (!(e.getEntity().getShooter() instanceof Player)) {
				return null;
			}
			return new EventItems(e, ((Player) e.getEntity().getShooter()).getItemInHand());
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type == Material.BOW;
	}
	
}
