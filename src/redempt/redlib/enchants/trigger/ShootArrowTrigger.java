package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import redempt.redlib.RedLib;
import redempt.redlib.enchants.CustomEnchant;
import redempt.redlib.misc.EventListener;

class ShootArrowTrigger implements EnchantTrigger<ProjectileLaunchEvent> {
	
	@Override
	public void register(CustomEnchant<ProjectileLaunchEvent> ench) {
		new EventListener<>(ench.getRegistry().getPlugin(), ProjectileLaunchEvent.class, EventPriority.MONITOR, e -> {
			ProjectileSource source = e.getEntity().getShooter();
			if (!(source instanceof Player)) {
				return;
			}
			Player player = (Player) source;
			int level = ench.getLevel(player.getItemInHand());
			if (level == 0 || !ench.appliesTo(player.getItemInHand().getType())) {
				if (RedLib.midVersion >= 9) {
					level = ench.getLevel(player.getInventory().getItemInOffHand());
				}
				if (level == 0) {
					return;
				}
			}
			ench.activate(e, level);
		});
	}
	
	@Override
	public boolean defaultAppliesTo(Material type) {
		return type == Material.BOW;
	}
	
}
