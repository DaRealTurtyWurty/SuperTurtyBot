package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class PexelsImageCommand extends ImageCommand {
    private static final String BASE_URL = "https://api.pexels.com/v1/";
    
    protected PexelsImageCommand(Types types) {
        super(types);
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Gets a random " + getName() + " image";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (this.types.normal()) {
            event.getMessage().reply("Loading " + getName() + " image...").mentionRepliedUser(false).queue(msg -> {
                final String search = URLEncoder.encode(getSearchTerm().trim(), StandardCharsets.UTF_8);
                final CompletableFuture<String> futurePhoto = getRandomPhoto(search, maxPages());
                futurePhoto.thenAccept(photo -> msg.editMessage(photo).mentionRepliedUser(false).queue());
            });
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (this.types.slash()) {
            event.deferReply().setContent("Loading " + getName() + " image...").mentionRepliedUser(false).queue(msg -> {
                final String search = URLEncoder.encode(getSearchTerm().trim(), StandardCharsets.UTF_8);
                final CompletableFuture<String> futurePhoto = getRandomPhoto(search, maxPages());
                futurePhoto.thenAccept(photo -> event.getHook().setEphemeral(false).editOriginal(photo).queue());
            });
        }
    }
    
    abstract String getSearchTerm();
    
    int maxPages() {
        return 5;
    }
    
    public static CompletableFuture<List<JsonObject>> getPhotos(String search, int maxPages) {
        final var future = new CompletableFuture<List<JsonObject>>();
        try {
            final List<JsonObject> results = new ArrayList<>();
            String url = BASE_URL + "search?query=" + search + "&per_page=80";
            int count = 0;
            while (url != null) {
                final URLConnection connection = new URL(url).openConnection();
                connection.addRequestProperty("Authorization", Environment.INSTANCE.pexelsKey());
                
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
        } catch (final IOException exception) {
            exception.printStackTrace();
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
}
