package dev.darealturtywurty.superturtybot.commands.music;

import com.mongodb.client.model.Filters;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.TrackData;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.youtube.YouTubeSearchResult;
import dev.darealturtywurty.superturtybot.commands.music.manager.handler.YoutubeResultsCallback;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.Request;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
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

        if(Environment.INSTANCE.youtubeApiKey().isEmpty()) {
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
        }).exceptionally(throwable -> {
            event.replyChoices().queue();
            Constants.LOGGER.error("Failed to search youtube!", throwable);
            return null;
        });
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (Objects.requireNonNull(event.getMember()).getVoiceState() == null || !event.getMember().getVoiceState()
                .inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        if (!event.getGuild().getAudioManager().isConnected()) {
            event.getGuild().getAudioManager().openAudioConnection(channel);
            event.getChannel().sendMessage("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
                    .queue();
        }

        if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to play a song!", false, true);
            return;
        }

        if(AudioManager.isGuessSongRunning(event.getGuild())) {
            reply(event, "❌ A guess the song game is currently running!", false, true);
            return;
        }

        event.deferReply().queue();

        List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());

        if(!queue.isEmpty()) {
            GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", event.getGuild().getIdLong())).first();
            if (config == null) {
                config = new GuildData(event.getGuild().getIdLong());
                Database.getDatabase().guildData.insertOne(config);
            }

            long songsForUser = 0;
            AudioTrack currentTrack = AudioManager.getCurrentlyPlaying(event.getGuild());
            if(currentTrack != null) {
                TrackData data = currentTrack.getUserData(TrackData.class);
                if(data != null && data.getUserId() == event.getUser().getIdLong()) {
                    songsForUser++;
                }
            }

            if(queue.size() > config.getMaxSongsPerUser()) {
                songsForUser += queue.stream().filter(track -> {
                    TrackData data = track.getUserData(TrackData.class);
                    return data != null && data.getUserId() == event.getUser().getIdLong();
                }).count();
            }

            if(songsForUser >= config.getMaxSongsPerUser()) {
                event.getHook().editOriginal("❌ You have reached the maximum amount of songs you can queue!").queue();
                return;
            }
        }

        final String search = event.getOption("search_term", "", OptionMapping::getAsString).trim();
        final CompletableFuture<Pair<Boolean, String>> future = AudioManager.play(channel,
                event.getChannel().asTextChannel(), search, event.getUser());
        future.thenAccept(pair -> event.getHook().editOriginal(switch (pair.getValue()) {
            case "load_failed" ->
                    "❌ This track has failed to load. Please refer to the above message for more information!";
            case "playlist_loaded", "track_loaded" -> "✅ Successfully added to the queue!";
            default -> "❌ An unknown error has occurred. Please notify the bot owner as this should not be possible!";
        }).mentionRepliedUser(false).queue());
    }

    public static CompletableFuture<List<YouTubeSearchResult>> youtubeSearch(String term) {
        final CompletableFuture<List<YouTubeSearchResult>> future = new CompletableFuture<>();

        if(Environment.INSTANCE.youtubeApiKey().isEmpty()) {
            future.completeExceptionally(new IllegalStateException("Youtube API key is not set!"));
            Constants.LOGGER.error("❌ Youtube API key is not set!");
            return future;
        }

        String url = YOUTUBE_SEARCH_API.formatted(Environment.INSTANCE.youtubeApiKey().get(), term);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Constants.HTTP_CLIENT.newCall(request).enqueue(new YoutubeResultsCallback(future));
        return future;
    }
}