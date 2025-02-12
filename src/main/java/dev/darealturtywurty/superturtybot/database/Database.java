package dev.darealturtywurty.superturtybot.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.codecs.*;
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

public class Database {
    private static final Database DATABASE = new Database();

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

    @SuppressWarnings("resource")
    public Database() {
        final CodecRegistry pojoRegistry = CodecRegistries
            .fromProviders(PojoCodecProvider.builder().automatic(true).build());
        final CodecRegistry customCodecRegistry = CodecRegistries.fromCodecs(
                new BigIntegerCodec(),
                new BigDecimalCodec(),
                new ColorCodec(),
                new NewsitemCodec()
        );
        final CodecRegistry codecRegistry = CodecRegistries
            .fromRegistries(customCodecRegistry, MongoClientSettings.getDefaultCodecRegistry(), pojoRegistry);

        final MongoClient client = connect(codecRegistry);
        ShutdownHooks.register(client::close);
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

        final Bson guildIndex = Indexes.descending("guild");
        final Bson userIndex = Indexes.descending("user");
        final Bson channelIndex = Indexes.descending("channel");
        final Bson messageIndex = Indexes.descending("message");
        final Bson guildUserIndex = Indexes.compoundIndex(guildIndex, userIndex);
        
        this.levelling.createIndex(guildUserIndex);
        this.counting.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("users")));
        this.suggestions.createIndex(guildUserIndex);
        this.highlighters.createIndex(guildUserIndex);
        this.warnings.createIndex(guildUserIndex);
        this.tags.createIndex(guildUserIndex);
        this.starboard.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, messageIndex, userIndex));
        this.guildData.createIndex(guildIndex);
        this.userConfig.createIndex(guildUserIndex);
        this.youtubeNotifier.createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("youtubeChannel")));
        this.twitchNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("channel")));
        this.steamNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("appId")));
        this.redditNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("subreddit")));
        this.reports.createIndex(guildIndex);
        this.quotes.createIndex(guildUserIndex);
        this.userEmbeds.createIndex(userIndex);
        this.reminders.createIndex(guildUserIndex);
        this.savedSongs.createIndex(userIndex);
        this.wordleProfiles.createIndex(userIndex);
        this.economy.createIndex(guildUserIndex);
        this.chatRevivers.createIndex(guildIndex);
        this.birthdays.createIndex(userIndex);
        this.submissionCategories.createIndex(guildIndex);
        this.userCollectables.createIndex(userIndex);
        this.twoThousandFortyEight.createIndex(userIndex);
    }

    public static Database getDatabase() {
        return DATABASE;
    }

    private static MongoClient connect(CodecRegistry codec) {
        if (Environment.INSTANCE.mongoConnectionString().isEmpty()) {
            Constants.LOGGER.error("MongoDB connection string has not been set!");
            return MongoClients.create();
        }

        final ConnectionString connectionString = new ConnectionString(Environment.INSTANCE.mongoConnectionString().get());
        final MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
            .applicationName("TurtyBot").codecRegistry(codec).build();
        return MongoClients.create(settings);
    }
}
