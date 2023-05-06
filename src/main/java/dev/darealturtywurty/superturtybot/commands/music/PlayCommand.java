package dev.darealturtywurty.superturtybot.commands.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.Either3;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PlayCommand extends CoreCommand {
    private static final String YOUTUBE_SEARCH_API = "https://youtube.googleapis.com/youtube/v3/search?part=snippet&key=%s&q=%s";

    public PlayCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "search_term", "The song that you want to play",
                true).setAutoComplete(true));
    }

    @Override
    public String getAccess() {
        return "Everyone (unless queue is locked)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Plays the supplied music";
    }

    @Override
    public String getHowToUse() {
        return "/play [songName|ytURL|spotifyURL|soundcloudURL]";
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getRichName() {
        return "Play Music";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) return;

        if (!event.getName().equals(getName())) return;

        if (!"search_term".equals(event.getFocusedOption().getName())) {
            event.replyChoices().queue();
            return;
        }

        final String searchTerm = URLEncoder.encode(event.getOption("search_term", "", OptionMapping::getAsString),
                StandardCharsets.UTF_8);
        if (searchTerm.isBlank()) {
            event.replyChoices().queue();
            return;
        }

        CompletableFuture<List<YouTubeSearchResult>> searchResults = youtubeSearch(searchTerm);
        searchResults.thenAcceptAsync(results -> {
            if (results.isEmpty()) {
                event.replyChoices().queue();
                return;
            }

            event.replyChoices(results.stream().map(YouTubeSearchResult::toChoice).toList()).queue();
        });
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        if (Objects.requireNonNull(event.getMember()).getVoiceState() == null || !event.getMember().getVoiceState()
                .inAudioChannel()) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getGuild().getAudioManager().isConnected()) {
            event.getGuild().getAudioManager().openAudioConnection(channel);
            event.getChannel().sendMessage("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
                    .queue();
        }

        if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            event.deferReply(true).setContent("❌ You must be in the same voice channel as me to play a song!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        if(AudioManager.isGuessSongRunning(event.getGuild())) {
            event.deferReply(true).setContent("❌ A guess the song game is currently running!").mentionRepliedUser(false).queue();
            return;
        }

        final String search = event.getOption("search_term", "", OptionMapping::getAsString).trim();
        final CompletableFuture<Pair<Boolean, String>> future = AudioManager.play(channel,
                event.getChannel().asTextChannel(), search, event.getUser());
        future.thenAccept(pair -> event.deferReply(true).setContent(switch (pair.getValue()) {
            case "load_failed" ->
                    "This track has failed to load. Please refer to the above message for more information!";
            case "playlist_loaded", "track_loaded" -> "Successfully added to the queue!";
            default -> "An unknown error has occured. Please notify the bot owner as this should not be possible!";
        }).mentionRepliedUser(false).queue());
    }

    public static CompletableFuture<List<YouTubeSearchResult>> youtubeSearch(String term) {
        final CompletableFuture<List<YouTubeSearchResult>> future = new CompletableFuture<>();
        Constants.HTTP_CLIENT.newCall(
                new Request.Builder().url(YOUTUBE_SEARCH_API.formatted(Environment.INSTANCE.youtubeApiKey(), term))
                        .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException exception) {
                future.completeExceptionally(new IllegalStateException("Failed to search youtube!", exception));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful() || responseBody == null) {
                    future.completeExceptionally(new IllegalStateException("Failed to search youtube!"));
                    return;
                }


                final String body = responseBody.string();
                JsonObject json = Constants.GSON.fromJson(body, JsonObject.class);
                JsonArray items = json.getAsJsonArray("items");
                future.complete(
                        items.asList().stream().map(JsonElement::getAsJsonObject).map(YouTubeSearchResult::fromJson)
                                .toList());
            }
        });

        return future;
    }

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
            return new Command.Choice(snippet.title(),
                    "https://www.youtube.com/watch?v=" + id.videoOrChannelOrPlaylistId().left().orElse("dQw4w9WgXcQ"));
        }
    }

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

    public record YouTubeSearchResultSnippetThumbnails(Optional<YouTubeSearchResultSnippetThumbnail> defaultThumbnail,
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

    public record YouTubeSearchResultSnippetThumbnail(String url, int width, int height) {
        public static YouTubeSearchResultSnippetThumbnail fromJson(JsonObject obj) {
            String url = obj.get("url").getAsString();
            int width = obj.get("width").getAsInt();
            int height = obj.get("height").getAsInt();
            return new YouTubeSearchResultSnippetThumbnail(url, width, height);
        }
    }
}
