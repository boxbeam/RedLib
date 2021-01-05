package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import redempt.redlib.enchants.EventItems;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a trigger for a CustomEnchant which will smartly pass relevant events
 * @author Redempt
 * @param <T> The event type this EventTrigger passes
 */
public abstract class EnchantTrigger<T extends Event> {
	
	/**
	 * Calls activate with a BlockBreakEvent when a block is broken with an item that has a CustomEnchant with this trigger
	 */
	public static final MineBlockTrigger MINE_BLOCK = new MineBlockTrigger();
	/**
	 * Calls activate with an EntityDamageByEntityEvent when a player attacks an entity with an item that has a CustomEnchant with this trigger
	 */
	public static final AttackEntityTrigger ATTACK_ENTITY = new AttackEntityTrigger();
	/**
	 * Calls activate with an EntityDeathEvent when a player kills an entity with an item that has a CustomEnchant with this trigger
	 */
	public static final KillEntityTrigger KILL_ENTITY = new KillEntityTrigger();
	/**
	 * Calls activate with a ProjectileLaunchEvent when a player shoots a projectile with an item that has a CustomEnchant with this trigger
	 */
	public static final ShootArrowTrigger SHOOT_ARROW = new ShootArrowTrigger();
	/**
	 * Calls activate with an EntityDamageEvent when a player takes damage wearing armor that has a CustomEnchant with this trigger
	 */
	public static final TakeDamageTrigger TAKE_DAMAGE = new TakeDamageTrigger();
	/**
	 * Calls activate with a PlayerChangedArmorEvent when a player equips armor that has a CustomEnchant with this trigger
	 * Also calls activate when a player joins wearing armor with this trigger
	 * Calls deactivate with a PlayerChangedArmorEvent when a player unequips armor that has a CustomEnchant with this trigger
	 * Also calls deactivate when a player leaves wearing armor with this trigger
	 */
	public static final EquipArmorTrigger EQUIP_ARMOR = new EquipArmorTrigger();
	/**
	 * Calls activate with a PlayerChangedHeldItemEvent when a player begins holding an item that has a CustomEnchant with this trigger
	 * Also calls activate when a player joins holding an item with this trigger
	 * Calls deactivate with a PlayerChangedHeldItemEvent when a player stops holding an item that has a CustomEnchant with this trigger
	 * Also calls deactivate when a player leaves holding an item with this trigger
	 */
	public static final HoldItemTrigger HOLD_ITEM = new HoldItemTrigger();
	
	private Map<Class<? extends Event>, Function<Event, EventItems>> events = new HashMap<>();
	
	protected abstract void register();
	
	public final void init() {
		events.clear();
		register();
	}
	
	/**
	 * Registers a listener for this EventTrigger to get the items and event for the specified event
	 * @param eventClass The class of the event
	 * @param func The function to get the EventItems from the event
	 * @param <T> The event type
	 */
	protected <T extends Event> void addListener(Class<T> eventClass, Function<T, EventItems> func) {
		events.put(eventClass, (Function<Event, EventItems>) func);
	}
	
	/**
	 * Gets the event listeners registered by this EnchantTrigger
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
	 * @param type The type to check
	 * @return Whether this EnchantTrigger applies to the type by default
	 */
	public abstract boolean defaultAppliesTo(Material type);

}
