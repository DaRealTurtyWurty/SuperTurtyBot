package dev.darealturtywurty.superturtybot.commands.music.manager.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube.YouTubeSearchResult;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class YoutubeResultsCallback implements Callback {
    private final CompletableFuture<List<YouTubeSearchResult>> future;

    public YoutubeResultsCallback(CompletableFuture<List<YouTubeSearchResult>> future) {
        this.future = future;
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException exception) {
        this.future.completeExceptionally(exception);
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (!response.isSuccessful() || responseBody == null) {
            this.future.completeExceptionally(new IllegalStateException("Failed to search youtube! Response was not successful!"));
            if (responseBody != null) {
                responseBody.close();
                Constants.LOGGER.error("Failed to search youtube! Response was empty.");
            }

            return;
        }

        final String body = responseBody.string();
        if (body.isBlank()) {
            this.future.completeExceptionally(new IllegalStateException("Failed to search youtube! Response body was blank!"));
            responseBody.close();
            return;
        }

        JsonObject json = Constants.GSON.fromJson(body, JsonObject.class);
        if (!json.has("items")) {
            this.future.completeExceptionally(new IllegalStateException("Failed to search youtube! Response body did not contain items!"));
            responseBody.close();
            return;
        }

        List<YouTubeSearchResult> items = json.getAsJsonArray("items")
                .asList()
                .stream()
                .map(JsonElement::getAsJsonObject)
                .map(YouTubeSearchResult::fromJson)
                .toList();

        this.future.complete(items);
        responseBody.close();
    }
}
