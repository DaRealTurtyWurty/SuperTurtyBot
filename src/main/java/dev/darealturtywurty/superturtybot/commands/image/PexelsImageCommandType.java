package dev.darealturtywurty.superturtybot.commands.image;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.text.WordUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class PexelsImageCommandType extends ImageCommandType {
    private static final String BASE_URL = "https://api.pexels.com/v1/";
    
    private String searchTerm;
    private final int maxPages;

    public PexelsImageCommandType(int maxPages) {
        super(createRunner());
        this.maxPages = maxPages;
    }

    public PexelsImageCommandType(int maxPages, String term) {
        this(maxPages);
        this.searchTerm = term;
    }

    @Override
    public Registerable setName(String name) {
        if (this.searchTerm == null || this.searchTerm.isBlank()) {
            this.searchTerm = name;
            return this;
        }

        return super.setName(name);
    }
    
    public static CompletableFuture<List<JsonObject>> getPhotos(String search, int maxPages) {
        if(Environment.INSTANCE.pexelsKey().isEmpty()) {
            Constants.LOGGER.warn("Pexels API key has not been set!");
            return CompletableFuture.completedFuture(null);
        }

        final var future = new CompletableFuture<List<JsonObject>>();
        try {
            final List<JsonObject> results = new ArrayList<>();
            String url = BASE_URL + "search?query=" + search + "&per_page=80";
            int count = 0;
            while (url != null) {
                final URLConnection connection = new URI(url).toURL().openConnection();
                connection.addRequestProperty("Authorization", Environment.INSTANCE.pexelsKey().get());
                
                JsonObject response;
                try {
                    response = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                        JsonObject.class);
                } catch (final IOException exception) {
                    continue;
                }
                
                if (response.has("photos")) {
                    count++;
                    final JsonArray photos = response.getAsJsonArray("photos");
                    for (final JsonElement photoElement : photos) {
                        results.add(photoElement.getAsJsonObject());
                    }
                }
                
                if (response.has("next_page")) {
                    url = response.get("next_page").getAsString();
                } else {
                    url = null;
                }
                
                if (count > maxPages) {
                    url = null;
                }
            }
            
            future.complete(results);
            return future;
        } catch (final IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Error getting photos for {}", search, exception);
            future.complete(null);
            return future;
        }
    }
    
    public static CompletableFuture<String> getRandomPhoto(String search, int maxPages) {
        final CompletableFuture<List<JsonObject>> futurePhotos = getPhotos(search, maxPages);
        
        final var futurePhoto = new CompletableFuture<String>();
        futurePhotos.thenAccept(photos -> {
            final JsonObject photo = photos.get(ThreadLocalRandom.current().nextInt(photos.size()));
            futurePhoto.complete(photo.getAsJsonObject("src").get("original").getAsString());
        });
        
        return futurePhoto;
    }
    
    private static BiConsumer<SlashCommandInteractionEvent, ImageCommandType> createRunner() {
        return (event, cmd) -> {
            if(Environment.INSTANCE.pexelsKey().isEmpty()) {
                event.reply("❌ This command has been disabled by the bot owner!").mentionRepliedUser(false).queue();
                Constants.LOGGER.warn("Pexels API key has not been set!");
                return;
            }

            final PexelsImageCommandType pexelsCmd = (PexelsImageCommandType) cmd;
            event.deferReply().setContent("Loading " + WordUtils.capitalize(pexelsCmd.searchTerm) + " image...")
                .mentionRepliedUser(false).queue(msg -> {
                    final String search = URLEncoder.encode(pexelsCmd.searchTerm.trim(), StandardCharsets.UTF_8);
                    final CompletableFuture<String> futurePhoto = getRandomPhoto(search, pexelsCmd.maxPages);
                    futurePhoto.thenAccept(photo -> event.getHook().setEphemeral(false).editOriginal(photo).queue());
                });
        };
    }
}
