package dev.darealturtywurty.superturtybot.core.util;

import lombok.Getter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class SimpleInvalidatingCache<T> {
    private static final long DEFAULT_EXPIRATION_TIME = 60000; // 1 minute
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private T value;
    private long lastUpdate;
    private final long expirationTime;
    private final boolean checkExpired;

    public SimpleInvalidatingCache() {
        this(true);
    }

    public SimpleInvalidatingCache(boolean checkExpired) {
        this(DEFAULT_EXPIRATION_TIME, checkExpired);
    }

    public SimpleInvalidatingCache(long expirationTime, boolean checkExpired) {
        this.value = null;
        this.lastUpdate = 0;
        this.expirationTime = expirationTime;
        this.checkExpired = checkExpired;
        this.executorService.scheduleAtFixedRate(() -> {
            if (!this.checkExpired || isExpired(this.expirationTime)) {
                value = null;
            }
        }, this.expirationTime, this.expirationTime, TimeUnit.MILLISECONDS);
    }

    public void setValue(T value) {
        this.value = value;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void invalidate() {
        this.value = null;
        this.lastUpdate = 0;
    }

    public boolean isExpired(long expirationTime) {
        return System.currentTimeMillis() - lastUpdate > expirationTime;
    }

    public T getOrLoad(Loader<T> loader) {
        if (value == null || (checkExpired && isExpired(expirationTime))) {
            value = loader.load();
            lastUpdate = System.currentTimeMillis();
        }

        return value;
    }

    public interface Loader<T> {
        T load();
    }
}
