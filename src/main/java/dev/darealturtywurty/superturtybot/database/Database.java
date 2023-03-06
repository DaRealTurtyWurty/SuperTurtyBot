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
import dev.darealturtywurty.superturtybot.database.pojos.collections.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.List;

public class Database {
    private static final Database DATABASE = new Database();

    public final MongoCollection<Levelling> levelling;
    public final MongoCollection<Counting> counting;
    public final MongoCollection<Suggestion> suggestions;
    public final MongoCollection<Highlighter> highlighters;
    public final MongoCollection<Warning> warnings;
    public final MongoCollection<Tag> tags;
    public final MongoCollection<Showcase> starboard;
    public final MongoCollection<GuildConfig> guildConfig;
    public final MongoCollection<UserConfig> userConfig;
    public final MongoCollection<YoutubeNotifier> youtubeNotifier;
    public final MongoCollection<TwitchNotifier> twitchNotifier;
    public final MongoCollection<SteamNotifier> steamNotifier;
    public final MongoCollection<Report> reports;
    public final MongoCollection<Quote> quotes;

    public Database() {
        final CodecRegistry pojoRegistry = CodecRegistries
            .fromProviders(PojoCodecProvider.builder().automatic(true).build());
        final CodecRegistry codecRegistry = CodecRegistries
            .fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoRegistry);

        final MongoClient client = connect(codecRegistry);
        ShutdownHooks.register(client::close);
        final MongoDatabase database = client.getDatabase("TurtyBot");

        this.levelling = database.getCollection("levelling", Levelling.class);
        this.counting = database.getCollection("counting", Counting.class);
        this.suggestions = database.getCollection("suggestions", Suggestion.class);
        this.highlighters = database.getCollection("highlighters", Highlighter.class);
        this.warnings = database.getCollection("warnings", Warning.class);
        this.tags = database.getCollection("tags", Tag.class);
        this.starboard = database.getCollection("starboard", Showcase.class);
        this.guildConfig = database.getCollection("guildConfig", GuildConfig.class);
        this.userConfig = database.getCollection("userConfig", UserConfig.class);
        this.youtubeNotifier = database.getCollection("youtubeNotifier", YoutubeNotifier.class);
        this.twitchNotifier = database.getCollection("twitchNotifier", TwitchNotifier.class);
        this.steamNotifier = database.getCollection("steamNotifier", SteamNotifier.class);
        this.reports = database.getCollection("reports", Report.class);
        this.quotes = database.getCollection("quotes", Quote.class);

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
        this.guildConfig.createIndex(guildIndex);
        this.userConfig.createIndex(guildUserIndex);
        this.youtubeNotifier
            .createIndex(Indexes.compoundIndex(guildIndex, channelIndex, Indexes.descending("youtubeChannel")));
        this.twitchNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("channel")));
        this.steamNotifier.createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("appId")));
        this.reports.createIndex(guildIndex);
        this.quotes.createIndex(guildUserIndex);
    }

    public static Database getDatabase() {
        return DATABASE;
    }

    private static MongoClient connect(CodecRegistry codec) {
        final ConnectionString connectionString = new ConnectionString(
            "mongodb+srv://" + Environment.INSTANCE.mongoUsername() + ":" + Environment.INSTANCE.mongoPassword()
                + "@turtybot.omb6j.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        final MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
            .applicationName("TurtyBot").codecRegistry(codec).build();
        return MongoClients.create(settings);
    }
}
