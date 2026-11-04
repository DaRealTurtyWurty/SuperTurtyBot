package dev.darealturtywurty.superturtybot.database;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.modules.audit.AuditedConfigCollection;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.codecs.*;
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import dev.darealturtywurty.superturtybot.modules.quest.QuestActivity;
import dev.darealturtywurty.superturtybot.modules.privacy.OptionalDataCollection;
import dev.darealturtywurty.superturtybot.modules.quest.QuestPlayer;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.Document;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Database {
    private static volatile Database instance;

    public final MongoDatabase mongoDatabase;
    private final MongoClient client;
    public final MongoCollection<Levelling> levelling;
    public final MongoCollection<Counting> counting;
    public final MongoCollection<Suggestion> suggestions;
    public final MongoCollection<Highlighter> highlighters;
    public final MongoCollection<Warning> warnings;
    public final MongoCollection<Tag> tags;
    public final MongoCollection<Showcase> starboard;
    public final MongoCollection<GuildData> guildData;
    public final MongoCollection<UserConfig> userConfig;
    public final MongoCollection<YoutubeNotifier> youtubeNotifier;
    public final MongoCollection<TwitchNotifier> twitchNotifier;
    public final MongoCollection<SteamNotifier> steamNotifier;
    public final MongoCollection<SteamStoreNotifier> steamStoreNotifier;
    public final MongoCollection<RedditNotifier> redditNotifier;
    public final MongoCollection<RedditPostCache> redditPostCache;
    public final MongoCollection<MinecraftNotifier> minecraftNotifier;
    public final MongoCollection<SiegeNotifier> siegeNotifier;
    public final MongoCollection<RocketLeagueNotifier> rocketLeagueNotifier;
    public final MongoCollection<LeagueNotifier> leagueNotifier;
    public final MongoCollection<ValorantNotifier> valorantNotifier;
    public final MongoCollection<Report> reports;
    public final MongoCollection<Quote> quotes;
    public final MongoCollection<UserEmbeds> userEmbeds;
    public final MongoCollection<Reminder> reminders;
    public final MongoCollection<TempBan> tempBans;
    public final MongoCollection<WordleProfile> wordleProfiles;
    public final MongoCollection<Economy> economy;
    public final MongoCollection<MarketplaceListing> marketplaceListings;
    public final MongoCollection<ChatReviver> chatRevivers;
    public final MongoCollection<Birthday> birthdays;
    public final MongoCollection<UserCollectables> userCollectables;
    public final MongoCollection<TwoThousandFortyEightProfile> twoThousandFortyEight;
    public final MongoCollection<StickyMessage> stickyMessages;
    public final MongoCollection<ModmailTicket> modmailTickets;
    public final MongoCollection<ModmailBlockedUser> modmailBlockedUsers;
    public final MongoCollection<ModmailTranscriptChunk> modmailTranscriptChunks;
    public final MongoCollection<StickyRoles> stickyRoles;
    public final MongoCollection<DashboardSession> dashboardSessions;
    public final MongoCollection<QuestPlayer> questPlayers;
    public final MongoCollection<QuestActivity> questActivities;

    private Database(MongoClient client) {
        this.client = client;
        this.mongoDatabase = client.getDatabase("TurtyBot" + (Environment.INSTANCE.isDevelopment() ? "-dev" : ""));

        this.levelling = OptionalDataCollection.wrap(mongoDatabase.getCollection("levelling", Levelling.class), "user");
        this.counting = AuditedConfigCollection.wrap(mongoDatabase.getCollection("counting", Counting.class));
        this.suggestions = OptionalDataCollection.wrap(mongoDatabase.getCollection("suggestions", Suggestion.class),
            "user");
        this.highlighters = OptionalDataCollection.wrap(mongoDatabase.getCollection("highlighters", Highlighter.class),
            "user");
        this.warnings = mongoDatabase.getCollection("warnings", Warning.class);
        this.tags = OptionalDataCollection.wrap(mongoDatabase.getCollection("tags", Tag.class), "user");
        this.starboard = OptionalDataCollection.wrap(mongoDatabase.getCollection("starboard", Showcase.class), "user");
        this.guildData = AuditedConfigCollection.wrap(mongoDatabase.getCollection("guildData", GuildData.class));
        this.userConfig = OptionalDataCollection.wrap(mongoDatabase.getCollection("userConfig", UserConfig.class),
            "user");
        this.youtubeNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("youtubeNotifier", YoutubeNotifier.class));
        this.twitchNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("twitchNotifier", TwitchNotifier.class));
        this.steamNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("steamNotifier", SteamNotifier.class));
        this.steamStoreNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("steamStoreNotifier", SteamStoreNotifier.class));
        this.redditNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("redditNotifier", RedditNotifier.class));
        this.redditPostCache = mongoDatabase.getCollection("redditPostCache", RedditPostCache.class);
        this.minecraftNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("minecraftNotifier", MinecraftNotifier.class));
        this.siegeNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("siegeNotifier", SiegeNotifier.class));
        this.rocketLeagueNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("rocketLeagueNotifier", RocketLeagueNotifier.class));
        this.leagueNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("leagueNotifier", LeagueNotifier.class));
        this.valorantNotifier = AuditedConfigCollection
            .wrap(mongoDatabase.getCollection("valorantNotifier", ValorantNotifier.class));
        this.reports = mongoDatabase.getCollection("reports", Report.class);
        this.quotes = OptionalDataCollection.wrap(mongoDatabase.getCollection("quotes", Quote.class), "user");
        this.userEmbeds = OptionalDataCollection.wrap(mongoDatabase.getCollection("userEmbeds", UserEmbeds.class),
            "user");
        this.reminders = OptionalDataCollection.wrap(mongoDatabase.getCollection("reminders", Reminder.class), "user");
        this.tempBans = mongoDatabase.getCollection("tempBans", TempBan.class);
        this.wordleProfiles = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("wordleProfiles", WordleProfile.class), "user");
        this.economy = OptionalDataCollection.wrap(mongoDatabase.getCollection("economy", Economy.class), "user");
        this.marketplaceListings = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("marketplaceListings", MarketplaceListing.class), "seller");
        this.chatRevivers = mongoDatabase.getCollection("chatRevivers", ChatReviver.class);
        this.birthdays = OptionalDataCollection.wrap(mongoDatabase.getCollection("birthdays", Birthday.class), "user");
        this.userCollectables = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("userCollectables", UserCollectables.class), "user");
        this.twoThousandFortyEight = OptionalDataCollection.wrap(mongoDatabase.getCollection("twoThousandFortyEight",
            TwoThousandFortyEightProfile.class), "user");
        this.stickyMessages = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("stickyMessages", StickyMessage.class), "owner");
        this.modmailTickets = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("modmailTickets", ModmailTicket.class), "user");
        this.modmailBlockedUsers = mongoDatabase.getCollection("modmailBlockedUsers", ModmailBlockedUser.class);
        this.modmailTranscriptChunks = mongoDatabase.getCollection("modmailTranscriptChunks",
            ModmailTranscriptChunk.class);
        this.stickyRoles = OptionalDataCollection.wrap(mongoDatabase.getCollection("stickyRoles", StickyRoles.class),
            "user");
        this.dashboardSessions = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("dashboardSessions", DashboardSession.class), "user.id");
        this.questPlayers = OptionalDataCollection.wrap(mongoDatabase.getCollection("questPlayers", QuestPlayer.class),
            "user");
        this.questActivities = OptionalDataCollection
            .wrap(mongoDatabase.getCollection("questActivities", QuestActivity.class), "user");

        ShutdownHooks.register(client::close);
    }

    public static Database getDatabase() {
        Database local = instance;
        if (local == null) {
            synchronized (Database.class) {
                local = instance;
                if (local == null) {
                    MongoClient client = connect(buildCodecRegistry());
                    instance = local = new Database(client);
                }
            }
        }

        return local;
    }

    public ClientSession startSession() {
        return this.client.startSession();
    }

    public static void ensureIndexes() {
        runWithRetry(3, Duration.ofSeconds(2), () -> {
            Database db = getDatabase();

            final Bson guildIndex = Indexes.descending("guild");
            final Bson userIndex = Indexes.descending("user");
            final Bson channelIndex = Indexes.descending("channel");
            final Bson messageIndex = Indexes.descending("message");
            final Bson guildUser = Indexes.compoundIndex(guildIndex, userIndex);

            db.levelling.createIndex(guildUser);
            db.counting.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("users")));
            db.suggestions.createIndex(guildUser);
            db.highlighters.createIndex(guildUser);
            db.warnings.createIndex(guildUser);
            db.tags.createIndex(guildUser);
            db.starboard.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, messageIndex, userIndex));
            db.guildData.createIndex(guildIndex);
            db.userConfig.createIndex(guildUser);
            db.youtubeNotifier
                .createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("youtubeChannel")));
            db.twitchNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("channel")));
            db.steamNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("appId")));
            db.steamStoreNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.redditNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("subreddit")));
            db.redditPostCache.createIndex(Indexes.compoundIndex(
                Indexes.descending("subreddit"),
                Indexes.descending("guid")),
                new IndexOptions().unique(true));
            db.redditPostCache.createIndex(Indexes.descending("createdAt"));
            db.minecraftNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.siegeNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.rocketLeagueNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.leagueNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.valorantNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex));
            db.reports.createIndex(guildIndex);
            db.quotes.createIndex(guildUser);
            db.userEmbeds.createIndex(userIndex);
            db.reminders.createIndex(guildUser);
            db.reminders.createIndex(userIndex);
            db.tempBans.createIndex(guildUser, new IndexOptions().unique(true));
            db.tempBans.createIndex(Indexes.descending("expiresAt"));
            db.wordleProfiles.createIndex(userIndex);
            db.economy.createIndex(guildUser);
            db.marketplaceListings.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("createdAt")));
            db.marketplaceListings.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("seller"),
                Indexes.descending("item")),
                new IndexOptions().unique(true).partialFilterExpression(Filters.eq("type", "rental")));
            db.chatRevivers.createIndex(guildIndex);
            db.birthdays.createIndex(userIndex);
            db.userCollectables.createIndex(userIndex);
            db.twoThousandFortyEight.createIndex(userIndex);
            db.stickyMessages.createIndex(Indexes.compoundIndex(guildIndex, channelIndex),
                new IndexOptions().unique(true));
            db.modmailTickets.createIndex(Indexes.compoundIndex(guildIndex, channelIndex),
                new IndexOptions().unique(true));
            db.modmailTickets.createIndex(Indexes.compoundIndex(guildIndex, userIndex, Indexes.descending("open")));
            db.modmailTickets.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("ticketNumber")));
            db.modmailBlockedUsers.createIndex(guildUser, new IndexOptions().unique(true));
            db.modmailTranscriptChunks.createIndex(
                Indexes.compoundIndex(guildIndex, Indexes.descending("ticketChannel"),
                    Indexes.descending("chunkIndex")),
                new IndexOptions().unique(true));
            db.stickyRoles.createIndex(guildUser, new IndexOptions().unique(true));
            db.dashboardSessions.createIndex(Indexes.descending("sessionId"), new IndexOptions().unique(true));
            db.dashboardSessions.createIndex(Indexes.descending("expiresAt"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
            db.mongoDatabase.getCollection("privacyRequests").createIndex(
                Indexes.compoundIndex(Indexes.ascending("user"), Indexes.descending("createdAt")));
            ensurePrivacyRequestExpiryIndex(db.mongoDatabase);
            db.mongoDatabase.getCollection("configHistory").createIndex(
                Indexes.compoundIndex(Indexes.ascending("guild"), Indexes.descending("createdAt"),
                    Indexes.descending("_id")));
            db.mongoDatabase.getCollection("configHistory").createIndex(Indexes.ascending("createdAt"),
                new IndexOptions().expireAfter(90L, TimeUnit.DAYS));
            db.mongoDatabase.getCollection("notifierDiagnostics").createIndex(Indexes.ascending("guild"));
            db.mongoDatabase.getCollection("notifierDiagnostics").createIndex(Indexes.ascending("updatedAt"),
                new IndexOptions().expireAfter(90L, TimeUnit.DAYS));
            db.mongoDatabase.getCollection("notifierTestLimits").createIndex(Indexes.ascending("expiresAt"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
            db.mongoDatabase.getCollection("privacyActivity").createIndex(Indexes.ascending("lastActiveAt"));
            db.modmailTickets.createIndex(Indexes.compoundIndex(Indexes.ascending("open"),
                Indexes.ascending("closedAt")));
            db.warnings.createIndex(Indexes.ascending("expiresAt"));
            db.mongoDatabase.getCollection("gistUploads").createIndex(Indexes.ascending("author"));
            db.mongoDatabase.getCollection("gistUploads").createIndex(Indexes.ascending("requester"));
            db.questPlayers.createIndex(Indexes.compoundIndex(guildIndex, userIndex, Indexes.descending("weekStart")),
                new IndexOptions().unique(true));
            db.questActivities.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("guild"),
                    Indexes.ascending("user"),
                    Indexes.ascending("weekStart"),
                    Indexes.ascending("type"),
                    Indexes.ascending("sourceId")),
                new IndexOptions().unique(true));

            return null;
        });
    }

    private static CodecRegistry buildCodecRegistry() {
        final CodecRegistry pojoRegistry = CodecRegistries
            .fromProviders(PojoCodecProvider.builder().automatic(true).build());
        final CodecRegistry customCodecRegistry = CodecRegistries.fromCodecs(
            new BigIntegerCodec(),
            new BigDecimalCodec(),
            new ColorCodec(),
            new NewsitemCodec(),
            new ReminderCodec());

        return CodecRegistries.fromRegistries(
            customCodecRegistry,
            MongoClientSettings.getDefaultCodecRegistry(),
            pojoRegistry);
    }

    private static void ensurePrivacyRequestExpiryIndex(MongoDatabase database) {
        var requests = database.getCollection("privacyRequests");
        for (Document index : requests.listIndexes()) {
            if (new Document("deleteAfter", 1).equals(index.get("key"))
                && (!(index.get("expireAfterSeconds") instanceof Number expiry) || expiry.longValue() != 0
                    || !new Document("status", "resolved").equals(index.get("partialFilterExpression")))) {
                requests.dropIndex(index.getString("name"));
            }
        }
        requests.createIndex(Indexes.ascending("deleteAfter"), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS)
            .partialFilterExpression(Filters.eq("status", "resolved")));
    }

    private static MongoClient connect(CodecRegistry codec) {
        String uri = Environment.INSTANCE.mongoConnectionString().orElse("");
        if (uri.isBlank()) {
            Constants.LOGGER.error("MongoDB connection string has not been set!");
            return MongoClients.create(
                MongoClientSettings.builder()
                    .codecRegistry(codec)
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(15, TimeUnit.SECONDS))
                    .applyToSocketSettings(builder -> builder.readTimeout(30, TimeUnit.SECONDS))
                    .build());
        }

        var connectionString = new ConnectionString(uri);
        var settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .codecRegistry(codec)
            .applicationName("TurtyBot")
            .retryReads(true)
            .retryWrites(true)
            .applyToClusterSettings(builder -> builder.serverSelectionTimeout(15, TimeUnit.SECONDS))
            .applyToSocketSettings(builder -> builder.readTimeout(30, TimeUnit.SECONDS))
            .build();

        return MongoClients.create(settings);
    }

    private static <T> T runWithRetry(int attempts, Duration backoffBase, Callable<T> callable) {
        MongoException last = null;
        for (int index = 1; index <= attempts; index++) {
            try {
                return callable.call();
            } catch (MongoSocketReadException | MongoTimeoutException exception) {
                last = exception;
                if (index == attempts)
                    break;
                try {
                    Thread.sleep(backoffBase.toMillis() * index);
                } catch (InterruptedException _) {
                }
            } catch (MongoException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        if (last == null)
            throw new IllegalStateException("Unreachable code executed in runWithRetry");

        throw last;
    }
}
