package redempt.redlib.blockdata.backend;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import redempt.redlib.blockdata.BlockDataManager;
import redempt.redlib.blockdata.ChunkPosition;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

class PDCBackend implements BlockDataBackend {

    private NamespacedKey key;

    public PDCBackend(Plugin plugin) {
        key = new NamespacedKey(plugin, "blockData");
    }

    @Override
    public CompletableFuture<String> load(ChunkPosition pos) {
        PersistentDataContainer pdc = pos.getWorld().getChunkAt(pos.getX(), pos.getZ()).getPersistentDataContainer();
        return CompletableFuture.completedFuture(pdc.get(key, PersistentDataType.STRING));
    }

    @Override
    public CompletableFuture<Void> save(ChunkPosition pos, String data) {
        PersistentDataContainer pdc = pos.getWorld().getChunkAt(pos.getX(), pos.getZ()).getPersistentDataContainer();
        pdc.set(key, PersistentDataType.STRING, data);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> remove(ChunkPosition pos) {
        PersistentDataContainer pdc = pos.getWorld().getChunkAt(pos.getX(), pos.getZ()).getPersistentDataContainer();
        pdc.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Map<ChunkPosition, String>> loadAll() {
        throw new UnsupportedOperationException("PDC backend cannot access all data blocks");
    }

    @Override
    public boolean attemptMigration(BlockDataManager manager) {
        return false;
    }

}
