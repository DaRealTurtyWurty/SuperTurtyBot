package dev.darealturtywurty.superturtybot.core.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Setter;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DiscordLogbackAppender extends AppenderBase<ILoggingEvent> {
    public static final String POST_URL = "https://discord.com/api/webhooks/%s/%s";
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Pattern CODE_FENCE = Pattern.compile("(?m)^```[^\\r\\n]{0,32}(?=\\r?$)");

    private final ExecutorService sender = Executors.newSingleThreadExecutor(runnable -> {
        final var thread = new Thread(runnable, "DiscordLogbackAppender");
        thread.setDaemon(true);
        return thread;
    });

    @Setter
    private Layout<ILoggingEvent> layout;
    private URI uri;
    private volatile boolean webhookUnavailable;

    public void login(String webhookId, String webhookToken) {
        this.uri = URI.create(POST_URL.formatted(webhookId, webhookToken));
        this.webhookUnavailable = false;
    }

    @Override
    protected void append(final ILoggingEvent eventObject) {
        if (this.uri == null || this.webhookUnavailable)
            return;

        final String content = getMessageContent(eventObject);
        if (content == null || content.isBlank() || content.stripTrailing().endsWith("Successfully resumed Session!"))
            return;

        final URI webhook = this.uri;
        final List<String> messages = splitMessage(content);
        this.sender.execute(() -> {
            try {
                for (final String message : messages) {
                    if (this.webhookUnavailable || !sendMessage(webhook, message))
                        return;
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public synchronized void stop() {
        super.stop();
        this.sender.shutdown();
    }

    private boolean sendMessage(URI webhook, String content) throws InterruptedException {
        final var body = new JsonObject();
        body.addProperty("content", content);
        final var mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        body.add("allowed_mentions", mentions);

        final Request request = new Request.Builder()
            .url(webhook + "?wait=true")
            .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
            .build();

        while (!Thread.currentThread().isInterrupted()) {
            final boolean retry;
            final long delayMillis;
            try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
                retry = response.code() == 429;
                if (!retry && !response.isSuccessful()) {
                    if (response.code() == 404) {
                        this.webhookUnavailable = true;
                    }
                    addWarn("Discord log webhook returned HTTP " + response.code());
                    return false;
                }

                delayMillis = retry
                    ? retryDelayMillis(response)
                    : "0".equals(response.header("X-RateLimit-Remaining"))
                        ? delayMillis(response.header("X-RateLimit-Reset-After"))
                        : 0;
            } catch (final IOException exception) {
                addWarn("Failed to deliver a Discord log message (" + exception.getClass().getSimpleName() + ")");
                return false;
            }

            if (delayMillis > 0) {
                TimeUnit.MILLISECONDS.sleep(delayMillis);
            }
            if (!retry)
                return true;
        }
        return false;
    }

    private static long retryDelayMillis(Response response) throws IOException {
        final String retryAfter = response.header("Retry-After");
        if (retryAfter != null)
            return delayMillis(retryAfter);

        if (response.body() != null) {
            try {
                final JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
                if (body.has("retry_after"))
                    return delayMillis(body.get("retry_after").getAsString());
            } catch (final RuntimeException _) {
            }
        }
        return delayMillis(response.header("X-RateLimit-Reset-After"));
    }

    private static long delayMillis(String seconds) {
        try {
            final double value = Double.parseDouble(seconds);
            if (Double.isFinite(value) && value >= 0)
                return Math.max(1L, (long) Math.ceil(value * 1000));
        } catch (final NumberFormatException | NullPointerException _) {
        }
        return 1000L;
    }

    private static List<String> splitMessage(String content) {
        if (content.length() <= MAX_MESSAGE_LENGTH)
            return List.of(content);

        final var messages = new ArrayList<String>();
        final var fences = CODE_FENCE.matcher(content);
        boolean hasFence = fences.find();
        String openFence = "";
        int offset = 0;
        while (offset < content.length()) {
            final String prefix = openFence.isEmpty() ? "" : openFence + "\n";
            int end = Math.min(content.length(), offset + MAX_MESSAGE_LENGTH - prefix.length() - 4);
            if (end < content.length()) {
                final int newline = content.lastIndexOf('\n', end - 1);
                if (newline >= offset) {
                    end = newline + 1;
                }
                if (Character.isHighSurrogate(content.charAt(end - 1))
                    && Character.isLowSurrogate(content.charAt(end))) {
                    end--;
                }
                if (content.charAt(end - 1) == '\r' && content.charAt(end) == '\n') {
                    end--;
                }
            }

            while (hasFence && fences.start() < end) {
                if (fences.end() > end) {
                    end = fences.start();
                    break;
                }
                if (openFence.isEmpty()) {
                    openFence = fences.group();
                } else if (fences.group().equals("```")) {
                    openFence = "";
                }
                hasFence = fences.find();
            }

            final String suffix = openFence.isEmpty() ? "" : "\n```";
            messages.add(prefix + content.substring(offset, end) + suffix);
            offset = end;
        }
        return messages;
    }

    protected String getMessageContent(final ILoggingEvent event) {
        return this.layout != null ? this.layout.doLayout(event) : event.getFormattedMessage();
    }

    public static void setup(Optional<String> webhookId, Optional<String> webhookToken) throws ClassCastException {
        if (webhookId.isEmpty() || webhookToken.isEmpty()) {
            Constants.LOGGER.warn("Webhook ID or Token is empty! Not setting up Discord logging!");
            return;
        }

        if (Environment.INSTANCE.isDevelopment()) {
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

        Constants.LOGGER.info("Setup logging!");
    }
}
