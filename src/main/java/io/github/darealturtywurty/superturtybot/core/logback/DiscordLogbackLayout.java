package io.github.darealturtywurty.superturtybot.core.logback;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;

// Thanks maty
public class DiscordLogbackLayout extends LayoutBase<ILoggingEvent> {
    /**
     * The emote used when the given {@link Level} does not a corresponding emote in {@link #LEVEL_TO_EMOTE}.
     */
    private static final String UNKNOWN_EMOTE = ":radio_button:";

    /**
     * An {@linkplain Map immutable map} of {@link Level}s to emotes.
     * <p>
     * Used for visual distinction of log messages within the Discord console channel.
     */
    public static final Map<Level, String> LEVEL_TO_EMOTE = Map.of(Level.ERROR, ":red_square:", Level.WARN,
        ":yellow_circle:", Level.INFO, ":white_medium_small_square:", Level.DEBUG, ":large_blue_diamond:", Level.TRACE,
        ":small_orange_diamond:");

    private static final boolean JDA_EXISTS;

    /**
     * The maximum length in characters of a stacktrace printed by this layout. If the stacktrace exceeds this length,
     * the stacktrace is truncated to this length, and a small snippet is appended with information about the truncated
     * portion of the stacktrace.
     */
    private static final int MAXIMUM_STACKTRACE_LENGTH = 1700;
    
    static {
        var jdaExists = true;
        try {
            Class.forName("net.dv8tion.jda.api.Jda");
        } catch (final ClassNotFoundException e) {
            jdaExists = false;
        }
        
        JDA_EXISTS = jdaExists;
    }
    
    /**
     * {@inheritDoc}
     *
     * @param  event the event
     * @return       the string
     */
    @Override
    public String doLayout(final ILoggingEvent event) {
        final StringBuilder builder = new StringBuilder(2000);
        builder.append(LEVEL_TO_EMOTE.getOrDefault(event.getLevel(), UNKNOWN_EMOTE));
        builder.append(" [**").append(event.getLoggerName());
        if (event.getMarkerList() != null && !event.getMarkerList().isEmpty()) {
            builder.append("**/**")
                .append(String.join(",", event.getMarkerList().stream().map(Marker::getName).toList()));
        }

        builder.append("**] - ").append(getFormattedMessage(event)).append(CoreConstants.LINE_SEPARATOR);
        
        if (event.getThrowableProxy() != null) {
            final IThrowableProxy proxy = event.getThrowableProxy();
            builder.append(proxy.getClassName()).append(": ").append(proxy.getMessage())
                .append(CoreConstants.LINE_SEPARATOR);
            
            final StringBuilder stacktrace = buildStacktrace(proxy);
            String stacktraceCutoff = null;
            builder.append("Stacktrace: ");
            if (stacktrace.length() > MAXIMUM_STACKTRACE_LENGTH) {
                stacktraceCutoff = stacktrace.substring(MAXIMUM_STACKTRACE_LENGTH, stacktrace.length());
                stacktrace.delete(MAXIMUM_STACKTRACE_LENGTH, stacktrace.length());
            }
            
            builder.append(CoreConstants.LINE_SEPARATOR).append("```ansi").append(CoreConstants.LINE_SEPARATOR)
                .append(stacktrace).append("```");
            
            if (stacktraceCutoff != null) {
                builder.append("*Too long to fully display. ").append(stacktraceCutoff.length())
                    .append(" characters or ").append(stacktraceCutoff.lines().count())
                    .append(" lines were truncated.*");
            }
        }
        return builder.toString();
    }
    
    private StringBuilder buildStacktrace(IThrowableProxy exception) {
        final var builder = new StringBuilder();
        for (int i = 0; i < exception.getStackTraceElementProxyArray().length; i++) {
            builder.append("\t ").append(exception.getStackTraceElementProxyArray()[i].toString())
                .append(CoreConstants.LINE_SEPARATOR);
        }
        
        return builder;
    }
    
    /**
     * Converts the given {@link ILoggingEvent} into a formatted message string, converting {@link IMentionable}s as
     * needed.
     *
     * @param  event The logging event
     * @return       The formatted message, with replaced mentions
     * @see          #tryFormat(Object) #tryConvertMentionables(Object)
     */
    private String getFormattedMessage(final ILoggingEvent event) {
        final Object[] arguments = event.getArgumentArray();
        if (event.getArgumentArray() != null) {
            final var newArgs = new Object[arguments.length];
            for (var i = 0; i < arguments.length; i++) {
                newArgs[i] = tryFormat(arguments[i]);
            }
            
            return MessageFormatter.arrayFormat(event.getMessage(), newArgs).getMessage();
        }
        
        return event.getFormattedMessage();
    }
    
    private static Object tryFormat(final Object obj) {
        if (JDA_EXISTS) {
            final Object jda = JDAFormatter.format(obj);
            if (jda != null)
                return jda;
        }

        if (obj instanceof final Collection<?> col) {
            final Stream<Object> stream = col.stream().map(DiscordLogbackLayout::tryFormat);
            if (obj instanceof Set)
                return stream.collect(Collectors.toSet());
            
            return stream.collect(Collectors.toList());
            
        }

        if (obj instanceof final Map<?, ?> map)
            return map.entrySet().stream().map(
                entry -> new AbstractMap.SimpleImmutableEntry<>(tryFormat(entry.getKey()), tryFormat(entry.getValue())))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        if (obj instanceof final Map.Entry<?, ?> entry)
            return new AbstractMap.SimpleImmutableEntry<>(tryFormat(entry.getKey()), tryFormat(entry.getValue()));

        return obj;
    }
    
    public static final class JDAFormatter {
        /**
         * Tries to convert the given object (or any contained objects within) to
         * {@linkplain IMentionable#getAsMention() string mentions}.
         * <p>
         * Mentions will consist of the return value of {@link IMentionable#getAsMention()}, along with the
         * {@linkplain net.dv8tion.jda.api.entities.ISnowflake#getIdLong() snowflake ID}* and the name of the object if
         * available.
         * <p>
         * If the object is {@link IMentionable}, cast and convert into mention, then return the new mention.
         *
         * @param  obj The object
         * @return     The converted object, according to the conversion rules
         */
        @Nullable
        public static Object format(final Object obj) {
            if (!(obj instanceof IMentionable))
                return null;
            String name = null;
            if (obj instanceof final User user) {
                name = user.getAsTag();
            } else if (obj instanceof final Role role) {
                name = role.getName();
            } else if (obj instanceof final GuildChannel channel) {
                name = channel.getName();
            } else if (obj instanceof final Emoji emoji) {
                name = emoji.getName();
            }
            
            if (name != null)
                return String.format("%s (%s;`%s`)", ((IMentionable) obj).getAsMention(), name,
                    ((IMentionable) obj).getIdLong());
            
            return String.format("%s (`%s`)", ((IMentionable) obj).getAsMention(), ((IMentionable) obj).getIdLong());
        }
    }
}