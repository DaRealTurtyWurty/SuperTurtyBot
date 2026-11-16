package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.Locale;

public final class PrivacyPreferences {
    public static final String COLLECTION = "privacyOptOuts";
    public static final String BLOCKED_MESSAGE = "You've opted out of optional data processing, so this feature "
        + "isn't supported. Use `/privacy opt-in` to enable it again. Your privacy commands remain available.";
    private static final Set<Long> OPTED_OUT = ConcurrentHashMap.newKeySet();
    private static final Object SNAPSHOT_LOCK = new Object();
    private static volatile List<Long> excluded = List.of();
    private static final Set<String> ALLOWED_COMMANDS = Set.of("privacy", "help", "commands", "ping", "uptime",
        "botinfo");

    private PrivacyPreferences() {
    }

    public static void initialize(MongoDatabase database) {
        synchronized (SNAPSHOT_LOCK) {
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
            excluded = List.copyOf(OPTED_OUT);
        }
    }

    public static boolean isOptedOut(long userId) {
        return OPTED_OUT.contains(userId);
    }

    public static boolean isAllowedCommand(String name) {
        return ALLOWED_COMMANDS.contains(name.toLowerCase(Locale.ROOT));
    }

    public static List<Long> excludedUsers() {
        List<Long> snapshot = excluded;
        if (snapshot == null) {
            synchronized (SNAPSHOT_LOCK) {
                if (excluded == null) {
                    excluded = List.copyOf(OPTED_OUT);
                }
                snapshot = excluded;
            }
        }
        return snapshot;
    }

    public static void setOptedOut(MongoDatabase database, long userId, boolean optedOut) {
        if (userId <= 0)
            throw new IllegalArgumentException("A Discord user ID is required");

        exclusiveOperation(userId, () -> {
            if (optedOut) {
                updateSnapshot(userId, true);
                database.getCollection(COLLECTION).replaceOne(Filters.eq("_id", userId),
                    new Document("_id", userId), new ReplaceOptions().upsert(true));
            } else {
                RetentionActivity.trackExistingProfiles(userId);
                database.getCollection(COLLECTION).deleteMany(Filters.in("_id", userId, Long.toString(userId)));
                updateSnapshot(userId, false);
            }
            return null;
        });
    }

    /**
     * Coordinates an unbounded operation against account deletion. The operation must use raw database delegates,
     * without nested account operations. Prefer the account overloads whenever the owners are known.
     */
    public static <T> T optionalOperation(Supplier<T> operation) {
        return PrivacyCoordinator.broad(operation);
    }

    public static <T> T optionalOperation(long userId, Supplier<T> operation) {
        return PrivacyCoordinator.shared(userId, operation);
    }

    public static <T> T optionalOperation(Collection<Long> userIds, Supplier<T> operation) {
        return PrivacyCoordinator.shared(userIds, operation);
    }

    public static <T> T exclusiveOperation(long userId, Supplier<T> operation) {
        return PrivacyCoordinator.exclusive(userId, operation);
    }

    private static void updateSnapshot(long userId, boolean optedOut) {
        synchronized (SNAPSHOT_LOCK) {
            boolean changed = optedOut ? OPTED_OUT.add(userId) : OPTED_OUT.remove(userId);
            if (changed) {
                excluded = null;
            }
        }
    }

    public static void requireOptionalData(long userId) {
        if (isOptedOut(userId))
            throw new PrivacyOptOutException();
    }
}
