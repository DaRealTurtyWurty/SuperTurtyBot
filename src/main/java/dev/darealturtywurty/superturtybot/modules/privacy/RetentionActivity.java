package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class RetentionActivity {
    public static final String COLLECTION = "privacyActivity";
    public static final Map<String, String> PROFILE_FIELDS = Map.ofEntries(
        Map.entry("levelling", "user"), Map.entry("userConfig", "user"),
        Map.entry("wordleProfiles", "user"), Map.entry("economy", "user"),
        Map.entry("userCollectables", "user"), Map.entry("twoThousandFortyEight", "user"),
        Map.entry("birthdays", "user"), Map.entry("userEmbeds", "user"), Map.entry("highlighters", "user"),
        Map.entry("tags", "user"), Map.entry("quotes", "user"), Map.entry("suggestions", "user"),
        Map.entry("starboard", "user"), Map.entry("stickyMessages", "owner"),
        Map.entry("marketplaceListings", "seller"), Map.entry("questPlayers", "user"),
        Map.entry("questActivities", "user"));
    private static final ReentrantReadWriteLock MUTATIONS = new ReentrantReadWriteLock(true);
    private static final Map<Long, Long> LAST_OBSERVED = new ConcurrentHashMap<>();
    private static final Map<Long, Long> LAST_SAVED = new ConcurrentHashMap<>();
    private static final Set<Long> EXPIRED = ConcurrentHashMap.newKeySet();
    private static volatile MongoDatabase database;
    private static Clock clock = Clock.systemUTC();
    private static Instant rollout;

    private RetentionActivity() {
    }

    public static void initialize(MongoDatabase database) {
        initialize(database, Clock.systemUTC());
    }

    public static void initialize(MongoDatabase source, Clock sourceClock) {
        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            var configuration = source.getCollection("privacyRetentionConfig");
            configuration.updateOne(Filters.eq("_id", "profiles"),
                new Document("$setOnInsert", new Document("startedAt", new Date(sourceClock.millis()))),
                new UpdateOptions().upsert(true));
            Document settings = configuration.find(Filters.eq("_id", "profiles")).first();
            if (settings == null || !(settings.get("startedAt") instanceof Date startedAt))
                throw new IllegalStateException("Cannot determine the retention rollout date");
            Map<Long, Long> loaded = new ConcurrentHashMap<>();
            try (var cursor = source.getCollection(COLLECTION).find().iterator()) {
                while (cursor.hasNext()) {
                    Document activity = cursor.next();
                    long user = Long.parseLong(activity.get("_id").toString());
                    if (user <= 0 || !(activity.get("lastActiveAt") instanceof Date date))
                        throw new IllegalStateException("Invalid retention activity record");
                    loaded.merge(user, date.getTime(), Math::max);
                }
            }
            clock = sourceClock;
            rollout = startedAt.toInstant();
            LAST_OBSERVED.clear();
            LAST_OBSERVED.putAll(loaded);
            LAST_SAVED.clear();
            LAST_SAVED.putAll(loaded);
            EXPIRED.clear();
            database = source;
        } finally {
            lock.unlock();
        }
    }

    public static <T> T mutation(Supplier<T> operation) {
        var lock = MUTATIONS.readLock();
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public static void register(MongoCollection<?> collection, long userId) {
        if (database == null || userId <= 0 || !isProfileCollection(collection))
            return;
        if (EXPIRED.contains(userId))
            throw new RetentionExpiredException();
        enroll(userId);
    }

    public static boolean isProfileCollection(MongoCollection<?> collection) {
        return collection.getNamespace() != null
            && PROFILE_FIELDS.containsKey(collection.getNamespace().getCollectionName());
    }

    public static boolean isExpired(long userId) {
        return EXPIRED.contains(userId);
    }

    private static void enroll(long userId) {
        if (PrivacyPreferences.isOptedOut(userId))
            return;
        LAST_OBSERVED.computeIfAbsent(userId, id -> {
            long now = clock.millis();
            var result = database.getCollection(COLLECTION).updateOne(Filters.eq("_id", id),
                new Document("$setOnInsert", new Document("lastActiveAt", new Date(now))),
                new UpdateOptions().upsert(true));
            if (!result.wasAcknowledged())
                throw new IllegalStateException("Retention activity was not saved");
            LAST_SAVED.put(id, now);
            return now;
        });
    }

    public static void touch(long userId) {
        if (database == null || userId <= 0 || PrivacyPreferences.isOptedOut(userId))
            return;
        mutation(() -> {
            EXPIRED.remove(userId);
            long now = clock.millis();
            LAST_OBSERVED.computeIfPresent(userId, (id, previous) -> Math.max(previous, now));
            if (LAST_OBSERVED.containsKey(userId)
                && now - LAST_SAVED.getOrDefault(userId, 0L) >= TimeUnit.HOURS.toMillis(1)) {
                var result = database.getCollection(COLLECTION).updateOne(Filters.eq("_id", userId),
                    new Document("$max", new Document("lastActiveAt", new Date(now))),
                    new UpdateOptions().upsert(true));
                if (!result.wasAcknowledged())
                    throw new IllegalStateException("Retention activity was not saved");
                LAST_SAVED.put(userId, now);
            }
            return null;
        });
    }

    public static Set<Long> backfillProfiles() {
        Set<Long> observed = new HashSet<>();
        for (var entry : PROFILE_FIELDS.entrySet()) {
            try (var cursor = database.getCollection(entry.getKey()).find()
                .projection(Projections.include(entry.getValue())).iterator()) {
                while (cursor.hasNext()) {
                    Object owner = cursor.next().get(entry.getValue());
                    if (owner == null)
                        continue;
                    long userId;
                    try {
                        userId = Long.parseLong(owner.toString());
                    } catch (NumberFormatException _) {
                        continue;
                    }
                    if (userId <= 0)
                        continue;
                    observed.add(userId);
                    PrivacyPreferences.optionalOperation(() -> mutation(() -> {
                        enroll(userId);
                        return null;
                    }));
                }
            }
        }
        observed.addAll(LAST_OBSERVED.keySet());
        return observed;
    }

    public static long expire(long userId, Instant cutoff) {
        return PrivacyPreferences.exclusiveOperation(() -> expireProfile(userId, cutoff));
    }

    private static long expireProfile(long userId, Instant cutoff) {
        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            Long observed = LAST_OBSERVED.get(userId);
            if (observed == null) {
                if (!PrivacyPreferences.isOptedOut(userId))
                    return 0;
                observed = rollout.toEpochMilli();
            }
            if (observed > cutoff.toEpochMilli())
                return 0;
            EXPIRED.add(userId);
            long changed = 0;
            for (var entry : PROFILE_FIELDS.entrySet()) {
                changed += database.getCollection(entry.getKey()).deleteMany(
                    Filters.in(entry.getValue(), userId, Long.toString(userId))).getDeletedCount();
            }
            changed += database.getCollection(COLLECTION).deleteMany(
                Filters.in("_id", userId, Long.toString(userId))).getDeletedCount();
            LAST_OBSERVED.remove(userId);
            LAST_SAVED.remove(userId);
            return changed;
        } finally {
            lock.unlock();
        }
    }

    public static Set<Long> excludedUsers() {
        return Set.copyOf(EXPIRED);
    }

    public static void forgetUser(long userId) {
        var lock = MUTATIONS.writeLock();
        lock.lock();
        try {
            if (database != null) {
                database.getCollection(COLLECTION).deleteMany(Filters.in("_id", userId, Long.toString(userId)));
            }
            LAST_OBSERVED.remove(userId);
            LAST_SAVED.remove(userId);
            EXPIRED.add(userId);
        } finally {
            lock.unlock();
        }
    }
}
