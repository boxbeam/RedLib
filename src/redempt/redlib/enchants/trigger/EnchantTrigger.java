package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import redempt.redlib.enchants.EventItems;
import redempt.redlib.enchants.events.PlayerChangedArmorEvent;
import redempt.redlib.enchants.events.PlayerChangedHeldItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a trigger for a CustomEnchant which will smartly pass relevant events
 *
 * @param <T> The event type this EventTrigger passes
 * @author Redempt
 */
public abstract class EnchantTrigger<T extends Event> {

    /**
     * Calls activate with a BlockBreakEvent when a block is broken with an item that has a CustomEnchant with this trigger
     */
    public static final EnchantTrigger<BlockBreakEvent> MINE_BLOCK = new MineBlockTrigger();
    /**
     * Calls activate with an EntityDamageByEntityEvent when a player attacks an entity with an item that has a CustomEnchant with this trigger
     */
    public static final EnchantTrigger<EntityDamageByEntityEvent> ATTACK_ENTITY = new AttackEntityTrigger();
    /**
     * Calls activate with an EntityDeathEvent when a player kills an entity with an item that has a CustomEnchant with this trigger
     */
    public static final EnchantTrigger<EntityDeathEvent> KILL_ENTITY = new KillEntityTrigger();
    /**
     * Calls activate with a ProjectileLaunchEvent when a player shoots a projectile with an item that has a CustomEnchant with this trigger
     */
    public static final EnchantTrigger<ProjectileLaunchEvent> SHOOT_ARROW = new ShootArrowTrigger();
    /**
     * Calls activate with an EntityDamageEvent when a player takes damage wearing armor that has a CustomEnchant with this trigger
     */
    public static final EnchantTrigger<EntityDamageEvent> TAKE_DAMAGE = new TakeDamageTrigger();
    /**
     * Calls activate with a PlayerChangedArmorEvent when a player equips armor that has a CustomEnchant with this trigger
     * Also calls activate when a player joins wearing armor with this trigger
     * Calls deactivate with a PlayerChangedArmorEvent when a player unequips armor that has a CustomEnchant with this trigger
     * Also calls deactivate when a player leaves wearing armor with this trigger
     */
    public static final EnchantTrigger<PlayerChangedArmorEvent> EQUIP_ARMOR = new EquipArmorTrigger();
    /**
     * Calls activate with a PlayerChangedHeldItemEvent when a player begins holding an item that has a CustomEnchant with this trigger
     * Also calls activate when a player joins holding an item with this trigger
     * Calls deactivate with a PlayerChangedHeldItemEvent when a player stops holding an item that has a CustomEnchant with this trigger
     * Also calls deactivate when a player leaves holding an item with this trigger
     */
    public static final EnchantTrigger<PlayerChangedHeldItemEvent> HOLD_ITEM = new HoldItemTrigger();

    protected Map<Class<? extends Event>, Function<Event, EventItems>> events = new HashMap<>();

    protected abstract void register();

    public final void init() {
        events.clear();
        register();
    }

    /**
     * Registers a listener for this EventTrigger to get the items and event for the specified event
     *
     * @param eventClass The class of the event
     * @param func       The function to get the EventItems from the event
     * @param <T>        The event type
     */
    protected <T extends Event> void addListener(Class<T> eventClass, Function<T, EventItems> func) {
        events.put(eventClass, (Function<Event, EventItems>) func);
    }

    /**
     * Gets the event listeners registered by this EnchantTrigger
     *
     * @return The map of events to their functions which will retrieve relevant items
     */
    public Map<Class<? extends Event>, Function<Event, EventItems>> getEvents() {
        return events;
    }

    /**
     * @return The EventPriority this EnchantTrigger's listeners should be registered with
     */
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }

    /**
     * Returns whether this EnchantTrigger applies to the given type by default
     *
     * @param type The type to check
     * @return Whether this EnchantTrigger applies to the type by default
     */
    public abstract boolean defaultAppliesTo(Material type);

    /**
     * Creates a copy of this EnchantTrigger with a different EventPriority
     *
     * @param priority The EventPriority to set on the copy
     * @return A copy of this EnchantTrigger with the given priority
     */
    public EnchantTrigger<T> withPriority(EventPriority priority) {
        Predicate<Material> appliesTo = this::defaultAppliesTo;
        Map<Class<? extends Event>, Function<Event, EventItems>> eventMap = this.events;
        Runnable register = () -> {
            if (events.isEmpty()) {
                register();
            }
        };
        return new EnchantTrigger<T>() {
            @Override
            protected void register() {
                register.run();
                this.events = eventMap;
            }

            @Override
            public boolean defaultAppliesTo(Material type) {
                return appliesTo.test(type);
            }

            @Override
            public EventPriority getPriority() {
                return priority;
            }
        };
    }

}
