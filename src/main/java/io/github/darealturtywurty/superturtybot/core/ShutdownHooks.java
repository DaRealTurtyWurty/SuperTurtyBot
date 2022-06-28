package io.github.darealturtywurty.superturtybot.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.dv8tion.jda.api.JDA;
import okhttp3.OkHttpClient;

public class ShutdownHooks {
    private static final Set<Runnable> HOOKS = new HashSet<>();

    public static void register(Runnable shutdown) {
        HOOKS.add(shutdown);
    }

    public static void shutdown(JDA jda) {
        jda.shutdown();
        HOOKS.forEach(Runnable::run);
    }
    
    public static void shutdownOkHttpClient(OkHttpClient client) {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        try {
            client.cache().close();
        } catch (final IOException | NullPointerException exception) {
            
        }
    }
}
