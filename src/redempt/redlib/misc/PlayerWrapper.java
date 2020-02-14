package redempt.redlib.misc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.bukkit.entity.Player;

public class PlayerWrapper {
	
	public static Player wrap(Player player, String... disable) {
		return (Player) Proxy.newProxyInstance(player.getClass().getClassLoader(), new Class<?>[] {player.getClass()}, new InvocationHandler() {
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (Arrays.stream(disable).anyMatch(s -> s.equals(method.getName()))) {
					return null;
				}
				return method.invoke(player, args);
			}
			
		});
	}
	
}
