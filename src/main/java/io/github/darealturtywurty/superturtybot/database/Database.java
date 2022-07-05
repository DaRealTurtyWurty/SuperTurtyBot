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
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;

public class Database {
    private static final Database DATABASE = new Database();
    
    public final MongoCollection<Levelling> levelling;
    public final MongoCollection<Counting> counting;
    public final MongoCollection<Suggestion> suggestions;
    
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

        final Bson guildIndex = Indexes.descending("guild");
        final Bson userIndex = Indexes.descending("user");
        final Bson guildUserIndex = Indexes.compoundIndex(guildIndex, userIndex);
        this.levelling.createIndex(guildUserIndex);
        this.counting
            .createIndex(Indexes.compoundIndex(guildIndex, Indexes.descending("channel"), Indexes.descending("users")));
        this.suggestions.createIndex(guildUserIndex);
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
