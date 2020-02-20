package redempt.redlib.misc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

/**
 * A compact way to define a Listener using a lambda
 * @author Redempt
 *
 * @param <T> The event being listened for
 */
public class EventListener<T extends Event> implements Listener {
	
	private BiConsumer<EventListener<T>, T> handler;
	
	/**
	 * Creates and registers a Listener for the given event
	 * @param plugin The plugin registering the listener
	 * @param eventClass The class of the event being listened for
	 * @param handler The callback to receive the event
	 */
	public EventListener(Plugin plugin, Class<T> eventClass, BiConsumer<EventListener<T>, T> handler) {
		this.handler = handler;
		try {
			Method method = eventClass.getMethod("getHandlerList");
			method.setAccessible(true);
			HandlerList list = (HandlerList) method.invoke(null);
	        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(this, plugin).entrySet()) {
	            list.registerAll(entry.getValue());
	        }
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void handleEvent(T event) {
		handler.accept(this, event);
	}
	
	/**
	 * Unregisters this listener
	 */
	public void unregister() {
		HandlerList.unregisterAll(this);
	}
	
}
