package redempt.redlib.blockdata.backend;

import org.bukkit.plugin.Plugin;
import redempt.redlib.blockdata.BlockDataManager;
import redempt.redlib.blockdata.ChunkPosition;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a data backend for a BlockDataManager
 *
 * @author Redempt
 */
public interface BlockDataBackend {

    /**
     * Creates a new BlockDataBackend backed by PersistentDataContainer
     *
     * @param plugin The plugin that owns the BlockDataManager
     * @return The BlockDataBackend
     */
    public static BlockDataBackend pdc(Plugin plugin) {
        return new PDCBackend(plugin);
    }

    /**
     * Creates a new BlockDataBackend backed by SQLite
     *
     * @param path The path to the SQLite database
     * @return The BlockDataBackend
     */
    public static BlockDataBackend sqlite(Path path) {
        return new SQLiteBackend(path);
    }

    /**
     * Loads the String data for a given chunk
     *
     * @param pos The location of the chunk
     * @return A CompletableFuture with the String data
     */
    public CompletableFuture<String> load(ChunkPosition pos);

    /**
     * Saves String data for a given chunk
     *
     * @param pos  The location of the chunk
     * @param data The data to save
     * @return A CompletableFuture for the saving task
     */
    public CompletableFuture<Void> save(ChunkPosition pos, String data);

    /**
     * Removes the data attached to a given chunk
     *
     * @param pos The location of the chunk
     * @return A CompletableFuture for the removal task
     */
    public CompletableFuture<Void> remove(ChunkPosition pos);

    /**
     * Saves all data that has been modified with this BlockDataBackend
     *
     * @return A CompletableFuture for the saving task
     */
    public CompletableFuture<Void> saveAll();

    /**
     * Closes and cleans up any connections if needed
     *
     * @return A CompletableFuture for the closing task
     */
    public CompletableFuture<Void> close();

    /**
     * Attempts to load all data stored in the backend, not supported by PDC
     *
     * @return A CompletableFuture with all the data
     */
    public CompletableFuture<Map<ChunkPosition, String>> loadAll();

    /**
     * Attempts to migrate SQLite from an older schema used by the previous BlockDataManager library
     *
     * @param manager The BlockDataManager
     * @return Whether a migration was performed successfully
     */
    public boolean attemptMigration(BlockDataManager manager);

}
