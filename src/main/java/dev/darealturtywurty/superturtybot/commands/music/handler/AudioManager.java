package dev.darealturtywurty.superturtybot.commands.music.handler;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.music.handler.filter.FilterChainConfiguration;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.Either;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SavedSongs;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class AudioManager {
    private static final Map<Long, GuildAudioManager> AUDIO_MANAGERS = new HashMap<>();
    public static final AudioPlayerManager AUDIO_MANAGER = new DefaultAudioPlayerManager();
    public static final List<String> GUESS_THE_SONG_TRACKS = new ArrayList<>();

    static {
        // Local Files (mp3, ogg, etc)
        AudioSourceManagers.registerLocalSource(AUDIO_MANAGER);

        // Spotify
        if(Environment.INSTANCE.spotifyID().isPresent() && Environment.INSTANCE.spotifySecret().isPresent()) {
            var spotifyManager = new SpotifySourceManager(null, Environment.INSTANCE.spotifyID().get(),
                    Environment.INSTANCE.spotifySecret().get(), "US", AUDIO_MANAGER);

            spotifyManager.setPlaylistPageLimit(30);
            AUDIO_MANAGER.registerSourceManager(spotifyManager);
        }

        // Clypit, Speech (TTS), PornHub, Reddit, OCRemix, TikTok, Mixcloud, SoundGasm
        DuncteBotSources.registerAll(AUDIO_MANAGER, "en-US");

        // YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, Beam, GetYarn, Http
        final var ytSource = new YoutubeAudioSourceManager(true, null, null);
        ytSource.setPlaylistPageCount(100);
        AUDIO_MANAGER.registerSourceManager(ytSource);
        AudioSourceManagers.registerRemoteSources(AUDIO_MANAGER);

        ShutdownHooks.register(() -> {
            AUDIO_MANAGER.shutdown();
            AUDIO_MANAGERS.values().forEach(manager -> manager.getPlayer().destroy());
        });
    }

    private AudioManager() {
    }

    public static void clear(Guild guild) {
        getOrCreate(guild).getMusicScheduler().clear();
    }

    @Nullable
    public static AudioTrack getCurrentlyPlaying(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().getCurrentlyPlaying();
    }

    public static GuildAudioManager getOrCreate(Guild guild) {
        AUDIO_MANAGERS.computeIfAbsent(guild.getIdLong(), guildId -> {
            final GuildAudioManager manager = new GuildAudioManager(AUDIO_MANAGER);
            guild.getAudioManager().setSendingHandler(manager.getSendHandler());
            return manager;
        });

        return AUDIO_MANAGERS.get(guild.getIdLong());
    }

    public static List<AudioTrack> getQueue(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().getQueue();
    }

    public static int getVolume(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().getVolume();
    }

    public static boolean isPaused(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().isPaused();
    }

    public static boolean isPlaying(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().getCurrentlyPlaying() != null;
    }

    public static void pause(Guild guild) {
        getOrCreate(guild).getMusicScheduler().pause();
    }

    public static boolean addGuessTheSongTrack(Guild guild, AudioTrack track) {
        return getOrCreate(guild).getMusicScheduler().addGuessTheSongTrack(track);
    }

    public static CompletableFuture<Pair<Boolean, String>> play(AudioChannel audioChannel, TextChannel textChannel,
            String toPlay, User user) {
        final GuildAudioManager manager = getOrCreate(audioChannel.getGuild());
        final var future = new CompletableFuture<Pair<Boolean, String>>();
        AUDIO_MANAGER.loadItemOrdered(manager, toPlay, new AudioLoadResultHandler() {
            @Override
            public void loadFailed(FriendlyException exception) {
                exception.printStackTrace();
                textChannel.sendMessage(
                                "I have failed to load this track. Please report the following to the bot owner: \n\nSeverity: "
                                        + exception.severity.name() + "\nTrack Search: " + toPlay + "\nException: "
                                        + exception.getMessage())
                        .queue();
                future.complete(Pair.of(false, "load_failed"));
            }

            @Override
            public void noMatches() {
                if (toPlay.startsWith("ytsearch:")) {
                    textChannel.sendMessage("I have not been able to find any matches for search term: `" + toPlay
                            + "`! If you think that this is not correct, please contact the bot author").queue();
                    future.complete(Pair.of(false, "load_failed"));
                } else {
                    play(audioChannel, textChannel, "ytsearch:" + toPlay, user).thenAccept(future::complete);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                playlist.getTracks().forEach(track -> track.setUserData(new TrackData(user.getIdLong(), toPlay)));

                if (toPlay.startsWith("ytsearch:")) {
                    manager.getMusicScheduler().queue(playlist.getTracks().get(0));

                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.GREEN);
                    embed.setTitle("Added: " + playlist.getTracks().get(0).getInfo().title + " to the queue!",
                            playlist.getTracks().get(0).getInfo().uri.startsWith("http")
                                    ? playlist.getTracks().get(0).getInfo().uri
                                    : null);
                    embed.setThumbnail("http://img.youtube.com/vi/" + playlist.getTracks().get(0).getIdentifier()
                            + "/maxresdefault.jpg");

                    textChannel.sendMessageEmbeds(embed.build()).queue();
                } else {
                    playlist.getTracks().forEach(manager.getMusicScheduler()::queue);

                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.GREEN);
                    embed.setTitle("Added: " + playlist.getTracks().size() + " tracks to the queue!",
                            toPlay.startsWith("http") ? toPlay : null);

                    textChannel.sendMessageEmbeds(embed.build()).queue();
                }

                final AudioTrack track = playlist.getTracks().get(0);
                if (Objects.equals(manager.getMusicScheduler().getCurrentlyPlaying(), track)) {
                    manager.getMusicScheduler().setAudioChannel(audioChannel);

                    final var playingEmbed = new EmbedBuilder();
                    playingEmbed.setTimestamp(Instant.now());
                    playingEmbed.setColor(Color.GREEN);
                    playingEmbed.setTitle("Now Playing: " + track.getInfo().title,
                            track.getInfo().uri.startsWith("http") ? track.getInfo().uri : null);
                    playingEmbed
                            .setThumbnail("http://img.youtube.com/vi/" + track.getIdentifier() + "/maxresdefault.jpg");

                    textChannel.sendMessageEmbeds(playingEmbed.build()).queue();
                }

                future.complete(Pair.of(true, "playlist_loaded"));
            }

            @Override
            public void trackLoaded(AudioTrack track) {
                manager.getMusicScheduler().queue(track);
                track.setUserData(new TrackData(user.getIdLong(), null));

                final AudioTrack nowPlaying = manager.getMusicScheduler().getCurrentlyPlaying();
                if (Objects.equals(nowPlaying, track)) {
                    manager.getMusicScheduler().setAudioChannel(audioChannel);
                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.GREEN);
                    embed.setTitle("Now Playing: " + track.getInfo().title,
                            track.getInfo().uri.startsWith("http") ? track.getInfo().uri : null);
                    embed.setThumbnail("http://img.youtube.com/vi/" + track.getIdentifier() + "/maxresdefault.jpg");

                    textChannel.sendMessageEmbeds(embed.build()).queue();
                } else {
                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.GREEN);
                    embed.setTitle("Added: " + track.getInfo().title + " to the queue!",
                            track.getInfo().uri.startsWith("http") ? track.getInfo().uri : null);
                    embed.setThumbnail("http://img.youtube.com/vi/" + track.getIdentifier() + "/maxresdefault.jpg");

                    textChannel.sendMessageEmbeds(embed.build()).queue();
                }

                future.complete(Pair.of(true, "track_loaded"));
            }
        });

        return future;
    }

    public static void play(Guild guild, AudioChannel channel, File audioFile) {
        final GuildAudioManager manager = getOrCreate(guild);
        AUDIO_MANAGER.loadItemOrdered(manager, audioFile.getAbsolutePath(), new AudioLoadResultHandler() {
            @Override
            public void loadFailed(FriendlyException exception) {
                Constants.LOGGER.error("Track failed to load: {}", exception.getMessage());
            }

            @Override
            public void noMatches() {
                Constants.LOGGER.error("No matches were found for file: '{}'!", audioFile.getAbsolutePath());
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                Constants.LOGGER.error("What the fuck have you done? Playlist: '{}' was loaded!", playlist.getName());
            }

            @Override
            public void trackLoaded(AudioTrack track) {
                manager.getMusicScheduler().setAudioChannel(channel);
                manager.getMusicScheduler().queue(track);
            }
        });
    }

    public static boolean removeTrack(Guild guild, AudioTrack track) {
        return getOrCreate(guild).getMusicScheduler().removeTrack(track);
    }

    @Nullable
    public static AudioTrack removeTrack(Guild guild, int index) {
        return getOrCreate(guild).getMusicScheduler().removeTrack(index);
    }

    public static void resume(Guild guild) {
        getOrCreate(guild).getMusicScheduler().resume();
    }

    public static CompletableFuture<Either<List<AudioTrack>, FriendlyException>> search(Guild guild, String term) {
        final GuildAudioManager manager = getOrCreate(guild);
        final var future = new CompletableFuture<Either<List<AudioTrack>, FriendlyException>>();
        AUDIO_MANAGER.loadItemOrdered(manager, term, new AudioLoadResultHandler() {
            @Override
            public void loadFailed(FriendlyException exception) {
                future.complete(Either.right(exception));
            }

            @Override
            public void noMatches() {
                future.complete(Either.left(List.of()));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                future.complete(Either.left(List.copyOf(playlist.getTracks())));
            }

            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(Either.left(List.of(track)));
            }
        });

        return future;
    }

    public static int setVolume(Guild guild, int volume) {
        getOrCreate(guild).getMusicScheduler().setVolume(volume);
        return getVolume(guild);
    }

    public static void shuffle(Guild guild) {
        getOrCreate(guild).getMusicScheduler().shuffle();
    }

    @Nullable
    public static AudioTrack skip(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().skip();
    }

    public static LoopState getLoopState(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().getLoopState();
    }

    public static void setLoopState(Guild guild, LoopState loopState) {
        getOrCreate(guild).getMusicScheduler().setLoopState(loopState);
    }

    public static LoopState toggleLoopState(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().toggleLoopState();
    }

    public static void restartTrack(Guild guild) {
        getOrCreate(guild).getMusicScheduler().restartTrack();
    }

    public static void seek(Guild guild, int time) {
        getOrCreate(guild).getMusicScheduler().seek(time);
    }

    public static int getTrackPosition(Guild guild, AudioTrack track) {
        List<AudioTrack> queue = getQueue(guild);
        return queue.indexOf(track);
    }

    public static void moveTrack(Guild guild, int from, int to) {
        getOrCreate(guild).getMusicScheduler().moveTrack(from, to);
    }

    public static CompletableFuture<Either<AudioTrack, FriendlyException>> playGuessTheSong(Guild guild, AudioChannel channel, String playlist) {
        CompletableFuture<Either<AudioTrack, FriendlyException>> future = new CompletableFuture<>();

        GuildAudioManager manager = getOrCreate(guild);
        if(GUESS_THE_SONG_TRACKS.isEmpty()) {
            AUDIO_MANAGER.loadItemOrdered(manager, playlist, new AudioLoadResultHandler() {
                @Override
                public void loadFailed(FriendlyException exception) {
                    future.complete(Either.right(exception));
                }

                @Override
                public void noMatches() {
                    future.complete(Either.left(null));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    GUESS_THE_SONG_TRACKS.addAll(playlist.getTracks().stream().map(track -> track.getInfo().uri).toList());

                    List<AudioTrack> tracks = playlist.getTracks();
                    AudioTrack track = tracks.get(ThreadLocalRandom.current().nextInt(tracks.size()));
                    manager.getMusicScheduler().setAudioChannel(channel);
                    addGuessTheSongTrack(guild, track);
                    future.complete(Either.left(track));
                }

                @Override
                public void trackLoaded(AudioTrack track) {
                    manager.getMusicScheduler().setAudioChannel(channel);
                    addGuessTheSongTrack(guild, track);
                    future.complete(Either.left(track));
                }
            });
        } else {
            String url = GUESS_THE_SONG_TRACKS.get(ThreadLocalRandom.current().nextInt(GUESS_THE_SONG_TRACKS.size()));
            AUDIO_MANAGER.loadItemOrdered(manager, url, new AudioLoadResultHandler() {
                @Override
                public void loadFailed(FriendlyException exception) {
                    future.complete(Either.right(exception));
                }

                @Override
                public void noMatches() {
                    future.complete(Either.left(null));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    future.complete(Either.left(null));
                }

                @Override
                public void trackLoaded(AudioTrack track) {
                    manager.getMusicScheduler().setAudioChannel(channel);
                    addGuessTheSongTrack(guild, track);
                    future.complete(Either.left(track));
                }
            });
        }

        return future;
    }

    public static void endGuessTheSong(Guild guild) {
        getOrCreate(guild).getMusicScheduler().endGuessTheSong();
    }

    public static boolean isGuessSongRunning(Guild guild) {
        return getOrCreate(guild).getMusicScheduler().isGuessSongRunning();
    }

    public static FilterChainConfiguration getFilterConfiguration(Guild guild) {
        return getOrCreate(guild).getFilterChainConfiguration();
    }

    public static Pair<Boolean, String> saveSong(User user, String name, String url) {
        if (user.isBot() || user.isSystem())
            return Pair.of(false, "You cannot save songs");

        if (!url.startsWith("http"))
            return Pair.of(false, "URL is invalid");

        try {
            new URL(url).openConnection();
        } catch (IOException exception) {
            return Pair.of(false, "URL is invalid");
        }

        if (name.length() > 64)
            return Pair.of(false, "Name is too long");
        if(name.isBlank() || name.length() < 3)
            return Pair.of(false, "Name is too short");

        SavedSongs songs = Database.getDatabase().savedSongs.find(Filters.eq("user", user.getIdLong())).first();
        if (songs == null) {
            songs = new SavedSongs(user.getIdLong());
            Database.getDatabase().savedSongs.insertOne(songs);
        }

        if(songs.getSongs() == null)
            songs.setSongs(new HashMap<>());

        if(songs.getSongs().size() > 25)
            return Pair.of(false, "You cannot save more than 25 songs");

        if(songs.getSongs()
                .entrySet()
                .stream()
                .anyMatch(entry ->
                        entry.getKey().equalsIgnoreCase(name) &&
                                entry.getValue().equalsIgnoreCase(url))) {
            return Pair.of(false, "You already have a song with that name or url");
        }

        songs.getSongs().put(name, url);
        Database.getDatabase().savedSongs.updateOne(Filters.eq("user", user.getIdLong()), Updates.set("songs", songs.getSongs()));
        return Pair.of(true, "Song saved");
    }

    public static Pair<Boolean, String> removeSongs(User user, @Nullable String name, @Nullable String url) {
        if(name == null && url == null)
            return Pair.of(false, "You must specify a name or url");

        if (user.isBot() || user.isSystem())
            return Pair.of(false, "You cannot remove songs");

        SavedSongs songs = Database.getDatabase().savedSongs.find(Filters.eq("user", user.getIdLong())).first();
        if (songs == null || songs.getSongs() == null || songs.getSongs().isEmpty())
            return Pair.of(false, "You do not have any saved songs");

        Map<String, String> matches = songs.getSongs()
                .entrySet()
                .stream()
                .filter(entry -> name != null ? entry.getKey().equalsIgnoreCase(name) : entry.getValue().equalsIgnoreCase(url))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(matches.isEmpty())
            return Pair.of(false, "You do not have a song with that name or url");

        matches.forEach((key, value) -> songs.getSongs().remove(key, value));
        Database.getDatabase().savedSongs.updateOne(Filters.eq("user", user.getIdLong()), Updates.set("songs", songs.getSongs()));
        return Pair.of(true, "Songs removed");
    }

    public static Map<String, String> getSavedSongs(User user) {
        SavedSongs songs = Database.getDatabase().savedSongs.find(Filters.eq("user", user.getIdLong())).first();
        if (songs == null || songs.getSongs() == null || songs.getSongs().isEmpty())
            return new HashMap<>();

        return songs.getSongs();
    }

    public static Pair<Boolean, String> clearSavedSongs(User user) {
        if (user.isBot() || user.isSystem())
            return Pair.of(false, "You cannot clear songs");

        SavedSongs songs = Database.getDatabase().savedSongs.find(Filters.eq("user", user.getIdLong())).first();
        if (songs == null || songs.getSongs() == null || songs.getSongs().isEmpty())
            return Pair.of(false, "You do not have any saved songs");

        songs.getSongs().clear();
        Database.getDatabase().savedSongs.updateOne(Filters.eq("user", user.getIdLong()), Updates.set("songs", songs.getSongs()));
        return Pair.of(true, "Songs cleared");
    }

    public static void playSongs(TextChannel channel, Member member, Collection<String> songList, boolean shuffle) {
        List<String> songs = new ArrayList<>(songList);
        if(shuffle)
            Collections.shuffle(songs);

        AudioChannel audioChannel = member.getVoiceState().getChannel();
        List<CompletableFuture<Pair<Boolean, String>>> results = new ArrayList<>();
        songs.forEach(song -> results.add(play(audioChannel, channel, song, member.getUser())));

        CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).thenRun(() -> {
            int failed = 0;
            for (CompletableFuture<Pair<Boolean, String>> result : results) {
                try {
                    Pair<Boolean, String> pair = result.get();
                    if (!pair.getLeft())
                        failed++;
                } catch (InterruptedException | ExecutionException ignored) {
                    failed++;
                }
            }

            channel.sendMessage("Finished adding saved songs, " + failed + " failed!").queue();
        });
    }
}
