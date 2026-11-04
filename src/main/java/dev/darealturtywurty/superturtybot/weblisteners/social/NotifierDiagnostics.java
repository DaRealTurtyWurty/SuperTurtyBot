package dev.darealturtywurty.superturtybot.weblisteners.social;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.bson.Document;

import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import com.google.gson.JsonParseException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;

public final class NotifierDiagnostics {
    private NotifierDiagnostics() {
    }

    public record Key(String type, long guild, String target, long channel) {
        public String id() {
            return type + ":" + guild + ":" + channel + ":" + target;
        }
    }

    public record Status(
        Long lastCheckAt,
        Long lastSuccessfulCheckAt,
        String checkError,
        Long checkErrorAt,
        Long lastDeliveredAt,
        String lastDeliveredItem,
        String deliveryError,
        Long deliveryErrorAt,
        Long lastTestAt,
        String testError,
        String channelProblem,
        boolean eventDriven
    ) {
    }

    public static Key key(String type, long guild, String target, long channel) {
        String normalized = target == null ? "" : target.trim();
        if (type.equals("twitch") || type.equals("reddit")) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return new Key(type, guild, normalized, channel);
    }

    public static void checking(Key key) {
        update(key, new Document("lastCheckAt", System.currentTimeMillis()));
    }

    public static void checked(Key key) {
        update(key, new Document("lastSuccessfulCheckAt", System.currentTimeMillis())
            .append("checkError", null).append("checkErrorAt", null));
    }

    public static void checkFailed(Key key, String reason) {
        update(key, new Document("checkError", reason).append("checkErrorAt", System.currentTimeMillis()));
    }

    public static void delivered(Key key, String item) {
        update(key, new Document("lastDeliveredAt", System.currentTimeMillis())
            .append("lastDeliveredItem", item).append("deliveryError", null).append("deliveryErrorAt", null));
    }

    public static void deliveryFailed(Key key, String reason) {
        update(key, new Document("deliveryError", reason).append("deliveryErrorAt", System.currentTimeMillis()));
    }

    public static void tested(Key key, String error) {
        update(key, new Document("lastTestAt", System.currentTimeMillis()).append("testError", error));
    }

    public static String failureReason(Throwable failure) {
        var httpStatus = Pattern.compile("\\bHTTP(?:\\s+status(?:\\s+code)?\\s*:?)?\\s+(\\d{3})\\b",
            Pattern.CASE_INSENSITIVE);
        for (int depth = 0; failure != null && depth < 10; depth++, failure = failure.getCause()) {
            var match = httpStatus.matcher(failure.getMessage() == null ? "" : failure.getMessage());
            if (match.find())
                return "The provider returned HTTP " + match.group(1) + ". The bot will retry automatically.";
            if (failure instanceof SocketTimeoutException || failure instanceof TimeoutException)
                return "The provider request timed out. The bot will retry automatically.";
            if (failure instanceof UnknownHostException)
                return "The provider hostname could not be resolved. The bot will retry automatically.";
            if (failure instanceof JsonParseException || failure instanceof SAXException)
                return "The provider response could not be parsed. Its feed format may have changed.";
        }
        return "The provider could not be fetched or parsed. The bot will retry on its next scheduled check.";
    }

    public static <N, A> void poll(
        List<N> notifiers,
        Function<N, Key> key,
        Supplier<List<A>> fetch,
        BiConsumer<N, List<A>> deliver
    ) {
        notifiers.forEach(notifier -> checking(key.apply(notifier)));
        List<A> articles;
        try {
            articles = fetch.get();
        } catch (RuntimeException exception) {
            notifiers.forEach(notifier -> checkFailed(key.apply(notifier), failureReason(exception)));
            Constants.LOGGER.error("Notifier feed check failed", exception);
            return;
        }
        for (N notifier : notifiers) {
            checked(key.apply(notifier));
            try {
                if (!articles.isEmpty()) {
                    deliver.accept(notifier, articles);
                }
            } catch (RuntimeException exception) {
                deliveryFailed(key.apply(notifier),
                    "The update could not be prepared or delivered. Ask the bot operator to check the logs.");
                Constants.LOGGER.error("Notifier processing failed", exception);
            }
        }
    }

    public static String channelProblem(Guild guild, long channelId) {
        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, channelId);
        if (channel == null)
            return "The destination channel is missing or is not a text/announcement channel.";
        if (!channel.canTalk())
            return "The bot cannot view or send messages in the destination channel.";
        if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS))
            return "The bot needs Embed Links permission in the destination channel.";
        return null;
    }

    public static Status status(Key key, Guild guild, Document stored) {
        Document record = stored == null ? new Document() : stored;
        return new Status(number(record, "lastCheckAt"), number(record, "lastSuccessfulCheckAt"),
            record.getString("checkError"), number(record, "checkErrorAt"), number(record, "lastDeliveredAt"),
            record.getString("lastDeliveredItem"), record.getString("deliveryError"), number(record, "deliveryErrorAt"),
            number(record, "lastTestAt"), record.getString("testError"), channelProblem(guild, key.channel()),
            key.type().equals("twitch"));
    }

    private static Long number(Document record, String field) {
        return record.get(field) instanceof Number value ? value.longValue() : null;
    }

    private static void update(Key key, Document fields) {
        fields.append("updatedAt", new Date());
        try {
            collection().updateOne(new Document("_id", key.id()), new Document("$set", fields)
                .append("$setOnInsert", new Document("guild", key.guild()).append("type", key.type())),
                new UpdateOptions().upsert(true));
        } catch (RuntimeException exception) {
            Constants.LOGGER.error("Could not persist {} notifier diagnostics for guild {}", key.type(), key.guild(),
                exception);
        }
    }

    public static MongoCollection<Document> collection() {
        return Database.getDatabase().mongoDatabase.getCollection("notifierDiagnostics");
    }
}
