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
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

public class Database {
    private static final Database DATABASE = new Database();

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

    @SuppressWarnings("resource")
    public Database() {
        final CodecRegistry pojoRegistry = CodecRegistries
            .fromProviders(PojoCodecProvider.builder().automatic(true).build());
        final CodecRegistry codecRegistry = CodecRegistries
            .fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoRegistry);

        final MongoClient client = connect(codecRegistry);
        ShutdownHooks.register(client::close);
        final MongoDatabase database = client.getDatabase(Environment.INSTANCE.isDevelopment() ? "TurtyBotDev" : "TurtyBot");

        this.levelling = database.getCollection("levelling", Levelling.class);
        this.counting = database.getCollection("counting", Counting.class);
        this.suggestions = database.getCollection("suggestions", Suggestion.class);
        this.highlighters = database.getCollection("highlighters", Highlighter.class);
        this.warnings = database.getCollection("warnings", Warning.class);
        this.tags = database.getCollection("tags", Tag.class);
        this.starboard = database.getCollection("starboard", Showcase.class);
        this.guildData = database.getCollection("guildData", GuildData.class);
        this.userConfig = database.getCollection("userConfig", UserConfig.class);
        this.youtubeNotifier = database.getCollection("youtubeNotifier", YoutubeNotifier.class);
        this.twitchNotifier = database.getCollection("twitchNotifier", TwitchNotifier.class);
        this.steamNotifier = database.getCollection("steamNotifier", SteamNotifier.class);
        this.redditNotifier = database.getCollection("redditNotifier", RedditNotifier.class);
        this.reports = database.getCollection("reports", Report.class);
        this.quotes = database.getCollection("quotes", Quote.class);
        this.userEmbeds = database.getCollection("userEmbeds", UserEmbeds.class);
        this.reminders = database.getCollection("reminders", Reminder.class);
        this.savedSongs = database.getCollection("savedSongs", SavedSongs.class);
        this.wordleProfiles = database.getCollection("wordleProfiles", WordleProfile.class);
        this.economy = database.getCollection("economy", Economy.class);
        this.chatRevivers = database.getCollection("chatRevivers", ChatReviver.class);
        this.birthdays = database.getCollection("birthdays", Birthday.class);
        this.submissionCategories = database.getCollection("submissionCategories", SubmissionCategory.class);
        this.userCollectables = database.getCollection("userCollectables", UserCollectables.class);

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
