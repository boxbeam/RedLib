package redempt.redlib.misc;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import redempt.redlib.RedLib;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * A cache of offline users by name which can be queried without worrying about web requests
 *
 * @author Redempt
 */
public class UserCache {

    private static Map<String, OfflinePlayer> nameCache;
    private static CompletableFuture<Void> initTask;

    /**
     * Initializes the user cache asynchronously
     *
     * @param onComplete A Runnable to be run when the initialization is complete
     */
    public static synchronized void asyncInit(Runnable onComplete) {
        if (initTask != null || nameCache != null) {
            onComplete.run();
            return;
        }
        initTask = CompletableFuture.runAsync(() -> {
            if (nameCache != null) {
                return;
            }
            nameCache = new ConcurrentHashMap<>();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                nameCache.put(player.getName().toLowerCase(Locale.ROOT), player);
            }
            Task.syncDelayed(() -> new EventListener<>(RedLib.getInstance(), PlayerJoinEvent.class, EventPriority.LOWEST, e -> {
                Player player = e.getPlayer();
                nameCache.put(player.getName().toLowerCase(Locale.ROOT), player);
            }));
        });
    }

    /**
     * Initializes the user cache asynchronously
     */
    public static void asyncInit() {
        asyncInit(() -> {
        });
    }

    /**
     * Initializes the user cache synchronously
     */
    public static void init() {
        asyncInit();
        if (!initTask.isDone()) {
            try {
                initTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets an OfflinePlayer by name, initializing the cache synchronously if it has not been initialized yet
     *
     * @param name The name of the player, case insensitive
     * @return The OfflinePlayer, or null
     */
    public static OfflinePlayer getOfflinePlayer(String name) {
        if (nameCache == null) {
            init();
        }
        return nameCache.get(name.toLowerCase(Locale.ROOT));
    }

}
