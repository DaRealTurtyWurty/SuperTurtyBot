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

import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.core.util.WeightedRandomBag;
import io.github.darealturtywurty.superturtybot.database.TurtyBotDatabase;
import io.github.darealturtywurty.superturtybot.database.impl.LevellingDatabaseHandler;
import io.github.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class LevellingManager extends ListenerAdapter {
    public static final LevellingManager INSTANCE = new LevellingManager();
    private final Map<Long, Map<Long, Pair<Integer, Integer>>> levelMap = new HashMap<>();
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

        final Map<Long, Map<Long, Pair<Integer, Integer>>> levels = LevellingDatabaseHandler.load();
        this.levelMap.putAll(levels);
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

        this.levelMap.computeIfAbsent(guild.getIdLong(), id -> new HashMap<>());
        this.cooldownMap.computeIfAbsent(event.getGuild().getIdLong(), id -> new ConcurrentHashMap<>());
        final Map<Long, Pair<Integer, Integer>> memberXP = this.levelMap.get(guild.getIdLong());
        final Map<Long, Long> cooldowns = this.cooldownMap.get(guild.getIdLong());

        final Member member = event.getMember();
        if (cooldowns.containsKey(member.getIdLong()) && cooldowns.get(member.getIdLong()) > 0)
            return;

        // TODO: Server configurable and re-enable
        // cooldowns.put(member.getIdLong(), 25000L);

        memberXP.computeIfAbsent(member.getIdLong(), id -> Pair.of(0, 0));
        final Pair<Integer, Integer> xpLevel = memberXP.get(member.getIdLong());
        final int level = xpLevel.getKey();

        int xp = xpLevel.getValue();
        xp += ThreadLocalRandom.current().nextInt(5, 15);
        // TODO: Re-enable
        // xp = applyBooster(member, event.getMessage(), xp);
        TurtyBotDatabase.LEVELS.putXP(member, xp);

        final int newLevel = getLevelForXP(xp);
        memberXP.put(member.getIdLong(), Pair.of(newLevel, xp));
        if (newLevel > level) {
            TurtyBotDatabase.LEVELS.putLevel(member, newLevel);
            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setDescription(member.getAsMention() + ", you are now Level " + newLevel + "! 🎉");
            embed.setColor(Color.BLUE);
            event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
            if (ThreadLocalRandom.current().nextInt(50) == 0) {
                final XPInventory inventory = TurtyBotDatabase.LEVELS.getInventory(member);

                final RankCardItem chosen = createWeightedBag(inventory).getRandom();
                inventory.add(chosen);
                event.getMessage()
                    .reply("Congratulations " + member.getAsMention() + "! You earned an `"
                        + WordUtils.capitalize(chosen.rarity.name().toLowerCase())
                        + "` rank card item! Use `/xpinventory` to view your inventory!")
                    .mentionRepliedUser(false).queue();
            }
        }

        this.levelMap.put(guild.getIdLong(), memberXP);
        this.cooldownMap.put(guild.getIdLong(), cooldowns);
    }

    private int applyBooster(Member member, Message message, int current) {
        return Math.round(
            current * (message.getContentRaw().length() > 64 ? (message.getContentRaw().length() - 64) * 1.15f : 1f))
            * (message.getAttachments().size() + 1);
    }

    private WeightedRandomBag<RankCardItem> createWeightedBag(XPInventory inventory) {
        final var bag = new WeightedRandomBag<RankCardItem>();
        
        final List<RankCardItem> canAdd = new ArrayList<>();
        RankCardItemRegistry.RANK_CARD_ITEMS.getRegistry().values().forEach(canAdd::add);
        inventory.forEach(canAdd::remove);
        
        canAdd.forEach(item -> bag.addEntry(item, item.rarity.chance));
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
