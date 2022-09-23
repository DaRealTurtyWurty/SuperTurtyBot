package io.github.darealturtywurty.superturtybot.commands.levelling;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;

import com.google.common.primitives.Longs;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.commands.core.config.ServerConfigCommand;
import io.github.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Rarity;
import io.github.darealturtywurty.superturtybot.core.ShutdownHooks;
import io.github.darealturtywurty.superturtybot.core.util.WeightedRandomBag;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import io.github.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class LevellingManager extends ListenerAdapter {
    public static final LevellingManager INSTANCE = new LevellingManager();
    private final Map<Long, Set<Long>> disabledChannels = new HashMap<>();
    private final Map<Long, Map<Long, Long>> cooldownMap = new ConcurrentHashMap<>();
    private final List<Long> disabledGuilds = List.of();
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

        final Bson serverConfigFilter = ServerConfigCommand.getFilter(guild);
        final GuildConfig config = ServerConfigCommand.get(serverConfigFilter, guild);

        final List<Long> disabledChannels = Stream.of(config.getDisabledLevellingChannels().split("[\s;]"))
            .map(Longs::tryParse).toList();
        if (!config.isLevellingEnabled() || disabledChannels.contains(event.getChannel().getIdLong()))
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

        cooldowns.put(member.getIdLong(), config.getLevelCooldown());

        final List<Bson> updates = new ArrayList<>();

        final int level = userProfile.getLevel();
        int xp = userProfile.getXp();
        xp += ThreadLocalRandom.current().nextInt(config.getMinXP(), config.getMaxXP());

        userProfile.setXp(xp);
        updates.add(Updates.set("xp", xp));

        final int newLevel = getLevelForXP(xp);
        if (newLevel > level) {
            userProfile.setLevel(newLevel);
            updates.add(Updates.set("level", newLevel));

            final var userRoles = event.getMember().getRoles().stream().map(Role::getIdLong)
                .collect(Collectors.toSet());
            final var levelRoles = getLevelRoles(config);
            final var toAddRoles = levelRoles.entrySet().stream().filter(it -> it.getKey() <= newLevel)
                .filter(it -> !userRoles.contains(it.getValue())).map(Map.Entry::getValue)
                .map(event.getGuild()::getRoleById).filter(Objects::nonNull).toList();
            event.getGuild().modifyMemberRoles(event.getMember(), toAddRoles, null).queue();

            final var description = new StringBuilder(
                member.getAsMention() + ", you are now Level " + newLevel + "! 🎉");

            // TODO: Re-enable when implemented properly
            /*
             * if (ThreadLocalRandom.current().nextInt(config.getLevellingItemChance()) == 0) { final List<String>
             * inventory = userProfile.getInventory(); final Pair<String, Rarity> chosen =
             * createWeightedBag(inventory).getRandom(); inventory.add(chosen.getLeft());
             * updates.add(Updates.set("inventory", inventory)); description.append("\n\nCongratulations " +
             * member.getAsMention() + "! You earned an `" +
             * WordUtils.capitalize(chosen.getRight().name().toLowerCase()) +
             * "` rank card item! Use `/xpinventory` to view your inventory!"); }
             */

            if (!config.areLevelUpMessagesDisabled()) {
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setDescription(description);
                embed.setColor(Color.BLUE);

                if (!config.hasLevelUpChannel()) {
                    if (config.shouldEmbedLevelUpMessage()) {
                        event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
                    } else {
                        event.getMessage().reply(description).mentionRepliedUser(false).queue();
                    }
                } else {
                    final TextChannel channel = event.getJDA().getTextChannelById(config.getLevelUpMessageChannel());
                    if (channel == null) {
                        if (config.shouldEmbedLevelUpMessage()) {
                            event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
                        } else {
                            event.getMessage().reply(description).mentionRepliedUser(false).queue();
                        }
                    } else if (config.shouldEmbedLevelUpMessage()) {
                        channel.sendMessageEmbeds(embed.build()).mentionRepliedUser(false).queue();
                    } else {
                        channel.sendMessage(description).mentionRepliedUser(false).queue();
                    }
                }
            }
        }

        Database.getDatabase().levelling.updateOne(filter, updates);
        this.cooldownMap.put(guild.getIdLong(), cooldowns);
    }

    private WeightedRandomBag<Pair<String, Rarity>> createWeightedBag(List<String> inventory) {
        final var bag = new WeightedRandomBag<Pair<String, Rarity>>();

        final Map<String, Rarity> canAdd = new HashMap<>();
        RankCardItemRegistry.RANK_CARD_ITEMS.getRegistry().values()
            .forEach(item -> canAdd.put(item.getName(), item.rarity));

        inventory.forEach(canAdd::remove);

        canAdd.forEach((key, value) -> bag.addEntry(Pair.of(key, value), value.chance));
        return bag;
    }

    private Map<Integer, Long> getLevelRoles(GuildConfig config) {
        record LevelWithRole(int level, long role) {
        }

        if (config.getLevelRoles().isEmpty())
            return Map.of();

        return Stream.of(config.getLevelRoles().split("[\s;]")).map(it -> it.split("->"))
            .map(it -> new LevelWithRole(Integer.parseInt(it[0].trim()), Long.parseLong(it[1].trim())))
            .collect(Collectors.toMap(LevelWithRole::level, LevelWithRole::role));
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
