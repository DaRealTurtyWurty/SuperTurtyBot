package io.github.darealturtywurty.superturtybot.database.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.database.GuildedDatabaseHandler;
import io.github.darealturtywurty.superturtybot.modules.counting.CountingMode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class CountingDatabaseHandler extends GuildedDatabaseHandler {
    public CountingDatabaseHandler() {
        super("counting");
    }
    
    public List<Document> getChannels(Guild guild) {
        final MongoCollection<Document> collection = getCollection(guild);
        if (collection.countDocuments() < 1)
            return new ArrayList<>();
        
        final List<Document> docs = new ArrayList<>();
        collection.find().forEach(docs::add);
        return docs;
    }

    public int getCounts(Member member) {
        return getMember(member).getInteger("counts", 0);
    }

    public ChannelData getData(Guild guild, long id) {
        final List<Document> channels = getChannels(guild);
        final Optional<Document> optDoc = channels.stream().filter(doc -> doc.containsKey(String.valueOf(id)))
            .findFirst();
        if (!optDoc.isPresent())
            return ChannelData.EMPTY;

        final Document document = optDoc.get();

        final Document data = (Document) document.get(String.valueOf(id));
        
        CountingMode mode;
        if (data.containsKey("mode")) {
            mode = CountingMode.valueOf(data.getString("mode"));
        } else {
            mode = CountingMode.NONE;
        }
        
        final int currentCount = data.getInteger("currentCount", 0);
        final int highestCount = data.getInteger("highestCount", currentCount);
        final long lastCounter = data.getLong("lastCounter");
        return new ChannelData(mode, currentCount, highestCount, lastCounter);
    }
    
    public ChannelData getData(Guild guild, TextChannel channel) {
        return getData(guild, channel.getIdLong());
    }

    public Document getMember(Member member) {
        final MongoCollection<Document> guild = getCollection(member.getGuild());
        Document doc;
        if (guild.find(Filters.eq(member.getId())).first() == null) {
            doc = new Document(Map.of("counts", 0));
            updateMember(member.getGuild(), member.getId(), doc);
        } else {
            doc = guild.find(Filters.eq(member.getId())).first();
        }
        
        return doc;
    }

    public boolean isCountingChannel(Guild guild, long id) {
        final List<Document> channels = getChannels(guild);
        return channels.stream().anyMatch(doc -> doc.containsKey(String.valueOf(id)));
    }

    public boolean isCountingChannel(Guild guild, TextChannel channel) {
        return isCountingChannel(guild, channel.getIdLong());
    }
    
    public void putCounts(Member member, int counts) {
        put(member, "counts", counts);
    }
    
    public void resetChannel(Guild guild, long id, ChannelData currentData) {
        setData(guild, id, new ChannelData(currentData.mode(), 0, currentData.highestCount(), 0L));
    }
    
    public boolean setCountingChannel(Guild guild, long id, CountingMode mode) {
        if (isCountingChannel(guild, id))
            return false;
        
        updateGuild(guild, String.valueOf(id),
            new Document(Map.of("mode", mode.toString(), "currentCount", 0, "highestCount", 0, "lastCounter", 0L)));
        return true;
    }
    
    public void setData(Guild guild, long id, ChannelData data) {
        final List<Document> channels = getChannels(guild);
        final Optional<Document> optDoc = channels.stream().filter(doc -> doc.containsKey(String.valueOf(id)))
            .findFirst();
        if (!optDoc.isPresent())
            return;
        
        final Document found = channels.stream().filter(doc -> doc.containsKey(String.valueOf(id))).findFirst()
            .orElse(null);
        if (found == null)
            return;
        
        updateGuild(guild, String.valueOf(id), new Document(Map.of("mode", data.mode().toString(), "currentCount",
            data.currentCount(), "highestCount", data.highestCount(), "lastCounter", data.lastCounter())));
    }

    private void put(Member member, String key, Object value) {
        final Document doc = getMember(member);
        doc.put(key, value);
        updateMember(member.getGuild(), member.getId(), doc);
    }

    public static record ChannelData(CountingMode mode, int currentCount, int highestCount, long lastCounter) {
        public static final ChannelData EMPTY = new ChannelData(CountingMode.NONE, 0, 0, 0L);
    }
}
