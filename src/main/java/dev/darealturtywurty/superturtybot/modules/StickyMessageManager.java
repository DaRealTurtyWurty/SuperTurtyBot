package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class StickyMessageManager extends ListenerAdapter {
    private static final long REPOST_DEBOUNCE_MILLIS = 2500L;
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, DebounceState> DEBOUNCE_STATES = new ConcurrentHashMap<>();
    private static final Map<String, Long> ACTIVE_STICKY_MESSAGES = new ConcurrentHashMap<>();
    public static final StickyMessageManager INSTANCE = new StickyMessageManager();

    private StickyMessageManager() {
        ShutdownHooks.register(SCHEDULER::shutdown);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem())
            return;

        StickyMessage sticky = getSticky(event.getGuild().getIdLong(), event.getChannel().getIdLong());
        if (sticky == null)
            return;

        final String key = createKey(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        // Ignore the sticky post itself so reposting doesn't cause a loop.
        if (sticky.getPostedMessage() == event.getMessageIdLong()
                || event.getMessageIdLong() == ACTIVE_STICKY_MESSAGES.getOrDefault(key, 0L))
            return;

        scheduleRepost(event.getGuild(), event.getChannel().asGuildMessageChannel().getIdLong());
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild())
            return;

        StickyMessage sticky = getSticky(event.getGuild().getIdLong(), event.getChannel().getIdLong());
        if (sticky == null || sticky.getPostedMessage() != event.getMessageIdLong())
            return;

        ACTIVE_STICKY_MESSAGES.remove(createKey(sticky.getGuild(), sticky.getChannel()));
        Database.getDatabase().stickyMessages.updateOne(
                Filters.and(Filters.eq("guild", sticky.getGuild()), Filters.eq("channel", sticky.getChannel())),
                Updates.set("postedMessage", 0L));
    }

    public static StickyMessage getSticky(long guildId, long channelId) {
        return Database.getDatabase().stickyMessages.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("channel", channelId))).first();
    }

    public static void saveSticky(StickyMessage sticky) {
        StickyMessage existing = getSticky(sticky.getGuild(), sticky.getChannel());
        if (existing != null && sticky.getPostedMessage() == 0L) {
            sticky.setPostedMessage(existing.getPostedMessage());
        }

        sticky.setUpdatedAt(System.currentTimeMillis());
        Database.getDatabase().stickyMessages.replaceOne(
                Filters.and(Filters.eq("guild", sticky.getGuild()), Filters.eq("channel", sticky.getChannel())),
                sticky,
                new ReplaceOptions().upsert(true));
    }

    public static boolean clearSticky(Guild guild, long channelId) {
        StickyMessage sticky = getSticky(guild.getIdLong(), channelId);
        if (sticky == null)
            return false;

        cancelPendingRepost(guild.getIdLong(), channelId);
        ACTIVE_STICKY_MESSAGES.remove(createKey(guild.getIdLong(), channelId));
        deletePostedSticky(guild, channelId, sticky.getPostedMessage());
        Database.getDatabase().stickyMessages.deleteOne(Filters.and(
                Filters.eq("guild", guild.getIdLong()),
                Filters.eq("channel", channelId)));
        return true;
    }

    public static void repostSticky(Guild guild, GuildMessageChannel channel, StickyMessage sticky) {
        cancelPendingRepost(guild.getIdLong(), channel.getIdLong());
        deletePostedSticky(guild, channel.getIdLong(), sticky.getPostedMessage());
        postSticky(guild, channel, sticky);
    }

    private static void scheduleRepost(Guild guild, long channelId) {
        final String key = createKey(guild.getIdLong(), channelId);
        DebounceState state = DEBOUNCE_STATES.computeIfAbsent(key, ignored -> new DebounceState());
        long version;
        synchronized (state) {
            state.lastMessageAt = System.currentTimeMillis();
            version = ++state.version;
            if (state.future != null) {
                state.future.cancel(false);
            }

            state.future = scheduleFlush(guild, channelId, key, state, version, REPOST_DEBOUNCE_MILLIS);
        }
    }

    private static void cancelPendingRepost(long guildId, long channelId) {
        final String key = createKey(guildId, channelId);
        DebounceState state = DEBOUNCE_STATES.remove(key);
        if (state == null)
            return;

        synchronized (state) {
            state.version++;
            if (state.future != null) {
                state.future.cancel(false);
                state.future = null;
            }
        }
    }

    private static String createKey(long guildId, long channelId) {
        return guildId + ":" + channelId;
    }

    private static ScheduledFuture<?> scheduleFlush(Guild guild, long channelId, String key, DebounceState state, long version, long delayMillis) {
        return SCHEDULER.schedule(() -> flushRepost(guild, channelId, key, state, version), delayMillis, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static void flushRepost(Guild guild, long channelId, String key, DebounceState state, long expectedVersion) {
        try {
            long remainingDelay;
            synchronized (state) {
                if (state.version != expectedVersion) {
                    return;
                }

                remainingDelay = REPOST_DEBOUNCE_MILLIS - (System.currentTimeMillis() - state.lastMessageAt);
                if (remainingDelay > 0L) {
                    state.future = scheduleFlush(guild, channelId, key, state, expectedVersion, remainingDelay);
                    return;
                }

                state.future = null;
            }

            StickyMessage sticky = getSticky(guild.getIdLong(), channelId);
            if (sticky == null) {
                DEBOUNCE_STATES.remove(key, state);
                return;
            }

            var guildChannel = guild.getGuildChannelById(channelId);
            if (!(guildChannel instanceof GuildMessageChannel messageChannel)) {
                DEBOUNCE_STATES.remove(key, state);
                return;
            }

            repostSticky(guild, messageChannel, sticky);
        } catch (Exception exception) {
            Constants.LOGGER.warn("Failed to repost sticky in channel {}", channelId, exception);
        } finally {
            synchronized (state) {
                if (state.version == expectedVersion && state.future == null) {
                    DEBOUNCE_STATES.remove(key, state);
                }
            }
        }
    }

    private static void postSticky(Guild guild, GuildMessageChannel channel, StickyMessage sticky) {
        if (sticky.hasEmbed()) {
            EmbedBuilder embed = EmbedBuilder.fromData(DataObject.fromJson(sticky.getEmbed()));
            channel.sendMessageEmbeds(embed.build()).queue(
                    message -> updatePostedMessage(guild.getIdLong(), channel.getIdLong(), message),
                    failure -> Constants.LOGGER.warn("Failed to post sticky embed in channel {}", channel.getIdLong(), failure));
            return;
        }

        if (sticky.hasText()) {
            channel.sendMessage(sticky.getContent()).queue(
                    message -> updatePostedMessage(guild.getIdLong(), channel.getIdLong(), message),
                    failure -> Constants.LOGGER.warn("Failed to post sticky message in channel {}", channel.getIdLong(), failure));
        }
    }

    private static void updatePostedMessage(long guildId, long channelId, Message message) {
        ACTIVE_STICKY_MESSAGES.put(createKey(guildId, channelId), message.getIdLong());
        Database.getDatabase().stickyMessages.updateOne(
                Filters.and(Filters.eq("guild", guildId), Filters.eq("channel", channelId)),
                Updates.combine(
                        Updates.set("postedMessage", message.getIdLong()),
                        Updates.set("updatedAt", System.currentTimeMillis())));
    }

    private static void deletePostedSticky(Guild guild, long channelId, long postedMessageId) {
        if (postedMessageId == 0L)
            return;

        var guildChannel = guild.getGuildChannelById(channelId);
        if (!(guildChannel instanceof GuildMessageChannel messageChannel))
            return;

        ACTIVE_STICKY_MESSAGES.remove(createKey(guild.getIdLong(), channelId), postedMessageId);
        messageChannel.deleteMessageById(postedMessageId).queue(
                _ -> {},
                failure -> Constants.LOGGER.debug("Failed to delete old sticky message {}", postedMessageId, failure));
    }

    private static final class DebounceState {
        private long version;
        private long lastMessageAt;
        private ScheduledFuture<?> future;
    }
}
