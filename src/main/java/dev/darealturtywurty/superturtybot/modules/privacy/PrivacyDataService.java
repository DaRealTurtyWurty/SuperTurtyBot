package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PrivacyDataService {
    private static final int MAX_RECORDS = 5_000;
    private static final int MAX_RAW_BYTES = 16 * 1024 * 1024;
    public static final int MAX_ATTACHMENT_BYTES = 7 * 1024 * 1024;

    private static final String EXPORT_README = """
        TurtyBot personal data export

        FILES
        - turtybot-data.json: your supported stored records and account-related summaries.
        - README.txt: this guide.

        READING YOUR DATA
        Open turtybot-data.json in a text editor or JSON viewer. The top-level fields are:
        - formatVersion: the export format version.
        - userId: the Discord account this export belongs to.
        - generatedAt: the time the export was prepared, in UTC.
        - scope and limitations: what the export covers and its exclusions.
        - records: data grouped by feature, across all servers where matching records exist.

        Groups may include preferences, birthdays, reminders, game progress, levelling,
        virtual economy records, saved content, moderation summaries, modmail messages
        you authored, dashboard profile information, and privacy requests.
        Groups ending in Participation contain only your part of shared records.
        An empty group means no matching records were exported for that category.
        Missing or null fields can represent absent or redacted information.

        Discord IDs identify accounts, servers, channels, roles, or messages.
        Many stored timestamps are Unix time in milliseconds. MongoDB dates may instead
        appear as an object containing a $date field. Virtual currency amounts describe
        activity within the bot, rather than real payments or bank balances.

        WHAT IS NOT INCLUDED
        This automated export excludes session identifiers, other people's private
        identities and message entries, and staff-only moderation text. Some shared
        records are represented by a summary rather than their full contents.
        Original image, audio, video, and other attachments are not bundled. Stored
        attachment links or references may be present; they may expire or require access.
        Live games, memory caches, Discord-hosted posts, Gists, server logs, and data
        retained by external providers are outside this automated database export.
        Records may change while the export is being prepared.

        ACCESS, CORRECTION, AND DELETION
        Email turtybot@gmail.com to request additional records, clarify an exclusion,
        correct information, or get help. Include your Discord user ID and, if relevant,
        the export's generatedAt time. Never send passwords or authentication tokens.
        Creating this export does not delete your data. Use /privacy delete to begin
        confirmed cleanup and operator review of remaining copies, or contact us by email.

        Keep this archive private: it can contain your personal information and messages.
        """;

    private static final Map<String, String> OWNED = Map.ofEntries(
        Map.entry("levelling", "user"), Map.entry("highlighters", "user"),
        Map.entry("userConfig", "user"), Map.entry("tags", "user"),
        Map.entry("quotes", "user"), Map.entry("starboard", "user"),
        Map.entry("suggestions", "user"), Map.entry("userEmbeds", "user"),
        Map.entry("reminders", "user"), Map.entry("wordleProfiles", "user"),
        Map.entry("economy", "user"), Map.entry("birthdays", "user"),
        Map.entry("userCollectables", "user"), Map.entry("twoThousandFortyEight", "user"),
        Map.entry("stickyMessages", "owner"), Map.entry("stickyRoles", "user"),
        Map.entry("marketplaceListings", "seller"), Map.entry("questPlayers", "user"),
        Map.entry("questActivities", "user"), Map.entry("warnings", "user"));
    private static final Set<String> OTHER_PERSON_FIELDS = Set.of(
        "targetId", "owner", "renter", "warner", "moderator", "reporter", "reported",
        "blockedBy", "closedBy", "responder", "addedBy");
    private static final List<String> MANUAL_REVIEW = List.of(
        "Discord posts, open modmail channels, Gists, downloaded files and provider-held copies",
        "Active temporary-ban schedules, modmail blocks and AI access restrictions",
        "Free-text references, server audit snapshots and any concurrent or in-progress activity");

    private final MongoDatabase database;

    public PrivacyDataService(MongoDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    public byte[] exportUser(long userId) throws IOException {
        var exported = new Document("formatVersion", 1)
            .append("userId", Long.toString(userId)).append("generatedAt", Instant.now().toString())
            .append("scope", "Structured database records and account-related summaries across all servers")
            .append("limitations", List.of(
                "Staff-only moderation text and other people's messages and identities are excluded.",
                "Shared-record summaries show only your participation. Session identifiers are never exported.",
                "Live games, memory caches, Discord posts, Gists, server logs and provider-held copies are not included.",
                "Email turtybot@gmail.com to request review of additional records."));
        var records = new Document();
        for (var entry : new TreeMap<>(OWNED).entrySet()) {
            records.put(entry.getKey(), read(entry.getKey(), id(entry.getValue(), userId)).stream()
                .map(document -> exportOwned(entry.getKey(), document, userId)).toList());
            checkSize(records);
        }

        records.put("reports", read("reports", Filters.or(id("reported", userId), id("reporter", userId)))
            .stream().map(document -> moderationSummary(document, userId)).toList());
        records.put("dashboardSessions", read("dashboardSessions", id("user.id", userId)).stream()
            .map(document -> select(document, "user", "createdAt", "expiresAt")).toList());
        records.put("modmailTickets", read("modmailTickets", id("user", userId)).stream()
            .map(document -> select(document, "guild", "user", "ticketNumber", "open", "openedAt", "closedAt",
                "openerMessage"))
            .toList());
        for (String collection : sharedCollections()) {
            List<Document> summaries = read(collection, sharedFilter(collection, userId)).stream()
                .map(document -> exportShared(collection, document, userId))
                .filter(document -> !document.isEmpty()).toList();
            records.put(collection + "Participation", summaries);
            checkSize(records);
        }

        records.put("activeTemporaryBans", read("tempBans", id("user", userId)).stream()
            .map(document -> select(document, "guild", "user", "createdAt", "expiresAt")).toList());
        records.put("modmailBlocks", read("modmailBlockedUsers", id("user", userId)).stream()
            .map(document -> select(document, "guild", "user", "blockedAt")).toList());
        records.put("privacyRequests", read("privacyRequests", id("user", userId)).stream()
            .map(document -> select(document, "_id", "createdAt", "status", "automaticCompletedAt", "manualReview"))
            .toList());
        exported.put("records", records);
        checkSize(exported);
        byte[] json = exported.toJson().getBytes(StandardCharsets.UTF_8);
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("turtybot-data.json"));
            zip.write(json);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("README.txt"));
            zip.write(EXPORT_README.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        if (output.size() > MAX_ATTACHMENT_BYTES)
            throw new ExportTooLargeException();

        return output.toByteArray();
    }

    public DeletionResult deleteUser(long userId, String requestId) {
        return deleteUser(userId, requestId, _ -> {
        });
    }

    public DeletionResult deleteUser(long userId, String requestId, LongConsumer runtimeCleanup) {
        var request = new Document("_id", requestId).append("user", userId)
            .append("createdAt", new Date()).append("status", "processing")
            .append("manualReview", MANUAL_REVIEW);
        database.getCollection("privacyRequests").insertOne(request);
        try {
            List<Document> references = new ArrayList<>();
            for (String name : List.of("starboard", "suggestions", "stickyMessages", "quotes", "modmailTickets")) {
                String userField = name.equals("stickyMessages") ? "owner" : "user";
                for (Document record : read(name, id(userField, userId))) {
                    references.add(select(record, "guild", "channel", "message", "starboardMessage", "postedMessage",
                        "ticketNumber").append("collection", name));
                }
            }

            collection("privacyRequests").updateOne(Filters.eq("_id", requestId),
                new Document("$set", new Document("externalReferences", references)));
        } catch (RuntimeException exception) {
            recordFailure(requestId, "external copy reference capture");
            throw exception;
        }

        List<String> failed = new ArrayList<>();
        long changed = 0;
        try {
            runtimeCleanup.accept(userId);
        } catch (RuntimeException exception) {
            failed.add("memory caches or scheduled reminders");
        }

        try {
            for (Document ticket : read("modmailTickets", id("user", userId))) {
                changed += collection("modmailTranscriptChunks").deleteMany(Filters.and(
                    Filters.eq("guild", ticket.get("guild")), Filters.eq("ticketChannel", ticket.get("channel"))))
                    .getDeletedCount();
            }

            changed += collection("modmailTickets").deleteMany(id("user", userId)).getDeletedCount();
        } catch (RuntimeException exception) {
            failed.add("modmailTickets");
        }

        for (var entry : new TreeMap<>(OWNED).entrySet()) {
            try {
                changed += collection(entry.getKey()).deleteMany(id(entry.getValue(), userId)).getDeletedCount();
            } catch (RuntimeException exception) {
                failed.add(entry.getKey());
            }
        }

        for (String name : List.of("reports", "dashboardSessions")) {
            try {
                Bson filter = name.equals("reports")
                    ? Filters.or(id("reported", userId), id("reporter", userId))
                    : id("user.id", userId);
                changed += collection(name).deleteMany(filter).getDeletedCount();
            } catch (RuntimeException exception) {
                failed.add(name);
            }
        }

        for (String name : sharedCollections()) {
            try {
                for (Document original : read(name, sharedFilter(name, userId))) {
                    changed += cleanSharedRecord(name, original, userId);
                }
            } catch (RuntimeException exception) {
                failed.add(name + " references");
            }
        }

        String status = failed.isEmpty() ? "manual_review" : "partial_failure";
        var update = new Document("status", status).append("failedSteps", failed)
            .append("automaticCompletedAt", new Date()).append("recordsChanged", changed);
        collection("privacyRequests").updateOne(Filters.eq("_id", requestId), new Document("$set", update));
        return new DeletionResult(requestId, changed, List.copyOf(failed));
    }

    public void recordFailure(String requestId, String step) {
        collection("privacyRequests").updateOne(Filters.eq("_id", requestId),
            new Document("$set", new Document("status", "partial_failure"))
                .append("$addToSet", new Document("failedSteps", step)));
    }

    private long cleanSharedRecord(String name, Document original, long userId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Document cleaned = cleanShared(name, original, userId);
            if (cleaned.equals(original))
                return 0;

            // Match the previous values as well as _id so a concurrent game or staff update is never overwritten
            if (collection(name).replaceOne(original, cleaned).getMatchedCount() == 1)
                return 1;

            original = collection(name).find(Filters.eq("_id", original.get("_id"))).first();
            if (original == null)
                return 0;
        }

        throw new IllegalStateException("Record changed during privacy cleanup");
    }

    private List<Document> read(String collection, Bson filter) {
        List<Document> documents = this.collection(collection).find(filter).maxTime(20, TimeUnit.SECONDS)
            .limit(MAX_RECORDS + 1).into(new ArrayList<>());
        if (documents.size() > MAX_RECORDS)
            throw new ExportTooLargeException();

        return documents;
    }

    private MongoCollection<Document> collection(String name) {
        return database.getCollection(name);
    }

    private static void checkSize(Document document) {
        if (document.toJson().getBytes(StandardCharsets.UTF_8).length > MAX_RAW_BYTES)
            throw new ExportTooLargeException();
    }

    private static Bson id(String field, long userId) {
        return Filters.in(field, userId, Long.toString(userId));
    }

    private static List<String> sharedCollections() {
        return List.of("counting", "guildData", "suggestions", "quotes", "warnings", "tempBans",
            "modmailBlockedUsers", "modmailTickets", "modmailTranscriptChunks", "economy");
    }

    private static Bson sharedFilter(String collection, long userId) {
        return switch (collection) {
            case "counting" -> Filters.or(id("users.user", userId), id("latestCounter", userId));
            case "guildData" -> Filters.or(id("enabledBirthdayUsers", userId),
                Filters.exists("endOfDayIncomeTax." + userId));
            case "suggestions" -> id("responses.responder", userId);
            case "quotes" -> id("addedBy", userId);
            case "warnings" -> id("warner", userId);
            case "tempBans" -> id("moderator", userId);
            case "modmailBlockedUsers" -> id("blockedBy", userId);
            case "modmailTickets" -> id("closedBy", userId);
            case "modmailTranscriptChunks" -> id("entries.authorId", userId);
            case "economy" -> Filters.or(id("transactions.targetId", userId), id("properties.renter", userId),
                id("properties.previousOwners", userId), id("properties.owner", userId));
            default -> throw new IllegalArgumentException("Unknown shared collection");
        };
    }

    private static Document exportOwned(String collection, Document original, long userId) {
        if (collection.equals("warnings"))
            return select(original, "guild", "user", "uuid", "warnedAt", "expiresAt")
                .append("notice", "Staff-only text and moderator identity require operator review.");

        if (collection.equals("questActivities"))
            return select(original, "guild", "user", "weekStart", "type", "occurredAt");

        Document exported = Document.parse(original.toJson());
        exported.remove("_id");
        exported.remove("responses");
        sanitizeIdentities(exported, userId);
        return exported;
    }

    private static void sanitizeIdentities(Map<String, Object> values, long userId) {
        for (var entry : values.entrySet()) {
            Object value = entry.getValue();
            if (OTHER_PERSON_FIELDS.contains(entry.getKey()) && value != null && !isUser(value, userId)) {
                entry.setValue(null);
            } else if (entry.getKey().equals("renterName")) {
                entry.setValue(null);
            } else if (entry.getKey().equals("previousOwners") && value instanceof List<?>) {
                entry.setValue(((List<?>) value).stream()
                    .filter(owner -> isUser(owner, userId))
                    .toList());
            } else if (value instanceof Document nested) {
                sanitizeIdentities(nested, userId);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Document nested) {
                        sanitizeIdentities(nested, userId);
                    }
                }
            }
        }
    }

    private static Document moderationSummary(Document original, long userId) {
        return select(original, "guild", "reportedAt").append("reportedByYou", isUser(original.get("reporter"), userId))
            .append("reportedUserIsYou", isUser(original.get("reported"), userId))
            .append("notice", "Report text and other identities require operator review.");
    }

    private static Document exportShared(String collection, Document original, long userId) {
        Document exported = select(original, "guild", "channel");
        switch (collection) {
            case "counting" -> exported.append("yourCounts", documents(original, "users").stream()
                .filter(user -> isUser(user.get("user"), userId)).toList());
            case "guildData" -> {
                exported.put("birthdayAnnouncementsEnabled",
                    containsUser(original.get("enabledBirthdayUsers"), userId));
                if (original.get("endOfDayIncomeTax") instanceof Document taxes) {
                    exported.put("yourPendingIncomeTax", taxes.get(Long.toString(userId)));
                }
            }
            case "modmailTranscriptChunks" -> {
                exported.put("ticketChannel", original.get("ticketChannel"));
                exported.put("yourMessages", documents(original, "entries").stream()
                    .filter(entry -> isUser(entry.get("authorId"), userId)).toList());
            }
            default ->
                exported.put("notice",
                    "Your account is referenced here; other people's records require operator review.");
        }
        return exported;
    }

    private static Document cleanShared(String collection, Document original, long userId) {
        Document cleaned = Document.parse(original.toJson());
        switch (collection) {
            case "counting" -> {
                removeDocuments(cleaned, "users", "user", userId);
                if (isUser(cleaned.get("latestCounter"), userId)) {
                    cleaned.put("latestCounter", 0L);
                }
            }
            case "guildData" -> {
                if (cleaned.get("enabledBirthdayUsers") instanceof List<?> users) {
                    users.removeIf(user -> isUser(user, userId));
                }

                if (cleaned.get("endOfDayIncomeTax") instanceof Document taxes) {
                    taxes.remove(Long.toString(userId));
                }
            }
            case "suggestions" -> removeDocuments(cleaned, "responses", "responder", userId);
            case "quotes" -> eraseAuthor(cleaned, "addedBy", null, userId);
            case "warnings" -> eraseAuthor(cleaned, "warner", "reason", userId);
            case "tempBans" -> eraseAuthor(cleaned, "moderator", "reason", userId);
            case "modmailBlockedUsers" -> eraseAuthor(cleaned, "blockedBy", "reason", userId);
            case "modmailTickets" -> eraseAuthor(cleaned, "closedBy", "closeReason", userId);
            case "modmailTranscriptChunks" -> removeDocuments(cleaned, "entries", "authorId", userId);
            case "economy" -> {
                for (Document transaction : documents(cleaned, "transactions")) {
                    if (isUser(transaction.get("targetId"), userId)) {
                        transaction.put("targetId", null);
                    }
                }

                for (Document property : documents(cleaned, "properties")) {
                    if (isUser(property.get("owner"), userId)) {
                        property.put("owner", -1L);
                    }
                    if (property.get("previousOwners") instanceof List<?> owners) {
                        owners.removeIf(owner -> isUser(owner, userId));
                    }

                    if (isUser(property.get("renter"), userId)) {
                        property.put("renter", -1L);
                        property.put("renterName", null);
                        property.put("rentEndsAt", 0L);
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unknown shared collection");
        }
        return cleaned;
    }

    private static void eraseAuthor(Document document, String field, String textField, long userId) {
        if (isUser(document.get(field), userId)) {
            document.put(field, 0L);
            if (textField != null) {
                document.put(textField, "[Removed at the author's request]");
            }
        }
    }

    private static void removeDocuments(Document document, String field, String userField, long userId) {
        if (document.get(field) instanceof List<?> list) {
            list.removeIf(value -> value instanceof Document entry
                && isUser(entry.get(userField), userId));
        }
    }

    private static List<Document> documents(Document document, String field) {
        if (!(document.get(field) instanceof List<?> list))
            return List.of();

        return list.stream().filter(Document.class::isInstance).map(Document.class::cast).toList();
    }

    private static boolean containsUser(Object value, long userId) {
        return value instanceof List<?> users && users.stream()
            .anyMatch(user -> isUser(user, userId));
    }

    private static boolean isUser(Object value, long userId) {
        return value != null && Long.toString(userId).equals(value.toString());
    }

    private static Document select(Document original, String... fields) {
        var selected = new Document();
        for (String field : fields) {
            if (original.containsKey(field)) {
                selected.put(field, original.get(field));
            }
        }

        return selected;
    }

    public record DeletionResult(String requestId, long recordsChanged, List<String> failedSteps) {
    }

    public static final class ExportTooLargeException extends RuntimeException {
        public ExportTooLargeException() {
            super("The export exceeds the safe attachment limit. Email turtybot@gmail.com for assistance.");
        }
    }
}
