package redempt.redlib.misc;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import redempt.redlib.RedLib;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A compact way to define a Listener using a lambda
 *
 * @param <T> The event being listened for
 * @author Redempt
 */
public class EventListener<T extends Event> implements Listener {

    private BiConsumer<EventListener<T>, T> handler;
    private Class<T> eventClass;

    /**
     * Creates and registers a Listener for the given event
     *
     * @param eventClass The class of the event being listened for
     * @param priority   The EventPriority for this listener
     * @param handler    The callback to receive the event and this EventListener
     */
    public EventListener(Class<T> eventClass, EventPriority priority, BiConsumer<EventListener<T>, T> handler) {
        this(RedLib.getCallingPlugin(), eventClass, priority, handler);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param plugin     The plugin registering the listener
     * @param eventClass The class of the event being listened for
     * @param priority   The EventPriority for this listener
     * @param handler    The callback to receive the event and this EventListener
     */
    public EventListener(Plugin plugin, Class<T> eventClass, EventPriority priority, BiConsumer<EventListener<T>, T> handler) {
        this.handler = handler;
        this.eventClass = eventClass;
        Bukkit.getPluginManager().registerEvent(eventClass, this, priority, (l, e) -> handleEvent((T) e), plugin);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param eventClass The class of the event being listened for
     * @param priority   The EventPriority for this listener
     * @param handler    The callback to receive the event
     */
    public EventListener(Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
        this(RedLib.getCallingPlugin(), eventClass, priority, handler);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param plugin     The plugin registering the listener
     * @param eventClass The class of the event being listened for
     * @param priority   The EventPriority for this listener
     * @param handler    The callback to receive the event
     */
    public EventListener(Plugin plugin, Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
        this(plugin, eventClass, priority, (l, e) -> handler.accept(e));
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param eventClass The class of the event being listened for
     * @param handler    The callback to receive the event and this EventListener
     */
    public EventListener(Class<T> eventClass, BiConsumer<EventListener<T>, T> handler) {
        this(RedLib.getCallingPlugin(), eventClass, handler);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param plugin     The plugin registering the listener
     * @param eventClass The class of the event being listened for
     * @param handler    The callback to receive the event and this EventListener
     */
    public EventListener(Plugin plugin, Class<T> eventClass, BiConsumer<EventListener<T>, T> handler) {
        this(plugin, eventClass, EventPriority.NORMAL, handler);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param eventClass The class of the event being listened for
     * @param handler    The callback to receive the event
     */
    public EventListener(Class<T> eventClass, Consumer<T> handler) {
        this(RedLib.getCallingPlugin(), eventClass, handler);
    }

    /**
     * Creates and registers a Listener for the given event
     *
     * @param plugin     The plugin registering the listener
     * @param eventClass The class of the event being listened for
     * @param handler    The callback to receive the event
     */
    public EventListener(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        this(plugin, eventClass, EventPriority.NORMAL, handler);
    }

    @EventHandler
    public void handleEvent(T event) {
        if (eventClass.isAssignableFrom(event.getClass())) {
            handler.accept(this, event);
        }
    }

    /**
     * Unregisters this listener
     */
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

}
