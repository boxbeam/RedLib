package redempt.redlib.misc;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import redempt.redlib.RedLib;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache of offline users by name which can be queried without worrying about web requests
 * @author Redempt
 */
public class UserCache {
	
	private static Map<String, OfflinePlayer> nameCache;
	
	/**
	 * Initializes the user cache asynchronously
	 * @param onComplete A Runnable to be run when the initialization is complete
	 */
	public static void asyncInit(Runnable onComplete) {
		Task.asyncDelayed(() -> {
			init();
			onComplete.run();
		});
	}
	
	/**
	 * Initializes the user cache asynchronously
	 */
	public static void asyncInit() {
		asyncInit(() -> {});
	}
	
	/**
	 * Initializes the user cache synchronously
	 */
	public static void init() {
		if (nameCache != null) {
			return;
		}
		nameCache = new ConcurrentHashMap<>();
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			nameCache.put(player.getName().toLowerCase(Locale.ROOT), player);
		}
		new EventListener<>(RedLib.getInstance(), PlayerJoinEvent.class, EventPriority.LOWEST, e -> {
			Player player = e.getPlayer();
			nameCache.put(player.getName().toLowerCase(Locale.ROOT), player);
		});
	}
	
	/**
	 * Gets an OfflinePlayer by name
	 * @param name The name of the player, case insensitive
	 * @return The OfflinePlayer, or null
	 */
	public static OfflinePlayer getOfflinePlayer(String name) {
		return nameCache.get(name.toLowerCase(Locale.ROOT));
	}
	
}
