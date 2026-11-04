package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoDatabase;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrivacyRetentionManager {
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private PrivacyRetentionManager() {
    }

    public static void start(MongoDatabase database) {
        if (!STARTED.compareAndSet(false, true))
            return;
        var scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "privacy-retention");
            thread.setDaemon(true);
            return thread;
        });
        ShutdownHooks.register(scheduler::shutdown);
        var service = new DataRetentionService(database, ManagedTemporaryFiles.INSTANCE, Clock.systemUTC());
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                DataRetentionService.CleanupReport report = service.run();
                Constants.LOGGER.info(
                    "Daily privacy cleanup removed {} records or temporary workspaces; failed steps: {}",
                    report.removed().values().stream().mapToLong(Long::longValue).sum(), report.failedSteps());
            } catch (RuntimeException exception) {
                Constants.LOGGER.warn("Daily privacy cleanup failed ({}); the next scheduled run will retry",
                    exception.getClass().getSimpleName());
            }
        }, 0, 1, TimeUnit.DAYS);
    }
}
