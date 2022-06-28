package io.github.darealturtywurty.superturtybot.database;

import java.util.function.Function;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;

public class DatabaseHandler {
    private final Function<String, MongoCollection<Document>> collection;
    protected final MongoDatabase database;
    public final String name;

    public DatabaseHandler(String name) {
        this.collection = this::getCollection;
        this.name = name;
        this.database = getDatabase(this.name);
    }

    public Document createId(Object id) {
        return new Document("_id", String.valueOf(id));
    }
    
    public FindIterable<Document> get(String collection, String key) {
        return getCollection(collection).find(Filters.exists(key));
    }
    
    public FindIterable<Document> get(String collection, String key, Object value) {
        return getCollection(collection).find(Filters.eq(key, value));
    }
    
    public FindIterable<Document> getById(String collection, Object id) {
        return getCollection(collection).find(Filters.eq(String.valueOf(id)));
    }

    public FindIterable<Document> getById(String collection, String id) {
        return getCollection(collection).find(Filters.eq(id));
    }

    public MongoDatabase getDatabase() {
        return this.database;
    }
    
    public void put(MongoCollection<Document> collection, Document document) {
        collection.insertOne(document);
    }
    
    protected final MongoCollection<Document> getCollection(String name) {
        MongoCollection<Document> collection;
        try {
            collection = this.database.getCollection(name);
        } catch (final IllegalArgumentException exception) {
            this.database.createCollection(name);
            try {
                collection = this.database.getCollection(name);
            } catch (final IllegalArgumentException exception0) {
                throw new IllegalStateException("Unable to create " + name + "collection!", exception0);
            }
        }
        
        return collection;
    }
    
    public static Bson set(Object toSet) {
        return new Document("$set", toSet);
    }
    
    private static final MongoDatabase getDatabase(String name) {
        final ConnectionString connectionString = new ConnectionString(
            "mongodb+srv://" + Environment.INSTANCE.mongoUsername() + ":" + Environment.INSTANCE.mongoPassword()
                + "@turtybot.omb6j.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
        final MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
            .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build()).build();
        final MongoClient mongoClient = MongoClients.create(settings);
        ShutdownHooks.register(mongoClient::close);
        return mongoClient.getDatabase(name);
    }
}
