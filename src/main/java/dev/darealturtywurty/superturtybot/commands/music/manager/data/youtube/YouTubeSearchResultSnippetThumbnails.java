package dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube;

import com.google.gson.JsonObject;

import java.util.Optional;

public record YouTubeSearchResultSnippetThumbnails(
        Optional<YouTubeSearchResultSnippetThumbnail> defaultThumbnail,
        Optional<YouTubeSearchResultSnippetThumbnail> medium,
        Optional<YouTubeSearchResultSnippetThumbnail> high) {
    public static YouTubeSearchResultSnippetThumbnails fromJson(JsonObject obj) {
        Optional<YouTubeSearchResultSnippetThumbnail> defaultThumbnail;
        Optional<YouTubeSearchResultSnippetThumbnail> medium;
        Optional<YouTubeSearchResultSnippetThumbnail> high;

        try {
            defaultThumbnail = Optional.of(
                    YouTubeSearchResultSnippetThumbnail.fromJson(obj.getAsJsonObject("default")));
        } catch (Exception exception) {
            defaultThumbnail = Optional.empty();
        }

        try {
            medium = Optional.of(YouTubeSearchResultSnippetThumbnail.fromJson(obj.getAsJsonObject("medium")));
        } catch (Exception exception) {
            medium = Optional.empty();
        }

        try {
            high = Optional.of(YouTubeSearchResultSnippetThumbnail.fromJson(obj.getAsJsonObject("high")));
        } catch (Exception exception) {
            high = Optional.empty();
        }

        return new YouTubeSearchResultSnippetThumbnails(defaultThumbnail, medium, high);
    }
}