package io.github.darealturtywurty.superturtybot.database;

import org.bson.Document;

import com.google.errorprone.annotations.ForOverride;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import net.dv8tion.jda.api.entities.Guild;

public class GuildedDatabaseHandler extends DatabaseHandler {
    public GuildedDatabaseHandler(String name) {
        super(name);
    }

    public FindIterable<Document> get(Guild guild, String key) {
        return getCollection(guild).find(Filters.exists(key));
    }
    
    public FindIterable<Document> get(Guild guild, String key, Object value) {
        return getCollection(guild).find(Filters.eq(key, value));
    }

    public FindIterable<Document> getById(Guild guild, Object id) {
        return getCollection(guild).find(Filters.eq(String.valueOf(id)));
    }

    public FindIterable<Document> getById(Guild guild, String id) {
        return getCollection(guild).find(Filters.eq(id));
    }
    
    public MongoCollection<Document> getCollection(Guild guild) {
        return getCollection(guild.getId());
    }
    
    public void updateGuild(Guild guild, String key, Object obj) {
        final MongoCollection<Document> collection = getCollection(guild);
        final UpdateResult result = collection.updateOne(Filters.exists(key), set(obj));
        if (result.getModifiedCount() < 1) {
            System.out.println("inserted");
            collection.insertOne(new Document(key, obj));
        } else {
            System.out.println("updated");
        }
    }

    public void updateMember(Guild guild, String id, Object object) {
        final MongoCollection<Document> collection = getCollection(guild);
        final UpdateResult result = collection.updateOne(Filters.eq(id), set(object));
        if (result.getModifiedCount() < 1) {
            System.out.println("inserted");
            final Document doc = createId(id);
            if (object instanceof final Document document) {
                document.forEach(doc::append);
            }

            collection.insertOne(doc);
        } else {
            System.out.println("updated");
        }
    }
    
    @ForOverride
    protected Document createGuildDoc(Guild guild) {
        return createId(guild.getId());
    }
}
