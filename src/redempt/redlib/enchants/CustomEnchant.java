package redempt.redlib.enchants;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import redempt.redlib.enchants.trigger.EnchantTrigger;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Represents a custom enchantment created by another plugin
 *
 * @author Redempt
 */
public abstract class CustomEnchant {

    /**
     * Converts a number to roman numerals, between 1 and 10
     *
     * @param num The number to convert
     * @return The roman numerals representation of the number
     */
    public static String toRomanNumerals(int num) {
        switch (num) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return num + "";
        }
    }

    /**
     * Converts roman numeral string, between 1 and 10, back to a number
     *
     * @param romanNumerals The roman numerals string
     * @return The number represented by the roman numerals
     */
    public static int fromRomanNumerals(String romanNumerals) {
        switch (romanNumerals) {
            case "I":
                return 1;
            case "II":
                return 2;
            case "III":
                return 3;
            case "IV":
                return 4;
            case "V":
                return 5;
            case "VI":
                return 6;
            case "VII":
                return 7;
            case "VIII":
                return 8;
            case "IX":
                return 9;
            case "X":
                return 10;
            default:
                for (char c : romanNumerals.toCharArray()) {
                    if (c > '9' || c < '0') {
                        return 0;
                    }
                }
                return Integer.parseInt(romanNumerals);
        }
    }

    private EnchantRegistry registry;
    private Map<EnchantTrigger<?>, EnchantListener<?>> triggers = new HashMap<>();
    private int maxLevel;
    private String name;
    private EnumSet<Material> applicable;

    /**
     * Constructs a new CustomEnchant
     *
     * @param name     The name of this CustomEnchant
     * @param maxLevel The max level of this CustomEnchant
     */
    public CustomEnchant(String name, int maxLevel) {
        if (this.name != null) {
            throw new IllegalStateException("This enchantment has already been instantiated, get it through the EnchantRegistry!");
        }
        this.name = name;
        this.maxLevel = maxLevel;
    }

    protected void register(EnchantRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers an EnchantTrigger with a listener
     *
     * @param trigger    The EnchantTrigger to register
     * @param activate   The callback for when this trigger is activated
     * @param deactivate The callback for when this trigger is deactivated
     * @param <T>        The event type
     */
    protected <T extends Event> void addTrigger(EnchantTrigger<T> trigger, BiConsumer<T, Integer> activate, BiConsumer<T, Integer> deactivate) {
        triggers.put(trigger, new EnchantListener<>(activate, deactivate));
    }

    /**
     * Registers an EnchantTrigger with a listener
     *
     * @param trigger  The EnchantTrigger to register
     * @param activate The callback for when this trigger is activated
     * @param <T>      The event type
     */
    protected <T extends Event> void addTrigger(EnchantTrigger<T> trigger, BiConsumer<T, Integer> activate) {
        triggers.put(trigger, new EnchantListener<>(activate, (e, l) -> {
        }));
    }

    /**
     * @return An array of all other CustomEnchants that are incompatible with this one
     */
    public CustomEnchant[] getIncompatible() {
        return new CustomEnchant[]{};
    }

    /**
     * @return The EventTrigger for this CustomEnchant
     */
    public final Map<EnchantTrigger<?>, EnchantListener<?>> getTriggers() {
        return triggers;
    }

    /**
     * Checks whether this CustomEnchant applies to a certain item type
     *
     * @param type The type to check
     * @return Whether this CustomEnchant applies to the given type
     */
    protected boolean appliesTo(Material type) {
        return triggers.keySet().stream().anyMatch(t -> t.defaultAppliesTo(type));
    }

    /**
     * @return The name of this CustomEnchant
     */
    public final String getName() {
        return name;
    }

    /**
     * @return The ID of this CustomEnchant, the same as a lowercase version of the name that has spaces replaced with underscores
     */
    public final String getId() {
        return name.toLowerCase().replace(" ", "_");
    }

    /**
     * @return The max level of this CustomEnchant
     */
    public final int getMaxLevel() {
        return maxLevel;
    }

    /**
     * @return The EnchantRegistry this CustomEnchant is registered to, or null if it has not yet been registered
     */
    public final EnchantRegistry getRegistry() {
        return registry;
    }

    /**
     * @return Whether this CustomEnchant has been registered yet
     */
    public final boolean isRegistered() {
        return registry != null;
    }

    /**
     * @return The display name of this CustomEnchant, generated using the namer function of its EnchantRegistry
     */
    public String getDisplayName() {
        return registry.getDisplayName(this);
    }

    /**
     * Applies this CustomEnchant to an item, replacing it if it was already present. Removes if level is 0.
     *
     * @param item  The item to apply this CustomEnchant to
     * @param level The level to apply
     * @return The enchanted item
     */
    public ItemStack apply(ItemStack item, int level) {
        if (item == null) {
            return null;
        }
        if (level == 0) {
            return remove(item);
        }
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        lore = lore == null ? new ArrayList<>() : lore;
        int where = -1;
        boolean replace = false;
        String displayName = getDisplayName();
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (where == -1) {
                if (registry.fromLoreLine(line) != null) {
                    where = i + 1;
                }
            }
            if (line.startsWith(displayName)) {
                replace = true;
                where = i;
                break;
            }
        }
        if (where == -1) {
            where = lore.size();
        }
        if (replace) {
            lore.set(where, getLore(level));
        } else {
            lore.add(where, getLore(level));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Removes this CustomEnchant from the given item
     *
     * @param item The item to remove this CustomEnchant from
     * @return The item with the enchant removed
     */
    public ItemStack remove(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return item;
        }
        String displayName = getDisplayName();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        lore.removeIf(s -> s.startsWith(displayName));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets the level of this CustomEnchant on an item
     *
     * @param item The item to check the level on
     * @return The level on the item, 0 if it is absent or if the item is null
     */
    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return 0;
        }
        List<String> lore = item.getItemMeta().getLore();
        String displayName = getDisplayName();
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = lore.get(i);
            if (line.startsWith(displayName)) {
                if (line.length() == displayName.length()) {
                    return 1;
                }
                return fromRomanNumerals(line.substring(displayName.length() + 1));
            }
        }
        return 0;
    }

    /**
     * Checks if this CustomEnchant can be applied to the given item
     *
     * @param item The item to check
     * @return False if this CustomEnchantment cannot be applied to the item's type, or one of the CustomEnchants already on the item is incompatible with this one, true otherwise
     */
    public final boolean canApply(ItemStack item) {
        if (!canApply(item.getType())) {
            return false;
        }
        Map<CustomEnchant, Integer> enchants = registry.getEnchants(item);
        for (CustomEnchant ench : getIncompatible()) {
            if (enchants.containsKey(ench)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether this CustomEnchant applies to the given type
     *
     * @param type The type
     * @return Whether this CustomEnchant applies to the type
     */
    public final boolean canApply(Material type) {
        if (applicable == null) {
            applicable = EnumSet.noneOf(Material.class);
            Arrays.stream(Material.values()).filter(this::appliesTo).forEach(applicable::add);
        }
        return applicable.contains(type);
    }

    /**
     * Checks if this CustomEnchant is compatible with another CustomEnchant
     *
     * @param ench The CustomEnchant to check compatibility with
     * @return Whether this CustomEnchant is compatible with the given enchant
     */
    public boolean isCompatible(CustomEnchant ench) {
        return Arrays.stream(getIncompatible()).noneMatch(e -> e.equals(ench));
    }

    /**
     * Gets the lore that will be added to an item if this CustomEnchant is applied at the given level
     *
     * @param level The level to be specified in the lore
     * @return The line of lore
     */
    public String getLore(int level) {
        if (level == 1 && maxLevel == 1) {
            return getDisplayName();
        }
        return getDisplayName() + " " + toRomanNumerals(level);
    }

    /**
     * Increase the level of this enchantment on an item, ensuring it does not exceed the maximum level
     *
     * @param itemStack     The targeted item stack of which contains the CustomEnchant to be increased.
     * @param customEnchant The CustomEnchant to be increased on the ItemStack.
     * @param amount        The amount to increase the enchant level by.
     * @return The item stack with the increased enchant level.
     */
    public ItemStack increaseLevel(ItemStack itemStack, CustomEnchant customEnchant, int amount) {
        return customEnchant.apply(itemStack, Math.min(customEnchant.getLevel(itemStack) + amount, customEnchant.getMaxLevel()));
    }
}
