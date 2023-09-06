package dev.darealturtywurty.superturtybot.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.GLA;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import okhttp3.OkHttpClient;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class Constants {
    public static final Logger LOGGER = LoggerFactory.getLogger(TurtyBot.class);
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final String BEAN_DUMPY_URL = "https://media.discordapp.net/attachments/855162784924434442/859517109725954048/dumpy.gif";
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(UrlValidator.ALLOW_2_SLASHES);
    public static final GLA GENIUS_LYRICS = new GLA();

    static {
        ShutdownHooks.register(() -> ShutdownHooks.shutdownOkHttpClient(HTTP_CLIENT));
    }
    
    private Constants() {
        throw new IllegalAccessError("This is illegal, expect police at your door in 2-5 minutes!");
    }
}
