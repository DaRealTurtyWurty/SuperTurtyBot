package dev.darealturtywurty.superturtybot.database;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.codecs.BigDecimalCodec;
import dev.darealturtywurty.superturtybot.database.codecs.BigIntegerCodec;
import dev.darealturtywurty.superturtybot.database.codecs.ColorCodec;
import dev.darealturtywurty.superturtybot.database.codecs.NewsitemCodec;
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Database {
    private static volatile Database instance;

    public final MongoDatabase mongoDatabase;
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
    public final MongoCollection<RedditNotifier> redditNotifier;
    public final MongoCollection<Report> reports;
    public final MongoCollection<Quote> quotes;
    public final MongoCollection<UserEmbeds> userEmbeds;
    public final MongoCollection<Reminder> reminders;
    public final MongoCollection<SavedSongs> savedSongs;
    public final MongoCollection<WordleProfile> wordleProfiles;
    public final MongoCollection<Economy> economy;
    public final MongoCollection<ChatReviver> chatRevivers;
    public final MongoCollection<Birthday> birthdays;
    public final MongoCollection<SubmissionCategory> submissionCategories;
    public final MongoCollection<UserCollectables> userCollectables;
    public final MongoCollection<TwoThousandFortyEightProfile> twoThousandFortyEight;

    private Database(MongoClient client) {
        this.mongoDatabase = client.getDatabase("TurtyBot" + (Environment.INSTANCE.isDevelopment() ? "-dev" : ""));

        this.levelling = mongoDatabase.getCollection("levelling", Levelling.class);
        this.counting = mongoDatabase.getCollection("counting", Counting.class);
        this.suggestions = mongoDatabase.getCollection("suggestions", Suggestion.class);
        this.highlighters = mongoDatabase.getCollection("highlighters", Highlighter.class);
        this.warnings = mongoDatabase.getCollection("warnings", Warning.class);
        this.tags = mongoDatabase.getCollection("tags", Tag.class);
        this.starboard = mongoDatabase.getCollection("starboard", Showcase.class);
        this.guildData = mongoDatabase.getCollection("guildData", GuildData.class);
        this.userConfig = mongoDatabase.getCollection("userConfig", UserConfig.class);
        this.youtubeNotifier = mongoDatabase.getCollection("youtubeNotifier", YoutubeNotifier.class);
        this.twitchNotifier = mongoDatabase.getCollection("twitchNotifier", TwitchNotifier.class);
        this.steamNotifier = mongoDatabase.getCollection("steamNotifier", SteamNotifier.class);
        this.redditNotifier = mongoDatabase.getCollection("redditNotifier", RedditNotifier.class);
        this.reports = mongoDatabase.getCollection("reports", Report.class);
        this.quotes = mongoDatabase.getCollection("quotes", Quote.class);
        this.userEmbeds = mongoDatabase.getCollection("userEmbeds", UserEmbeds.class);
        this.reminders = mongoDatabase.getCollection("reminders", Reminder.class);
        this.savedSongs = mongoDatabase.getCollection("savedSongs", SavedSongs.class);
        this.wordleProfiles = mongoDatabase.getCollection("wordleProfiles", WordleProfile.class);
        this.economy = mongoDatabase.getCollection("economy", Economy.class);
        this.chatRevivers = mongoDatabase.getCollection("chatRevivers", ChatReviver.class);
        this.birthdays = mongoDatabase.getCollection("birthdays", Birthday.class);
        this.submissionCategories = mongoDatabase.getCollection("submissionCategories", SubmissionCategory.class);
        this.userCollectables = mongoDatabase.getCollection("userCollectables", UserCollectables.class);
        this.twoThousandFortyEight = mongoDatabase.getCollection("twoThousandFortyEight", TwoThousandFortyEightProfile.class);

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
            db.youtubeNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("youtubeChannel")));
            db.twitchNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("channel")));
            db.steamNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("appId")));
            db.redditNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("subreddit")));
            db.reports.createIndex(guildIndex);
            db.quotes.createIndex(guildUser);
            db.userEmbeds.createIndex(userIndex);
            db.reminders.createIndex(guildUser);
            db.savedSongs.createIndex(userIndex);
            db.wordleProfiles.createIndex(userIndex);
            db.economy.createIndex(guildUser);
            db.chatRevivers.createIndex(guildIndex);
            db.birthdays.createIndex(userIndex);
            db.submissionCategories.createIndex(guildIndex);
            db.userCollectables.createIndex(userIndex);
            db.twoThousandFortyEight.createIndex(userIndex);

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
                new NewsitemCodec()
        );

        return CodecRegistries.fromRegistries(
                customCodecRegistry,
                MongoClientSettings.getDefaultCodecRegistry(),
                pojoRegistry
        );
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
                            .build()
            );
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
                if (index == attempts) break;
                try {
                    Thread.sleep(backoffBase.toMillis() * index);
                } catch (InterruptedException ignored) {}
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
