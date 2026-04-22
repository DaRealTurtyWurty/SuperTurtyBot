package dev.darealturtywurty.superturtybot.weblisteners.social;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class NotifierDeliverySupport {
    private static final Duration FAILURE_LOG_COOLDOWN = Duration.ofMinutes(15);
    private static final Map<String, Instant> RECENT_FAILURE_LOGS = new ConcurrentHashMap<>();

    private NotifierDeliverySupport() {
    }

    public static @Nullable StandardGuildMessageChannel resolveChannel(Guild guild, long channelId, String sourceName) {
        StandardGuildMessageChannel channel = guild.getChannelById(StandardGuildMessageChannel.class, channelId);
        if (channel == null) {
            logFailure(sourceName, guild.getIdLong(), channelId, "missing-channel", null,
                    "Skipping {} notifier for guild {} because channel {} is not a text or announcement channel, or no longer exists.",
                    sourceName, guild.getIdLong(), channelId);
            return null;
        }

        if (!channel.canTalk()) {
            logFailure(sourceName, guild.getIdLong(), channelId, "cannot-talk", null,
                    "Skipping {} notifier for guild {} because the bot cannot send messages in channel {}.",
                    sourceName, guild.getIdLong(), channelId);
            return null;
        }

        if (!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            logFailure(sourceName, guild.getIdLong(), channelId, "missing-embed-links", null,
                    "Skipping {} notifier for guild {} because the bot is missing {} in channel {}.",
                    sourceName, guild.getIdLong(), Permission.MESSAGE_EMBED_LINKS, channelId);
            return null;
        }

        return channel;
    }

    public static boolean sendAndWait(RestAction<?> action, String sourceName, StandardGuildMessageChannel channel) {
        try {
            action.submit().join();
            return true;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            logFailure(sourceName, channel.getGuild().getIdLong(), channel.getIdLong(), "send-failure", cause,
                    "Failed to send {} notifier update for guild {} in channel {}.",
                    sourceName, channel.getGuild().getIdLong(), channel.getIdLong());
            return false;
        } catch (Exception exception) {
            logFailure(sourceName, channel.getGuild().getIdLong(), channel.getIdLong(), "send-failure", exception,
                    "Failed to send {} notifier update for guild {} in channel {}.",
                    sourceName, channel.getGuild().getIdLong(), channel.getIdLong());
            return false;
        }
    }

    private static void logFailure(String sourceName, long guildId, long channelId, String reason, Throwable throwable,
                                   String message, Object... arguments) {
        String key = sourceName + "|" + guildId + "|" + channelId + "|" + reason;
        Instant now = Instant.now();
        Instant previous = RECENT_FAILURE_LOGS.get(key);
        if (previous != null && Duration.between(previous, now).compareTo(FAILURE_LOG_COOLDOWN) < 0)
            return;

        RECENT_FAILURE_LOGS.put(key, now);
        if (throwable == null) {
            Constants.LOGGER.warn(message, arguments);
        } else {
            Object[] extendedArguments = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, extendedArguments, 0, arguments.length);
            extendedArguments[arguments.length] = throwable;
            Constants.LOGGER.warn(message, extendedArguments);
        }
    }
}
