package dev.darealturtywurty.superturtybot.core.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandHook;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

// Thanks maty
public class DiscordLogbackAppender extends AppenderBase<ILoggingEvent> {
    public static final org.slf4j.Logger LOG = LoggerFactory.getLogger("DiscordLogbackAppender");
    public static final String POST_URL = "https://discord.com/api/webhooks/%s/%s";
    
    private Layout<ILoggingEvent> layout;
    private URI uri;
    
    public void login(String webhookId, String webhookToken) {
        this.uri = URI.create(POST_URL.formatted(webhookId, webhookToken));
    }
    
    /**
     * Sets the inner {@link Layout}, used for formatting the message to be sent.
     *
     * @param layoutIn The layout
     */
    public void setLayout(final Layout<ILoggingEvent> layoutIn) {
        this.layout = layoutIn;
    }
    
    @Override
    protected void append(final ILoggingEvent eventObject) {
        if (this.uri == null)
            return;

        try {
            final var contentBuf = new StringBuilder();
            escape(getMessageContent(eventObject), contentBuf);

            if (contentBuf.toString().endsWith("Successfully resumed Session!"))
                return;

            final String body = '{' + "\"content\":\"" + contentBuf + "\"," + "\"allowed_mentions\":{\"parse\": []}"
                    + '}';

            Request request = new Request.Builder()
                    .url(this.uri.toURL())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .build();

            // async request
            Constants.HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                    LOG.error("Failed to send message to Discord!", exception);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        LOG.error("Failed to send message to Discord! Response: {}", response.body().string());
                    }
                }
            });
        } catch (final IOException exception) {
            LOG.error("Failed to send message to Discord!", exception);
        }
    }
    
    protected String getMessageContent(final ILoggingEvent event) {
        return this.layout != null ? this.layout.doLayout(event) : event.getFormattedMessage();
    }
    
    public static void setup(Optional<String> webhookId, Optional<String> webhookToken) throws ClassCastException {
        if (webhookId.isEmpty() || webhookToken.isEmpty()) {
            Constants.LOGGER.warn("Webhook ID or Token is empty! Not setting up Discord logging!");
            return;
        }

        if(Environment.INSTANCE.isDevelopment()) {
            Constants.LOGGER.warn("Development environment detected! Not setting up Discord logging!");
            return;
        }

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        final var appender = new DiscordLogbackAppender();
        appender.setContext(context);
        
        final var layout = new DiscordLogbackLayout();
        layout.setContext(context);
        layout.start();
        appender.setLayout(layout);
        
        appender.login(webhookId.get(), webhookToken.get());
        appender.start();
        
        final ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
    }
    
    private static void escape(String s, StringBuilder sb) {
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            final char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '/' -> sb.append("\\/");
                default -> {
                    // Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if (ch <= '\u001F' || ch >= '\u007F' && ch <= '\u009F' || ch >= '\u2000' && ch <= '\u20FF') {
                        final String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        sb.append("0".repeat(4 - ss.length()));
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
    }
}