package dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record YouTubeSearchResult(String kind, String etag, YouTubeSearchResultId id,
                                  YouTubeSearchResultSnippet snippet) {
    public static YouTubeSearchResult fromJson(JsonObject obj) {
        String kind = obj.get("kind").getAsString();
        String etag = obj.get("etag").getAsString();
        var id = YouTubeSearchResultId.fromJson(obj.getAsJsonObject("id"));
        var snippet = YouTubeSearchResultSnippet.fromJson(obj.getAsJsonObject("snippet"));
        return new YouTubeSearchResult(kind, etag, id, snippet);
    }

    public Command.Choice toChoice() {
        String title = URLDecoder.decode(snippet.title(), StandardCharsets.UTF_8)
                .replace("&amp;", "&");

        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }

        String id = this.id.videoOrChannelOrPlaylistId().left().orElse("dQw4w9WgXcQ");
        return new Command.Choice(title, "https://www.youtube.com/watch?v=" + id);
    }
}