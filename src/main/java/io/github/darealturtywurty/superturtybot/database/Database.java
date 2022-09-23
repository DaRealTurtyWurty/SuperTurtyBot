package io.github.darealturtywurty.superturtybot.database;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Counting;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Highlighter;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Showcase;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.YoutubeNotifier;

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
