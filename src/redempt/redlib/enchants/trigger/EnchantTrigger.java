package redempt.redlib.enchants.trigger;

import org.bukkit.Material;
import org.bukkit.event.Event;
import redempt.redlib.enchants.CustomEnchant;

/**
 * Represents a trigger for a CustomEnchant which will smartly pass relevant events
 * @author Redempt
 * @param <T> The event type this EventTrigger passes
 */
public interface EnchantTrigger<T extends Event> {
	
	/**
	 * Calls activate with a BlockBreakEvent when a block is broken with an item that has a CustomEnchant with this trigger
	 */
	MineBlockTrigger MINE_BLOCK = new MineBlockTrigger();
	/**
	 * Calls activate with an EntityDamageByEntityEvent when a player attacks an entity with an item that has a CustomEnchant with this trigger
	 */
	AttackEntityTrigger ATTACK_ENTITY = new AttackEntityTrigger();
	/**
	 * Calls activate with an EntityDeathEvent when a player kills an entity with an item that has a CustomEnchant with this trigger
	 */
	KillEntityTrigger KILL_ENTITY = new KillEntityTrigger();
	/**
	 * Calls activate with a ProjectileLaunchEvent when a player shoots a projectile with an item that has a CustomEnchant with this trigger
	 */
	ShootArrowTrigger SHOOT_ARROW = new ShootArrowTrigger();
	/**
	 * Calls activate with an EntityDamageEvent when a player takes damage wearing armor that has a CustomEnchant with this trigger
	 */
	TakeDamageTrigger TAKE_DAMAGE = new TakeDamageTrigger();
	/**
	 * Calls activate with a PlayerChangedArmorEvent when a player equips armor that has a CustomEnchant with this trigger
	 * Also calls activate when a player joins wearing armor with this trigger
	 * Calls deactivate with a PlayerChangedArmorEvent when a player unequips armor that has a CustomEnchant with this trigger
	 * Also calls deactivate when a player leaves wearing armor with this trigger
	 */
	EquipArmorTrigger EQUIP_ARMOR = new EquipArmorTrigger();
	/**
	 * Calls activate with a PlayerChangedHeldItemEvent when a player begins holding an item that has a CustomEnchant with this trigger
	 * Also calls activate when a player joins holding an item with this trigger
	 * Calls deactivate with a PlayerChangedHeldItemEvent when a player stops holding an item that has a CustomEnchant with this trigger
	 * Also calls deactivate when a player leaves holding an item with this trigger
	 */
	HoldItemTrigger HOLD_ITEM = new HoldItemTrigger();
	/**
	 * Does nothing
	 */
	NoneTrigger NONE = new NoneTrigger();
	
	public void register(CustomEnchant<T> ench);
	public boolean defaultAppliesTo(Material type);

}
