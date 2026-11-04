package dev.darealturtywurty.superturtybot.modules.privacy;

import com.google.common.util.concurrent.Striped;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.GistManager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.service.GistService;
import org.eclipse.egit.github.core.service.UserService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

public final class TrackedGistService {
    public static final String COLLECTION = "gistUploads";
    private static final Striped<Lock> DELETIONS = Striped.lock(64);
    private final MongoCollection<Document> uploads;
    private final Supplier<GistService> github;
    private final ToLongFunction<GistService> accountId;

    public TrackedGistService(MongoDatabase database) {
        this(database, GistManager::getGistService, TrackedGistService::authenticatedAccount);
    }

    public TrackedGistService(
        MongoDatabase database,
        Supplier<GistService> github,
        ToLongFunction<GistService> accountId
    ) {
        this.uploads = database.getCollection(COLLECTION);
        this.github = github;
        this.accountId = accountId;
    }

    public Gist create(
        long author,
        long requester,
        long guild,
        long channel,
        long message,
        Map<String, GistFile> files
    ) {
        return PrivacyPreferences.optionalOperation(() -> {
            PrivacyPreferences.requireOptionalData(author);
            PrivacyPreferences.requireOptionalData(requester);
            GistService client = github.get();
            long githubAccount = accountId.applyAsLong(client);
            if (githubAccount <= 0)
                throw new IllegalStateException("Cannot identify the GitHub account");

            String trackingId = UUID.randomUUID().toString();
            Document record = new Document("_id", trackingId).append("author", author).append("requester", requester)
                .append("guild", guild).append("channel", channel).append("message", message)
                .append("githubAccount", githubAccount).append("createdAt", new Date()).append("state", "creating");
            if (!uploads.insertOne(record).wasAcknowledged())
                throw new IllegalStateException("Gist tracking was not acknowledged");

            Gist gist;
            try {
                gist = client.createGist(new Gist().setPublic(false).setFiles(files)
                    .setDescription("TurtyBot upload " + trackingId));
            } catch (IOException | RuntimeException exception) {
                markFailure(trackingId, "creation_uncertain");
                throw new IllegalStateException("Gist creation needs review", exception);
            }

            if (gist == null || gist.getId() == null || gist.getId().isBlank()) {
                markFailure(trackingId, "creation_uncertain");
                throw new IllegalStateException("GitHub did not return a Gist identifier");
            }

            try {
                UpdateResult updated = uploads.updateOne(Filters.eq("_id", trackingId),
                    new Document("$set", new Document("gistId", gist.getId()).append("state", "active")));
                if (!updated.wasAcknowledged() || updated.getMatchedCount() != 1)
                    throw new IllegalStateException("The Gist tracking record was not saved");
            } catch (RuntimeException exception) {
                boolean deletionConfirmed = false;
                try {
                    removeRemote(client, gist.getId());
                    deletionConfirmed = true;
                    if (!uploads.deleteOne(Filters.eq("_id", trackingId)).wasAcknowledged())
                        throw new IllegalStateException("Gist tracking removal was not acknowledged");
                } catch (IOException | RuntimeException cleanupFailure) {
                    try {
                        uploads.updateOne(Filters.eq("_id", trackingId), new Document("$set",
                            new Document("gistId", gist.getId()).append("state", "deletion_failed")
                                .append("deletionConfirmed", deletionConfirmed)));
                    } catch (RuntimeException _) {
                        // The original intent and its public recovery marker remain available for manual review
                    }

                    Constants.LOGGER.warn("Gist tracking {} requires operator review", trackingId);
                }

                throw new IllegalStateException("Gist tracking could not finish", exception);
            }

            return gist;
        });
    }

    public CleanupResult deleteUser(long userId) {
        List<Document> records = uploads.find(userFilter(userId)).maxTime(20, TimeUnit.SECONDS)
            .into(new ArrayList<>());
        if (records.isEmpty())
            return new CleanupResult(0, 0, List.of());

        long removed = 0;
        long deleted = 0;
        List<String> failed = new ArrayList<>();
        GistService client;
        long currentAccount;
        try {
            client = github.get();
            currentAccount = accountId.applyAsLong(client);
            if (currentAccount <= 0)
                throw new IllegalStateException("Cannot identify the GitHub account");
        } catch (RuntimeException exception) {
            for (Document record : records) {
                String trackingId = record.getString("_id");
                markFailure(trackingId, "github_unavailable");
                failed.add(trackingId);
            }

            return new CleanupResult(0, 0, List.copyOf(failed));
        }
        for (Document snapshot : records) {
            String trackingId = snapshot.getString("_id");
            Lock lock = DELETIONS.get(trackingId);
            lock.lock();
            try {
                Document record = uploads.find(Filters.eq("_id", trackingId)).maxTime(20, TimeUnit.SECONDS).first();
                if (record == null)
                    continue;
                if (!(record.get("githubAccount") instanceof Number owner) || owner.longValue() != currentAccount) {
                    markFailure(trackingId, "github_account_mismatch");
                    failed.add(trackingId);
                    continue;
                }
                String gistId = record.getString("gistId");
                if (gistId == null || gistId.isBlank()) {
                    markFailure(trackingId, "creation_uncertain");
                    failed.add(trackingId);
                    continue;
                }
                if (!Boolean.TRUE.equals(record.get("deletionConfirmed"))) {
                    removeRemote(client, gistId);
                    deleted++;
                    UpdateResult confirmation = uploads.updateOne(Filters.eq("_id", trackingId), new Document("$set",
                        new Document("deletionConfirmed", true)));
                    if (!confirmation.wasAcknowledged() || confirmation.getMatchedCount() != 1)
                        throw new IllegalStateException("Gist removal confirmation was not saved");
                }

                DeleteResult result = uploads.deleteOne(Filters.eq("_id", trackingId));
                if (!result.wasAcknowledged())
                    throw new IllegalStateException("Gist tracking removal was not acknowledged");

                removed += result.getDeletedCount();
            } catch (IOException | RuntimeException exception) {
                markFailure(trackingId, "deletion_failed");
                failed.add(trackingId);
            } finally {
                lock.unlock();
            }
        }

        return new CleanupResult(removed, deleted, List.copyOf(failed));
    }

    public static Bson userFilter(long userId) {
        return Filters.or(Filters.in("author", userId, Long.toString(userId)),
            Filters.in("requester", userId, Long.toString(userId)));
    }

    private void markFailure(String trackingId, String state) {
        try {
            uploads.updateOne(Filters.eq("_id", trackingId), new Document("$set", new Document("state", state)));
        } catch (RuntimeException exception) {
            Constants.LOGGER.warn("Could not update Gist tracking {} for operator review", trackingId);
        }
    }

    private static long authenticatedAccount(GistService client) {
        try {
            return new UserService(client.getClient()).getUser().getId();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void removeRemote(GistService client, String gistId) throws IOException {
        // A 404 can also hide a private Gist when credentials lose access. Retain the receipt for review
        client.deleteGist(gistId);
    }

    public record CleanupResult(long recordsRemoved, long gistsDeleted, List<String> failedUploads) {
    }
}
