package io.github.darealturtywurty.superturtybot.database.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.BasicBSONList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.commands.levelling.RankCardItem;
import io.github.darealturtywurty.superturtybot.commands.levelling.XPInventory;
import io.github.darealturtywurty.superturtybot.database.GuildedDatabaseHandler;
import io.github.darealturtywurty.superturtybot.database.TurtyBotDatabase;
import io.github.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public final class LevellingDatabaseHandler extends GuildedDatabaseHandler {
    public LevellingDatabaseHandler() {
        super("levels");
    }
    
    @NotNull
    public Pair<Integer, Integer> getData(Member member) {
        final Entry<Long, Pair<Integer, Integer>> found = findMember(member);
        return found == null ? Pair.of(-1, -1) : found.getValue();
    }
    
    @Nullable
    public Entry<Long, Pair<Integer, Integer>> getFirstPlace(Guild guild) {
        final List<Entry<Long, Pair<Integer, Integer>>> leaderboard = getSortedLeaderboard(guild);
        return leaderboard.isEmpty() ? null : leaderboard.get(0);
    }
    
    public XPInventory getInventory(Member member) {
        final Document document = getMember(member);
        final var inventory = new XPInventory();
        List<BsonDocument> docs = document.getList("inventory", BsonDocument.class);
        if (docs == null) {
            document.put("inventory", new BasicBSONList());
            updateMember(member.getGuild(), member.getId(), document);
            docs = new ArrayList<>();
        }
        
        for (final BsonDocument bsonDocument : docs) {
            final String name = bsonDocument.getString("name").getValue();
            final String data = bsonDocument.getString("data").getValue();
            final RankCardItem.Type type = RankCardItem.Type.valueOf(bsonDocument.get("type").asString().getValue());
            final RankCardItem.Rarity rarity = RankCardItem.Rarity
                .valueOf(bsonDocument.get("rarity").asString().getValue());
            inventory.add(RankCardItemRegistry.locate(name, data, type, rarity));
        }
        
        return inventory;
    }

    public Map<Long, Pair<Integer, Integer>> getLeaderboard(Guild guild) {
        final MongoCollection<Document> documents = getCollection(guild);
        final Map<Long, Pair<Integer, Integer>> memberLevels = new HashMap<>();
        for (final Document document : documents.find()) {
            memberLevels.put(Long.parseLong(document.getString("_id")),
                Pair.of(document.getInteger("level"), document.getInteger("xp")));
        }
        
        return memberLevels;
    }
    
    public int getLevel(Member member) {
        return getMember(member).getInteger("level");
    }
    
    public Document getMember(Member member) {
        final MongoCollection<Document> guild = getCollection(member.getGuild());
        Document doc;
        if (guild.find(Filters.eq(member.getId())).first() == null) {
            doc = new Document(Map.of("level", 0, "xp", 0));
            updateMember(member.getGuild(), member.getId(), doc);
        } else {
            doc = guild.find(Filters.eq(member.getId())).first();
        }
        
        return doc;
    }
    
    public int getRank(Member member) {
        final List<Entry<Long, Pair<Integer, Integer>>> leaderboard = TurtyBotDatabase.LEVELS
            .getSortedLeaderboard(member.getGuild());
        final Entry<Long, Pair<Integer, Integer>> found = findMember(member);
        return found == null ? leaderboard.size() : leaderboard.indexOf(found);
    }
    
    public List<Entry<Long, Pair<Integer, Integer>>> getSortedLeaderboard(Guild guild) {
        return sortLeaderboard(getLeaderboard(guild).entrySet().stream().toList());
    }
    
    public int getXP(Member member) {
        return getMember(member).getInteger("xp");
    }
    
    public void putLevel(Member member, int level) {
        put(member, "level", level);
    }
    
    public void putXP(Member member, int xp) {
        put(member, "xp", xp);
    }
    
    public List<Entry<Long, Pair<Integer, Integer>>> sortLeaderboard(
        List<Entry<Long, Pair<Integer, Integer>>> leaderboard) {
        return leaderboard.stream().sorted((entry0, entry1) -> {
            final int xp0 = entry0.getValue().getRight();
            final int xp1 = entry1.getValue().getRight();
            return Integer.compare(xp1, xp0);
        }).toList();
    }
    
    @Nullable
    private Entry<Long, Pair<Integer, Integer>> findMember(Member member) {
        final List<Entry<Long, Pair<Integer, Integer>>> leaderboard = TurtyBotDatabase.LEVELS
            .getSortedLeaderboard(member.getGuild());
        final Optional<Entry<Long, Pair<Integer, Integer>>> optional = leaderboard.stream()
            .filter(entry -> entry.getKey() == member.getIdLong()).findFirst();
        return optional.orElse(null);
    }
    
    private void put(Member member, String key, Object value) {
        final Document doc = getMember(member);
        doc.put(key, value);
        updateMember(member.getGuild(), member.getId(), doc);
    }
    
    public static List<Entry<Long, Pair<Integer, Integer>>> leaderboardToList(Map<Long, Pair<Integer, Integer>> map) {
        return map.entrySet().stream().toList();
    }
    
    public static Map<Long, Map<Long, Pair<Integer, Integer>>> load() {
        final Map<Long, Map<Long, Pair<Integer, Integer>>> guildLevels = new HashMap<>();
        final MongoDatabase database = TurtyBotDatabase.LEVELS.getDatabase();
        for (final String collectionName : database.listCollectionNames()) {
            final MongoCollection<Document> guild = database.getCollection(collectionName);
            final Map<Long, Pair<Integer, Integer>> memberLevels = new HashMap<>();
            for (final Document document : guild.find()) {
                memberLevels.put(Long.parseLong(document.getString("_id")),
                    Pair.of(document.getInteger("level"), document.getInteger("xp")));
            }
            
            guildLevels.put(Long.parseLong(collectionName), memberLevels);
        }
        
        return guildLevels;
    }
}
