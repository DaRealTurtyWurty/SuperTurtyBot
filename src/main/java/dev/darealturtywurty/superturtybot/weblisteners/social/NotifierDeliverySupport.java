package dev.darealturtywurty.superturtybot.weblisteners.social;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class NotifierDeliverySupport {
    private static final Duration FAILURE_LOG_COOLDOWN = Duration.ofMinutes(15);
    private static final Map<String, Instant> RECENT_FAILURE_LOGS = new ConcurrentHashMap<>();

    private NotifierDeliverySupport() {
    }

    public static @Nullable StandardGuildMessageChannel resolveChannel(Guild guild, NotifierDiagnostics.Key key) {
        String problem = NotifierDiagnostics.channelProblem(guild, key.channel());
        if (problem != null) {
            NotifierDiagnostics.deliveryFailed(key, problem);
            logFailure(key.type(), key.guild(), key.channel(), "channel-permissions", null,
                "Skipping {} notifier for guild {} in channel {}: {}", key.type(), key.guild(), key.channel(), problem);
            return null;
        }
        return guild.getChannelById(StandardGuildMessageChannel.class, key.channel());
    }

    public static boolean sendAndWait(RestAction<?> action, NotifierDiagnostics.Key key, String item) {
        try {
            action.timeout(15, TimeUnit.SECONDS).complete();
            NotifierDiagnostics.delivered(key, item);
            return true;
        } catch (RuntimeException exception) {
            NotifierDiagnostics.deliveryFailed(key,
                "Discord did not confirm delivery. Check destination permissions and bot connectivity.");
            logFailure(key.type(), key.guild(), key.channel(), "send-failure", exception,
                "Failed to send {} notifier update for guild {} in channel {}.", key.type(), key.guild(),
                key.channel());
            return false;
        }
    }

    public static void send(RestAction<?> action, NotifierDiagnostics.Key key, String item) {
        action.queue(_ -> NotifierDiagnostics.delivered(key, item), exception -> {
            NotifierDiagnostics.deliveryFailed(key,
                "Discord rejected delivery. Check destination permissions and bot connectivity.");
            logFailure(key.type(), key.guild(), key.channel(), "send-failure", exception,
                "Failed to send {} notifier update for guild {} in channel {}.", key.type(), key.guild(),
                key.channel());
        });
    }

    private static void logFailure(
        String sourceName,
        long guildId,
        long channelId,
        String reason,
        Throwable throwable,
        String message,
        Object... arguments
    ) {
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
