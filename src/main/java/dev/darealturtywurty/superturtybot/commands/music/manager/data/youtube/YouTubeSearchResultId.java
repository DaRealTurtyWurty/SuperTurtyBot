package dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.function.Either3;

public record YouTubeSearchResultId(String kind, Either3<String, String, String> videoOrChannelOrPlaylistId) {
    public static YouTubeSearchResultId fromJson(JsonObject obj) {
        String kind = obj.get("kind").getAsString();
        return switch (kind) {
            case "youtube#video" -> new YouTubeSearchResultId(kind, Either3.left(obj.get("videoId").getAsString()));
            case "youtube#channel" ->
                    new YouTubeSearchResultId(kind, Either3.middle(obj.get("channelId").getAsString()));
            case "youtube#playlist" ->
                    new YouTubeSearchResultId(kind, Either3.right(obj.get("playlistId").getAsString()));
            default -> throw new IllegalStateException("Unknown kind: " + kind);
        };
    }
}