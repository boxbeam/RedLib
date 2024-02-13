package redempt.redlib.itemutils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a custom item with special behavior
 *
 * @author Redempt
 */
public abstract class CustomItem {

    /**
     * Gets an instance of every class which extends CustomItem in your plugin, and puts them in a map by name
     * Note: Custom item classes MUST have a default constructor which takes no arguments to be loaded by this method
     *
     * @param plugin The plugin to get the custom items from
     * @return A map of the custom items by name
     */
    public static Map<String, CustomItem> getAll(Plugin plugin) {
        List<Class<? extends CustomItem>> list = RedLib.getExtendingClasses(plugin, CustomItem.class);
        Map<String, CustomItem> map = new HashMap<>();
        for (Class<?> clazz : list) {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                CustomItem citem = (CustomItem) constructor.newInstance();
                map.put(citem.getName(), citem);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new IllegalStateException("Class " + clazz.getName() + " does not have a default constructor or could not be loaded", e);
            }
        }
        return map;
    }

    private String name;
    private ItemStack item;

    /**
     * A constructor that should only be called by {@link CustomItem#getAll(Plugin)}
     *
     * @param name The name of the CustomItem - insert a constant when overriding this constructor, do not take it as a parameter of the overridden constructor
     */
    protected CustomItem(String name) {
        this.name = name;
        item = getDefaultItem();
    }

    /**
     * @return Whether the item should be cloned before being returned
     */
    protected boolean cloneOnGet() {
        return false;
    }

    /**
     * @return The default item for this CustomItem
     */
    public abstract ItemStack getDefaultItem();

    /**
     * @return The name of this custom item
     */
    public final String getName() {
        return name;
    }

    /**
     * @return The item
     */
    public ItemStack getItem() {
        if (cloneOnGet()) {
            return item.clone();
        }
        return item;
    }

}
