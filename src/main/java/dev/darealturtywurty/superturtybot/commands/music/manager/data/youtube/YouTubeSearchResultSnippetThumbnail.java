package dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube;

import com.google.gson.JsonObject;

public record YouTubeSearchResultSnippetThumbnail(String url, int width, int height) {
    public static YouTubeSearchResultSnippetThumbnail fromJson(JsonObject obj) {
        String url = obj.get("url").getAsString();
        int width = obj.get("width").getAsInt();
        int height = obj.get("height").getAsInt();
        return new YouTubeSearchResultSnippetThumbnail(url, width, height);
    }
}