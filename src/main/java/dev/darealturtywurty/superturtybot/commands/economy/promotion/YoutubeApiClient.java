package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record YoutubeApiClient(String apiKey, String videosApiUrl) {

    public VideoFetchResult fetchVideos(List<String> videoIds) throws IOException {
        HttpUrl url = HttpUrl.parse(videosApiUrl).newBuilder()
                .addQueryParameter("part", "snippet,statistics,contentDetails")
                .addQueryParameter("id", String.join(",", videoIds))
                .addQueryParameter("key", apiKey)
                .build();

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IOException("Youtube API response was unsuccessful: " + response.code());

            ResponseBody body = response.body();
            if (body == null)
                throw new IOException("Youtube API response body was empty.");

            JsonObject json = Constants.GSON.fromJson(body.string(), JsonObject.class);
            JsonArray items = json.getAsJsonArray("items");
            if (items == null || items.isEmpty())
                return new VideoFetchResult(List.of(), new HashSet<>(videoIds));

            List<YoutubeVideo> videos = new ArrayList<>();
            Set<String> foundIds = new HashSet<>();
            for (JsonElement element : items) {
                JsonObject item = element.getAsJsonObject();
                String id = getAsString(item, "id");
                if (id == null || id.isBlank())
                    continue;
                foundIds.add(id);

                JsonObject snippet = item.getAsJsonObject("snippet");
                JsonObject statistics = item.getAsJsonObject("statistics");
                JsonObject contentDetails = item.getAsJsonObject("contentDetails");
                if (snippet == null || statistics == null)
                    continue;

                YoutubeVideo video = parseVideo(id, snippet, statistics, contentDetails);
                if (video != null)
                    videos.add(video);
            }

            Set<String> missing = new HashSet<>(videoIds);
            missing.removeAll(foundIds);
            return new VideoFetchResult(videos, missing);
        }
    }

    private static YoutubeVideo parseVideo(String id, JsonObject snippet, JsonObject statistics,
                                           JsonObject contentDetails) {
        String title = getAsString(snippet, "title");
        if (title == null || title.isBlank())
            return null;

        String publishedAtRaw = getAsString(snippet, "publishedAt");
        Instant publishedAt;
        try {
            publishedAt = publishedAtRaw == null ? null : Instant.parse(publishedAtRaw);
        } catch (Exception exception) {
            publishedAt = null;
        }

        if (publishedAt == null)
            return null;

        String liveState = getAsString(snippet, "liveBroadcastContent");
        boolean isLive = liveState != null && !"none".equalsIgnoreCase(liveState);

        String thumbnailUrl = extractThumbnailUrl(snippet.getAsJsonObject("thumbnails"));
        long viewCount = getAsLong(statistics, "viewCount");
        long likeCount = getAsLong(statistics, "likeCount");
        long durationSeconds = parseDurationSeconds(contentDetails);

        return new YoutubeVideo(id, title, thumbnailUrl, viewCount, likeCount, publishedAt, durationSeconds, isLive);
    }

    private static String extractThumbnailUrl(JsonObject thumbnails) {
        if (thumbnails == null)
            return null;

        String[] keys = {"high", "medium", "default"};
        for (String key : keys) {
            JsonObject thumb = thumbnails.getAsJsonObject(key);
            if (thumb != null && thumb.has("url"))
                return thumb.get("url").getAsString();
        }
        return null;
    }

    private static long parseDurationSeconds(JsonObject contentDetails) {
        if (contentDetails == null)
            return 0L;

        String raw = getAsString(contentDetails, "duration");
        if (raw == null || raw.isBlank())
            return 0L;

        try {
            return Duration.parse(raw).getSeconds();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private static String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key))
            return null;

        return object.get(key).getAsString();
    }

    private static long getAsLong(JsonObject object, String key) {
        if (object == null || !object.has(key))
            return 0L;

        try {
            return object.get(key).getAsLong();
        } catch (Exception exception) {
            return 0L;
        }
    }

    public record VideoFetchResult(List<YoutubeVideo> videos, Set<String> missingIds) {
    }
}
