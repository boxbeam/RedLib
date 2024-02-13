package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import redempt.redlib.enchants.EventItems;

class TakeDamageTrigger extends EnchantTrigger<EntityDamageEvent> {

    @Override
    protected void register() {
        addListener(EntityDamageEvent.class, e -> {
            if (!(e.getEntity() instanceof Player)) {
                return null;
            }
            return new EventItems(e, ((Player) e.getEntity()).getInventory().getArmorContents());
        });
        addListener(EntityDamageByEntityEvent.class, e -> {
            if (!(e.getEntity() instanceof Player)) {
                return null;
            }
            return new EventItems(e, ((Player) e.getEntity()).getInventory().getArmorContents());
        });
        addListener(EntityDamageByBlockEvent.class, e -> {
            if (!(e.getEntity() instanceof Player)) {
                return null;
            }
            return new EventItems(e, ((Player) e.getEntity()).getInventory().getArmorContents());
        });
    }

    @Override
    public boolean defaultAppliesTo(Material type) {
        String str = type.toString();
        return str.endsWith("_BOOTS") || str.endsWith("_CHESTPLATE") || str.endsWith("_LEGGINGS") || str.endsWith("_HELMET");
    }

    @Override
    public EventPriority getPriority() {
        return EventPriority.MONITOR;
    }

}
