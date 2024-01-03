package dev.darealturtywurty.superturtybot.commands.levelling;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCardItem.Rarity;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.object.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LevellingManager extends ListenerAdapter {
    public static final LevellingManager INSTANCE = new LevellingManager();
    private final Map<Long, Set<Long>> disabledChannels = new HashMap<>();
    private final Map<Long, Map<Long, Long>> cooldownMap = new ConcurrentHashMap<>();
    private final List<Long> disabledGuilds = List.of();

    private LevellingManager() {
        ScheduledExecutorService cooldownScheduler = Executors.newSingleThreadScheduledExecutor();
        cooldownScheduler.scheduleAtFixedRate(new CooldownManager(), 0, 1000, TimeUnit.MILLISECONDS);
        ShutdownHooks.register(cooldownScheduler::shutdown);
    }

    public boolean areLevelsEnabled(Guild guild) {
        if(this.disabledGuilds.contains(guild.getIdLong()))
            return false;

        final Bson serverConfigFilter = GuildConfigCommand.getFilter(guild);
        final GuildConfig config = GuildConfigCommand.get(serverConfigFilter, guild);
        return config.isLevellingEnabled();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isSystem() || event.getAuthor().isBot()) return;

        final Guild guild = event.getGuild();

        final Bson serverConfigFilter = GuildConfigCommand.getFilter(guild);
        final GuildConfig config = GuildConfigCommand.get(serverConfigFilter, guild);

        final List<Long> disabledChannels = GuildConfig.getChannels(config.getDisabledLevellingChannels());
        if (!config.isLevellingEnabled() || disabledChannels.contains(event.getChannel().getIdLong())) return;
        if(event.getChannel().getType().isThread() && disabledChannels.contains(event.getChannel().asThreadChannel().getParentChannel().getIdLong())) return;

        Member member = event.getMember();
        if (member == null || !cooldown(guild, member, config)) return;

        int xp = addXP(guild, member.getUser(),
                ThreadLocalRandom.current().nextInt(config.getMinXP(), config.getMaxXP()),
                new LevelUpMessage(guild, Optional.of(event.getMessage())));
        if (xp <= 0) return;

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
    }

    private Levelling getProfile(Guild guild, Member member) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", member.getIdLong()));
        Levelling userProfile = Database.getDatabase().levelling.find(filter).first();
        if (userProfile == null) {
            userProfile = new Levelling(guild.getIdLong(), member.getIdLong());
            Database.getDatabase().levelling.insertOne(userProfile);
        }

        return userProfile;
    }

    public int addXP(Guild guild, User user, int xp) {
        return addXP(guild, user, xp, null);
    }

    public int addXP(Guild guild, User user, int xp, @Nullable LevelUpMessage levelUpMessage) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));

        Member member = guild.getMember(user);
        if (member == null)
            return -1;

        final Levelling userProfile = getProfile(guild, member);
        final List<Bson> updates = new ArrayList<>();

        final int level = userProfile.getLevel();
        int currentXP = userProfile.getXp();
        currentXP += xp;

        userProfile.setXp(currentXP);
        updates.add(Updates.set("xp", currentXP));

        final int newLevel = getLevelForXP(currentXP);
        if (newLevel > level) {
            userProfile.setLevel(newLevel);
            updates.add(Updates.set("level", newLevel));

            if (levelUpMessage != null) {
                final Bson serverConfigFilter = GuildConfigCommand.getFilter(guild);
                final GuildConfig config = GuildConfigCommand.get(serverConfigFilter, guild);

                sendLevelUpMessage(config, guild.getMember(user), newLevel, levelUpMessage.sendTo().orElse(null));
            }
        }

        Database.getDatabase().levelling.updateOne(filter, updates);
        updateLevelRoles(guild, member, newLevel);
        return currentXP;
    }

    public void removeXP(Guild guild, User toWarn, int xp) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", toWarn.getIdLong()));

        Member member = guild.getMember(toWarn);
        if (member == null)
            return;

        final Levelling userProfile = getProfile(guild, member);
        final List<Bson> updates = new ArrayList<>();

        final int level = userProfile.getLevel();
        int currentXP = userProfile.getXp();
        currentXP -= xp;

        if (currentXP < 0) {
            currentXP = 0;
        }

        userProfile.setXp(currentXP);
        updates.add(Updates.set("xp", currentXP));

        final int newLevel = getLevelForXP(currentXP);
        if (newLevel < level) {
            userProfile.setLevel(newLevel);
            updates.add(Updates.set("level", newLevel));
        }

        Database.getDatabase().levelling.updateOne(filter, updates);
        updateLevelRoles(guild, member, newLevel);
    }

    private boolean cooldown(Guild guild, Member member, GuildConfig config) {
        this.cooldownMap.computeIfAbsent(guild.getIdLong(), id -> new ConcurrentHashMap<>());
        final Map<Long, Long> cooldowns = this.cooldownMap.get(guild.getIdLong());

        if (cooldowns.containsKey(member.getIdLong()) && cooldowns.get(member.getIdLong()) > 0) return false;

        cooldowns.put(member.getIdLong(), config.getLevelCooldown());
        return true;
    }

    private void updateLevelRoles(Guild guild, Member member, int level) {
        final Bson serverConfigFilter = GuildConfigCommand.getFilter(guild);
        final GuildConfig config = GuildConfigCommand.get(serverConfigFilter, guild);
        final var userRoles = member.getRoles().stream().map(Role::getIdLong).collect(Collectors.toSet());
        final var levelRoles = getLevelRoles(config);
        final var toAddRoles = levelRoles.entrySet().stream().filter(it -> it.getKey() <= level)
                                         .filter(it -> !userRoles.contains(it.getValue())).map(Map.Entry::getValue)
                                         .map(guild::getRoleById)
                                         .filter(Objects::nonNull).toList();
        guild.modifyMemberRoles(member, toAddRoles, null).queue();
    }

    private void sendLevelUpMessage(GuildConfig config, Member member, int level, @Nullable Message replyTo) {
        if (config.isDisableLevelUpMessages()) return;

        final var description = new StringBuilder(member.getAsMention() + ", you are now Level " + level + "! 🎉");

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setDescription(description);
        embed.setColor(Color.BLUE);

        if (!config.isHasLevelUpChannel() && replyTo != null) {
            if (config.isShouldEmbedLevelUpMessage()) {
                replyTo.replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
            } else {
                replyTo.reply(description).mentionRepliedUser(false).queue();
            }
        } else {
            final TextChannel channel = member.getJDA().getTextChannelById(config.getLevelUpMessageChannel());
            if (channel == null && replyTo != null) {
                if (config.isShouldEmbedLevelUpMessage()) {
                    replyTo.replyEmbeds(embed.build()).mentionRepliedUser(false).queue();
                } else {
                    replyTo.reply(description).mentionRepliedUser(false).queue();
                }
            } else if (config.isShouldEmbedLevelUpMessage() && channel != null) {
                channel.sendMessageEmbeds(embed.build()).mentionRepliedUser(false).queue();
            } else if (channel != null) {
                channel.sendMessage(description).mentionRepliedUser(false).queue();
            }
        }
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

        if (config.getLevelRoles().isEmpty()) return Map.of();

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

    public void setXP(Guild guild, User user1, int amount) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user1.getIdLong()));

        Member member = guild.getMember(user1);
        if (member == null)
            return;

        final Levelling userProfile = getProfile(guild, member);
        final List<Bson> updates = new ArrayList<>();

        final int level = userProfile.getLevel();

        userProfile.setXp(amount);
        updates.add(Updates.set("xp", amount));

        final int newLevel = getLevelForXP(amount);
        if (newLevel > level) {
            userProfile.setLevel(newLevel);
            updates.add(Updates.set("level", newLevel));
        }

        Database.getDatabase().levelling.updateOne(filter, updates);
        updateLevelRoles(guild, member, newLevel);
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

    public record LevelUpMessage(Guild guild, Optional<Message> sendTo) {
    }
}
