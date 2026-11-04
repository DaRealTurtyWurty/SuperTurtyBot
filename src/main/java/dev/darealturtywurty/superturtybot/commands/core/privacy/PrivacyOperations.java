package dev.darealturtywurty.superturtybot.commands.core.privacy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.modules.privacy.PrivacyDataService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class PrivacyOperations {
    private final Set<Long> running = ConcurrentHashMap.newKeySet();
    private final Semaphore exports = new Semaphore(2);
    private final Cache<Long, Boolean> exportCooldown = CacheBuilder.newBuilder()
        .maximumSize(10_000).expireAfterWrite(1, TimeUnit.MINUTES).build();

    public boolean tryBegin(long userId) {
        return this.running.add(userId);
    }

    public void finish(long userId) {
        this.running.remove(userId);
    }

    public boolean tryAcquireExport() {
        return this.exports.tryAcquire();
    }

    public void releaseExport() {
        this.exports.release();
    }

    public boolean tryBeginExportCooldown(long userId) {
        return this.exportCooldown.asMap().putIfAbsent(userId, true) == null;
    }

    public void clearExportCooldown(long userId) {
        this.exportCooldown.invalidate(userId);
    }

    public static PrivacyDataService service() {
        return new PrivacyDataService(Database.getDatabase().mongoDatabase);
    }
}
