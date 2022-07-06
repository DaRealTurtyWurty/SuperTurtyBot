package io.github.darealturtywurty.superturtybot.commands.levelling;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Rarity;
import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.core.util.WeightedRandomBag;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import io.github.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class LevellingManager extends ListenerAdapter {
    public static final LevellingManager INSTANCE = new LevellingManager();
    private final Map<Long, Set<Long>> disabledChannels = new HashMap<>();
    private final Map<Long, Map<Long, Long>> cooldownMap = new ConcurrentHashMap<>();
    private final List<Long> disabledGuilds = new ArrayList<>();
    private final Timer cooldownTimer = new Timer();
    
    private LevellingManager() {
        final var cooldownManager = new CooldownManager();
        this.cooldownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cooldownManager.run();
            }
        }, 0, 1000);
        
        ShutdownHooks.register(this.cooldownTimer::cancel);
    }
    
    public boolean areLevelsEnabled(Guild guild) {
        return !this.disabledGuilds.contains(guild.getIdLong());
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isSystem()
            || event.getAuthor().isBot())
            return;
        
        final Guild guild = event.getGuild();
        if (guild.getIdLong() != 988836542120357949L || this.disabledGuilds.contains(guild.getIdLong())
            || this.disabledChannels.containsKey(event.getChannel().getIdLong()))
            return;
        
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("user", event.getAuthor().getIdLong()));
        Levelling userProfile = Database.getDatabase().levelling.find(filter).first();
        if (userProfile == null) {
            userProfile = new Levelling(guild.getIdLong(), event.getAuthor().getIdLong());
            Database.getDatabase().levelling.insertOne(userProfile);
        }
        
        this.cooldownMap.computeIfAbsent(event.getGuild().getIdLong(), id -> new ConcurrentHashMap<>());
        final Map<Long, Long> cooldowns = this.cooldownMap.get(guild.getIdLong());
        
        final Member member = event.getMember();
        if (cooldowns.containsKey(member.getIdLong()) && cooldowns.get(member.getIdLong()) > 0)
            return;
            
        // TODO: Server configurable and re-enable
        // cooldowns.put(member.getIdLong(), 25000L);
        
        final List<Bson> updates = new ArrayList<>();
        
        final int level = userProfile.getLevel();
        
        int xp = userProfile.getXp();
        xp += ThreadLocalRandom.current().nextInt(5, 15);
        // TODO: Re-enable
        // xp = applyBooster(member, event.getMessage(), xp);
        updates.add(Updates.set("xp", xp));
        userProfile.setXp(xp);
        
        final int newLevel = getLevelForXP(xp);
        if (newLevel > level) {
            updates.add(Updates.set("level", newLevel));
            userProfile.setLevel(newLevel);
            
            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setDescription(member.getAsMention() + ", you are now Level " + newLevel + "! 🎉");
            embed.setColor(Color.BLUE);
            
            if (ThreadLocalRandom.current().nextInt(50) == 0) {
                final List<String> inventory = userProfile.getInventory();
                
                final Pair<String, Rarity> chosen = createWeightedBag(inventory).getRandom();
                inventory.add(chosen.getLeft());
                updates.add(Updates.set("inventory", inventory));
                
                embed.appendDescription("\n\nCongratulations " + member.getAsMention() + "! You earned an `"
                    + WordUtils.capitalize(chosen.getRight().name().toLowerCase())
                    + "` rank card item! Use `/xpinventory` to view your inventory!");
            }
            
            event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
        }
        
        Database.getDatabase().levelling.updateOne(filter, updates);
        this.cooldownMap.put(guild.getIdLong(), cooldowns);
    }
    
    private int applyBooster(Member member, Message message, int current) {
        return Math.round(
            current * (message.getContentRaw().length() > 64 ? (message.getContentRaw().length() - 64) * 1.15f : 1f))
            * (message.getAttachments().size() + 1);
    }
    
    private WeightedRandomBag<Pair<String, Rarity>> createWeightedBag(List<String> inventory) {
        final var bag = new WeightedRandomBag<Pair<String, Rarity>>();
        
        final Map<String, Rarity> canAdd = new HashMap<>();
        RankCardItemRegistry.RANK_CARD_ITEMS.getRegistry().values().forEach(item -> {
            canAdd.put(item.getName(), item.rarity);
        });
        
        inventory.forEach(canAdd::remove);
        
        canAdd.forEach((key, value) -> bag.addEntry(Pair.of(key, value), value.chance));
        return bag;
    }
    
    public static int getLevelForXP(final int xp) {
        return (int) ((-25 + Math.sqrt(5 * (120 + xp))) / 5);
    }
    
    public static int getXPForLevel(final int level) {
        return (int) (5 * Math.pow(level, 2) + 50 * level + 5);
    }
    
    private final class CooldownManager implements Runnable {
        private CooldownManager() {
        }
        
        @Override
        public void run() {
            final Map<Long, Map<Long, Long>> newCooldownMap = new ConcurrentHashMap<>();
            LevellingManager.this.cooldownMap.forEach((guildId, cooldowns) -> {
                final Map<Long, Long> newCooldowns = cooldowns;
                newCooldowns.forEach((userId, cooldown) -> {
                    newCooldowns.put(userId, cooldown - 1000);
                    
                    if (cooldown <= 0) {
                        newCooldowns.remove(userId);
                    }
                });
                
                newCooldownMap.put(guildId, newCooldowns);
            });
            
            LevellingManager.this.cooldownMap.clear();
            LevellingManager.this.cooldownMap.putAll(newCooldownMap);
        }
    }
}
