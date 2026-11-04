package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class DataRetentionService {
    private final MongoDatabase database;
    private final ManagedTemporaryFiles temporaryFiles;
    private final Clock clock;

    public DataRetentionService(MongoDatabase database, ManagedTemporaryFiles temporaryFiles, Clock clock) {
        this.database = database;
        this.temporaryFiles = temporaryFiles;
        this.clock = clock;
    }

    public CleanupReport run() {
        Instant now = clock.instant();
        Map<String, Long> removed = new LinkedHashMap<>();
        List<String> failed = new ArrayList<>();
        step("inactive profiles", () -> expireProfiles(now), removed, failed);
        step("closed modmail", () -> expireModmail(now), removed, failed);
        step("orphaned transcripts", () -> expireOrphanedTranscripts(now), removed, failed);
        step("expired warnings", () -> expireWarnings(now), removed, failed);
        step("resolved reports", () -> expireReports(now), removed, failed);
        step("resolved privacy requests", () -> expireRequests(now), removed, failed);
        step("expired sessions", () -> database.getCollection("dashboardSessions")
            .deleteMany(Filters.lte("expiresAt", Date.from(now))).getDeletedCount(), removed, failed);
        step("sticky roles", () -> database.getCollection("stickyRoles")
            .deleteMany(Filters.and(Filters.gt("savedAt", 0L), Filters.lte("savedAt",
                now.atZone(ZoneOffset.UTC).minusMonths(6).toInstant().toEpochMilli())))
            .getDeletedCount(), removed, failed);
        step("abandoned temporary files", () -> {
            try {
                return temporaryFiles.cleanup(now);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }, removed, failed);
        return new CleanupReport(Map.copyOf(removed), List.copyOf(failed));
    }

    private static void step(String name, LongSupplier operation, Map<String, Long> removed, List<String> failed) {
        try {
            removed.put(name, operation.getAsLong());
        } catch (RuntimeException exception) {
            failed.add(name);
            Constants.LOGGER.warn("Daily retention step {} failed ({}); it will retry next run", name,
                exception.getClass().getSimpleName());
        }
    }

    private long expireProfiles(Instant now) {
        Instant cutoff = profileCutoff(now);
        long changed = 0;
        RuntimeException failure = null;
        for (long userId : RetentionActivity.backfillProfiles()) {
            try {
                changed += RetentionActivity.expire(userId, cutoff);
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }

        if (failure != null)
            throw failure;

        return changed;
    }

    public static Instant profileCutoff(Instant now) {
        return now.atZone(ZoneOffset.UTC).minusMonths(12).minusHours(1).toInstant();
    }

    private long expireModmail(Instant now) {
        long changed = 0;
        var tickets = database.getCollection("modmailTickets");
        for (Document ticket : tickets.find(Filters.eq("open", false)).maxTime(20, TimeUnit.SECONDS)) {
            Instant closed = timestamp(ticket.get("closedAt"));
            if (closed == null) {
                closed = timestamp(ticket.get("retentionClosedAt"));
                if (closed == null) {
                    tickets.updateOne(ticketIdentity(ticket), new Document("$set",
                        new Document("retentionClosedAt", Date.from(now))));
                    continue;
                }
            }

            if (closed.isAfter(now.minus(Duration.ofDays(90))))
                continue;

            Bson identity = ticketIdentity(ticket);
            if (tickets.find(identity).first() == null)
                continue;

            changed += database.getCollection("modmailTranscriptChunks").deleteMany(Filters.and(
                Filters.eq("guild", ticket.get("guild")), Filters.eq("ticketChannel", ticket.get("channel"))))
                .getDeletedCount();
            changed += tickets.deleteOne(identity).getDeletedCount();
        }

        return changed;
    }

    private static Bson ticketIdentity(Document ticket) {
        return Filters.and(
            Filters.eq("_id", ticket.get("_id")),
            Filters.eq("open", false),
            Filters.eq("closedAt", ticket.get("closedAt")),
            Filters.eq("retentionClosedAt", ticket.get("retentionClosedAt")));
    }

    private long expireOrphanedTranscripts(Instant now) {
        long changed = 0;
        Map<String, Boolean> tickets = new HashMap<>();
        var chunks = database.getCollection("modmailTranscriptChunks");
        for (Document chunk : chunks.find().projection(Projections.include("_id", "guild", "ticketChannel",
            "retentionOrphanedAt")).maxTime(20, TimeUnit.SECONDS)) {
            String key = chunk.get("guild") + ":" + chunk.get("ticketChannel");
            boolean exists = tickets.computeIfAbsent(key, _ -> database.getCollection("modmailTickets")
                .find(Filters.and(Filters.eq("guild", chunk.get("guild")),
                    Filters.eq("channel", chunk.get("ticketChannel"))))
                .first() != null);
            if (exists)
                continue;

            Instant orphanedAt = timestamp(chunk.get("retentionOrphanedAt"));
            if (orphanedAt == null) {
                chunks.updateOne(Filters.and(Filters.eq("_id", chunk.get("_id")),
                    Filters.exists("retentionOrphanedAt", false)),
                    new Document("$set",
                        new Document("retentionOrphanedAt", Date.from(now))));
            } else if (!orphanedAt.isAfter(now.minus(Duration.ofDays(90)))) {
                changed += chunks.deleteOne(Filters.and(Filters.eq("_id", chunk.get("_id")),
                    Filters.eq("retentionOrphanedAt", chunk.get("retentionOrphanedAt")))).getDeletedCount();
            }
        }

        return changed;
    }

    private long expireWarnings(Instant now) {
        long cutoff = now.minus(Duration.ofDays(90)).toEpochMilli();
        return database.getCollection("warnings").deleteMany(Filters.and(
            Filters.gt("expiresAt", 0L), Filters.lte("expiresAt", cutoff))).getDeletedCount();
    }

    private long expireReports(Instant now) {
        long changed = 0;
        MongoCollection<Document> reports = database.getCollection("reports");
        for (Document report : reports.find(Filters.or(Filters.eq("status", "resolved"),
            Filters.and(Filters.exists("status", false), Filters.eq("resolved", true))))
            .maxTime(20, TimeUnit.SECONDS)) {
            Bson identity = Filters.and(
                Filters.eq("_id", report.get("_id")),
                Filters.eq("status", report.get("status")),
                Filters.eq("resolved", report.get("resolved")),
                Filters.eq("resolvedAt", report.get("resolvedAt")),
                Filters.eq("retentionResolvedAt", report.get("retentionResolvedAt")));
            Instant resolved = timestamp(report.get("resolvedAt"));
            if (resolved == null) {
                resolved = timestamp(report.get("retentionResolvedAt"));
            }

            if (resolved == null) {
                reports.updateOne(identity, new Document("$set", new Document("retentionResolvedAt", Date.from(now))));
            } else if (!resolved.isAfter(now.minus(Duration.ofDays(90)))) {
                changed += reports.deleteOne(identity).getDeletedCount();
            }
        }

        return changed;
    }

    private long expireRequests(Instant now) {
        long changed = 0;
        var requests = database.getCollection("privacyRequests");
        requests.updateMany(Filters.and(
            Filters.ne("status", "resolved"),
            Filters.exists("deleteAfter")),
            new Document("$unset", new Document("deleteAfter", "")));
        for (Document request : requests.find(Filters.eq("status", "resolved"))
            .maxTime(20, TimeUnit.SECONDS)) {
            Instant resolved = timestamp(request.get("resolvedAt"));
            if (resolved == null || resolved.isAfter(now)) {
                resolved = now;
            }

            Instant deadline = resolved.plus(Duration.ofDays(30));
            Instant earlier = timestamp(request.get("deleteAfter"));
            if (earlier != null && earlier.isBefore(deadline)) {
                deadline = earlier;
            }

            var compact = new Document("_id", request.get("_id")).append("status", "resolved")
                .append("resolvedAt", Date.from(resolved)).append("deleteAfter", Date.from(deadline));
            for (String date : List.of("createdAt", "automaticCompletedAt")) {
                Instant value = timestamp(request.get(date));
                if (value != null) {
                    compact.put(date, Date.from(value));
                }
            }

            for (String count : List.of("recordsChanged", "gistsDeleted")) {
                if (request.get(count) instanceof Number number) {
                    compact.put(count, number);
                }
            }

            if (requests.replaceOne(request, compact).getMatchedCount() == 1 && !deadline.isAfter(now)) {
                changed += requests.deleteOne(compact).getDeletedCount();
            }
        }

        return changed;
    }

    private static Instant timestamp(Object value) {
        if (value instanceof Date date && date.getTime() > 0)
            return date.toInstant();

        if (value instanceof Number number && number.longValue() > 0)
            return Instant.ofEpochMilli(number.longValue());

        return null;
    }

    public record CleanupReport(Map<String, Long> removed, List<String> failedSteps) {
    }
}
