package dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube;

import com.google.gson.JsonObject;

public record YouTubeSearchResultSnippet(String publishedAt, String channelId, String title, String description,
                                         YouTubeSearchResultSnippetThumbnails thumbnails, String channelTitle,
                                         String liveBroadcastContent, String publishTime) {
    public static YouTubeSearchResultSnippet fromJson(JsonObject obj) {
        String publishedAt = obj.get("publishedAt").getAsString();
        String channelId = obj.get("channelId").getAsString();
        String title = obj.get("title").getAsString();
        String description = obj.get("description").getAsString();
        var thumbnails = YouTubeSearchResultSnippetThumbnails.fromJson(obj.getAsJsonObject("thumbnails"));
        String channelTitle = obj.get("channelTitle").getAsString();
        String liveBroadcastContent = obj.get("liveBroadcastContent").getAsString();
        String publishTime = obj.get("publishTime").getAsString();
        return new YouTubeSearchResultSnippet(publishedAt, channelId, title, description, thumbnails, channelTitle,
                liveBroadcastContent, publishTime);
    }
}