package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private static final Map<Long, Long> LAST_OBSERVED = new ConcurrentHashMap<>();
    private static final Map<Long, Long> LAST_SAVED = new ConcurrentHashMap<>();
    private static final Set<Long> EXPIRED = ConcurrentHashMap.newKeySet();
    private static volatile Set<Long> excluded = Set.of();
    private static final Object EXCLUSION_LOCK = new Object();
    private static final int BACKFILL_BATCH_SIZE = 500;
    private static volatile MongoDatabase database;
    private static Clock clock = Clock.systemUTC();
    private static Instant rollout;

    private RetentionActivity() {
    }

    public static void initialize(MongoDatabase database) {
        initialize(database, Clock.systemUTC());
    }

    public static void initialize(MongoDatabase source, Clock sourceClock) {
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
        excluded = Set.of();
        database = source;
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
        enrollProfile(userId);
    }

    public static void trackExistingProfiles(long userId) {
        if (database == null || LAST_OBSERVED.containsKey(userId))
            return;
        for (var profile : PROFILE_FIELDS.entrySet()) {
            if (database.getCollection(profile.getKey())
                .find(Filters.in(profile.getValue(), userId, Long.toString(userId)))
                .projection(Projections.include("_id")).first() != null) {
                enrollProfile(userId);
                return;
            }
        }
    }

    private static void enrollProfile(long userId) {
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
        PrivacyPreferences.optionalOperation(userId, () -> {
            if (PrivacyPreferences.isOptedOut(userId))
                return null;
            setExpired(userId, false);
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

    public static long backfillProfiles() {
        long scanned = 0;
        var configuration = database.getCollection("privacyRetentionConfig");
        for (var entry : new TreeMap<>(PROFILE_FIELDS).entrySet()) {
            String checkpointId = "profile-backfill-v1:" + entry.getKey();
            Document checkpoint = configuration.find(Filters.eq("_id", checkpointId)).first();
            if (checkpoint != null && Boolean.TRUE.equals(checkpoint.getBoolean("complete")))
                continue;
            Object after = checkpoint == null ? null : checkpoint.get("lastId");
            // $expr compares BSON types in the same order as the sort, including mixed legacy ID types.
            var filter = after == null
                ? new Document()
                : Filters.expr(new Document("$gt",
                    List.of("$_id", new Document("$literal", after))));
            Object lastId = after;
            int pending = 0;
            try (var cursor = database.getCollection(entry.getKey()).find(filter)
                .sort(Sorts.ascending("_id")).projection(Projections.include(entry.getValue()))
                .batchSize(BACKFILL_BATCH_SIZE).maxTime(20, TimeUnit.SECONDS).iterator()) {
                while (cursor.hasNext()) {
                    Document record = cursor.next();
                    Object owner = record.get(entry.getValue());
                    if (owner != null) {
                        try {
                            long userId = Long.parseLong(owner.toString());
                            if (userId > 0) {
                                PrivacyPreferences.optionalOperation(userId, () -> {
                                    if (!EXPIRED.contains(userId)) {
                                        enroll(userId);
                                    }
                                    return null;
                                });
                            }
                        } catch (NumberFormatException _) {
                        }
                    }
                    lastId = record.get("_id");
                    scanned++;
                    if (++pending == BACKFILL_BATCH_SIZE) {
                        saveCheckpoint(checkpointId, lastId, false);
                        pending = 0;
                    }
                }
            }
            saveCheckpoint(checkpointId, lastId, true);
        }
        return scanned;
    }

    private static void saveCheckpoint(String id, Object lastId, boolean complete) {
        var checkpoint = new Document("complete", complete);
        if (lastId != null) {
            checkpoint.append("lastId", lastId);
        }
        var update = new Document("$set", checkpoint);
        if (complete) {
            checkpoint.remove("lastId");
            update.append("$unset", new Document("lastId", ""));
        }
        if (!database.getCollection("privacyRetentionConfig").updateOne(Filters.eq("_id", id),
            update, new UpdateOptions().upsert(true)).wasAcknowledged())
            throw new IllegalStateException("Profile migration progress was not saved");
    }

    public static Set<Long> expiryCandidates(Instant cutoff) {
        Set<Long> candidates = new HashSet<>();
        try (var cursor = database.getCollection(COLLECTION)
            .find(Filters.lte("lastActiveAt", Date.from(cutoff))).projection(Projections.include("_id"))
            .batchSize(BACKFILL_BATCH_SIZE).maxTime(20, TimeUnit.SECONDS).iterator()) {
            while (cursor.hasNext()) {
                candidates.add(Long.parseLong(cursor.next().get("_id").toString()));
            }
        }
        if (!rollout.isAfter(cutoff)) {
            List<Object> untracked = new ArrayList<>();
            for (long user : PrivacyPreferences.excludedUsers()) {
                if (!LAST_OBSERVED.containsKey(user)) {
                    untracked.add(user);
                    untracked.add(Long.toString(user));
                    if (untracked.size() == BACKFILL_BATCH_SIZE * 2) {
                        findLegacyOptOuts(untracked, candidates);
                        untracked.clear();
                    }
                }
            }
            if (!untracked.isEmpty()) {
                findLegacyOptOuts(untracked, candidates);
            }
        }
        candidates.removeIf(user -> !isDue(user, cutoff));
        return candidates;
    }

    private static void findLegacyOptOuts(List<Object> users, Set<Long> candidates) {
        for (var profile : PROFILE_FIELDS.entrySet()) {
            try (var cursor = database.getCollection(profile.getKey()).find(Filters.in(profile.getValue(), users))
                .projection(Projections.include(profile.getValue())).maxTime(20, TimeUnit.SECONDS).iterator()) {
                while (cursor.hasNext()) {
                    candidates.add(Long.parseLong(cursor.next().get(profile.getValue()).toString()));
                }
            }
        }
    }

    public static long expire(long userId, Instant cutoff) {
        if (!isDue(userId, cutoff))
            return 0;
        return PrivacyPreferences.exclusiveOperation(userId, () -> expireProfile(userId, cutoff));
    }

    private static boolean isDue(long userId, Instant cutoff) {
        Long observed = LAST_OBSERVED.get(userId);
        if (observed == null) {
            if (!PrivacyPreferences.isOptedOut(userId))
                return false;
            observed = rollout.toEpochMilli();
        }
        return observed <= cutoff.toEpochMilli();
    }

    private static long expireProfile(long userId, Instant cutoff) {
        if (!isDue(userId, cutoff))
            return 0;
        setExpired(userId, true);
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
    }

    public static Set<Long> excludedUsers() {
        Set<Long> snapshot = excluded;
        if (snapshot == null) {
            synchronized (EXCLUSION_LOCK) {
                if (excluded == null) {
                    excluded = Set.copyOf(EXPIRED);
                }
                snapshot = excluded;
            }
        }
        return snapshot;
    }

    public static void forgetUser(long userId) {
        PrivacyPreferences.exclusiveOperation(userId, () -> {
            if (database != null) {
                database.getCollection(COLLECTION).deleteMany(Filters.in("_id", userId, Long.toString(userId)));
            }
            LAST_OBSERVED.remove(userId);
            LAST_SAVED.remove(userId);
            setExpired(userId, true);
            return null;
        });
    }

    private static void setExpired(long userId, boolean expired) {
        if (EXPIRED.contains(userId) == expired)
            return;
        synchronized (EXCLUSION_LOCK) {
            boolean changed = expired ? EXPIRED.add(userId) : EXPIRED.remove(userId);
            if (changed) {
                excluded = null;
            }
        }
    }
}
