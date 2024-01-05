package dev.darealturtywurty.superturtybot.core;

import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.JDA;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShutdownHooks {
    private static final Set<Runnable> HOOKS = new HashSet<>();
    
    public static void register(Runnable shutdown) {
        HOOKS.add(shutdown);
    }
    
    public static void shutdown(JDA jda) {
        jda.shutdown();
        HOOKS.forEach(Runnable::run);
    }

    @SuppressWarnings("resource")
    public static void shutdownOkHttpClient(OkHttpClient client) {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        try {
            var cache = client.cache();
            if (cache != null) {
                cache.close();
            }
        } catch (final IOException | NullPointerException exception) {
            Constants.LOGGER.error("Failed to close OkHttpClient cache!", exception);
        }
    }
}
