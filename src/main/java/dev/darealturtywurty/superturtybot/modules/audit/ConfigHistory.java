package dev.darealturtywurty.superturtybot.modules.audit;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import dev.darealturtywurty.superturtybot.database.Database;
import org.bson.Document;

import java.util.*;

public final class ConfigHistory {
    private static final ThreadLocal<Actor> ACTOR = new ThreadLocal<>();
    private static final Set<String> RUNTIME_FIELDS = Set.of("_id", "guild", "endOfDayIncomeTax",
        "enabledBirthdayUsers");
    private static final Set<String> NOTIFIER_FIELDS = Set.of("channel", "discordChannel", "mention", "youtubeChannel",
        "subreddit", "appId");

    private ConfigHistory() {
    }

    public record Actor(String userId, String source, String action) {
    }
    public record Change(String field, String before, String after) {
    }
    public record Entry(
        String id,
        String actorId,
        String source,
        String action,
        String area,
        long createdAt,
        List<Change> changes
    ) {
    }
    public record Page(List<Entry> entries, int page, boolean hasMore) {
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public static Scope actor(long userId, String source, String action) {
        Actor previous = ACTOR.get();
        ACTOR.set(new Actor(Long.toString(userId), source, action));
        return () -> {
            if (previous == null) {
                ACTOR.remove();
            } else {
                ACTOR.set(previous);
            }
        };
    }

    public static void clearActor() {
        ACTOR.remove();
    }

    public static void setDashboardActor(long userId, String action) {
        ACTOR.set(new Actor(Long.toString(userId), "dashboard", action));
    }

    public static List<Change> diff(String area, Document before, Document after) {
        Document oldValues = project(area, before);
        Document newValues = project(area, after);
        List<Change> changes = new ArrayList<>();
        Set<String> fields = new TreeSet<>(oldValues.keySet());
        fields.addAll(newValues.keySet());
        for (String field : fields) {
            Object oldValue = oldValues.get(field);
            Object newValue = newValues.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new Change(field, json(oldValue), json(newValue)));
            }
        }
        return changes;
    }

    private static Document project(String area, Document record) {
        var result = new Document();
        if (record == null)
            return result;
        record.forEach((key, value) -> {
            if (auditsField(area, key)) {
                result.put(key, value);
            }
        });
        return result;
    }

    public static boolean auditsField(String area, String field) {
        String root = field.split("\\.", 2)[0];
        if (area.equals("guildData"))
            return !RUNTIME_FIELDS.contains(root);
        if (area.equals("counting"))
            return root.equals("channel") || root.equals("countingMode");
        return NOTIFIER_FIELDS.contains(root);
    }

    public static final class RecordingException extends IllegalStateException {
        public RecordingException(Throwable cause) {
            super(
                "Settings were saved, but change history could not be recorded. Ask the bot operator to check the database.",
                cause);
        }
    }

    private static String json(Object value) {
        String wrapped = new Document("value", value).toJson();
        return wrapped.substring(wrapped.indexOf(':') + 1, wrapped.length() - 1).trim();
    }

    public static void record(String area, Document before, Document after) {
        List<Change> changes = diff(area, before, after);
        if (changes.isEmpty())
            return;
        Document record = after == null ? before : after;
        if (!(record.get("guild") instanceof Number guild))
            return;
        Actor actor = ACTOR.get();
        List<Document> values = changes.stream().map(change -> new Document("field", change.field())
            .append("before", change.before()).append("after", change.after())).toList();
        collection().insertOne(new Document("guild", guild.longValue())
            .append("actorId", actor == null ? null : actor.userId())
            .append("source", actor == null ? "system" : actor.source())
            .append("action", actor == null ? "Automatic configuration update" : actor.action())
            .append("area", area).append("createdAt", new Date()).append("changes", values));
    }

    public static Page list(long guildId, int page) {
        int pageSize = 25;
        List<Document> records = collection().find(Filters.eq("guild", guildId))
            .sort(Sorts.orderBy(Sorts.descending("createdAt"), Sorts.descending("_id")))
            .skip((page - 1) * pageSize).limit(pageSize + 1).into(new ArrayList<>());
        boolean hasMore = records.size() > pageSize;
        return new Page(records.stream().limit(pageSize).map(record -> new Entry(
            record.getObjectId("_id").toHexString(), record.getString("actorId"), record.getString("source"),
            record.getString("action"), record.getString("area"), record.getDate("createdAt").getTime(),
            record.getList("changes", Document.class).stream().map(change -> new Change(change.getString("field"),
                change.getString("before"), change.getString("after"))).toList()))
            .toList(), page, hasMore);
    }

    private static MongoCollection<Document> collection() {
        return Database.getDatabase().mongoDatabase.getCollection("configHistory");
    }
}
