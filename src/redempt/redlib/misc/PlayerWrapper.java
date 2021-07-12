package redempt.redlib.misc;

import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class PlayerWrapper {
	
	/**
	 * Wraps a player using a Proxy, disabling some methods from being called. Very hacky, do not use if it can be avoided.
	 * Meant for when you want to send a fake event to test if it will be cancelled, but don't want the plugins to be able to do certain things with the player based on the event. 
	 * @param player The player to wrap
	 * @param disable The names of the methods to disable
	 * @return The wrapped player
	 */
	public static Player wrap(Player player, String... disable) {
		return (Player) Proxy.newProxyInstance(player.getClass().getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
			if (Arrays.stream(disable).anyMatch(s -> s.equals(method.getName()))) {
				return null;
			}
			return method.invoke(player, args);
		});
	}
	
}
