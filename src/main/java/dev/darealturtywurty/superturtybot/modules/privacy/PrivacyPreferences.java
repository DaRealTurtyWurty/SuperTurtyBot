package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.Locale;

public final class PrivacyPreferences {
    public static final String COLLECTION = "privacyOptOuts";
    public static final String BLOCKED_MESSAGE = "You've opted out of optional data processing, so this feature "
        + "isn't supported. Use `/privacy opt-in` to enable it again. Your privacy commands remain available.";
    private static final Set<Long> OPTED_OUT = ConcurrentHashMap.newKeySet();
    private static final ReentrantReadWriteLock MUTATIONS = new ReentrantReadWriteLock(true);
    private static final Set<String> ALLOWED_COMMANDS = Set.of("privacy", "help", "commands", "ping", "uptime",
        "botinfo");

    private PrivacyPreferences() {
    }

    public static void initialize(MongoDatabase database) {
        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            Set<Long> loaded = new HashSet<>();
            try (var cursor = database.getCollection(COLLECTION).find().iterator()) {
                while (cursor.hasNext()) {
                    Object id = cursor.next().get("_id");
                    long userId = Long.parseLong(id.toString());
                    if (userId <= 0)
                        throw new IllegalStateException("Invalid privacy preference identifier");
                    loaded.add(userId);
                }
            }
            OPTED_OUT.clear();
            OPTED_OUT.addAll(loaded);
        } finally {
            lock.unlock();
        }
    }

    public static boolean isOptedOut(long userId) {
        return OPTED_OUT.contains(userId);
    }

    public static boolean isAllowedCommand(String name) {
        return ALLOWED_COMMANDS.contains(name.toLowerCase(Locale.ROOT));
    }

    public static List<Long> excludedUsers() {
        return List.copyOf(OPTED_OUT);
    }

    public static void setOptedOut(MongoDatabase database, long userId, boolean optedOut) {
        if (userId <= 0)
            throw new IllegalArgumentException("A Discord user ID is required");

        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            if (optedOut) {
                OPTED_OUT.add(userId);
                database.getCollection(COLLECTION).replaceOne(Filters.eq("_id", userId),
                    new Document("_id", userId), new ReplaceOptions().upsert(true));
            } else {
                database.getCollection(COLLECTION).deleteMany(Filters.in("_id", userId, Long.toString(userId)));
                OPTED_OUT.remove(userId);
            }
        } finally {
            lock.unlock();
        }
    }

    public static <T> T optionalOperation(Supplier<T> operation) {
        var lock = MUTATIONS.readLock();
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public static <T> T exclusiveOperation(Supplier<T> operation) {
        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public static void requireOptionalData(long userId) {
        if (isOptedOut(userId))
            throw new PrivacyOptOutException();
    }
}
