package dev.darealturtywurty.superturtybot.commands.music.handler;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.codepoetics.ambivalence.Either;
import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

public final class AudioManager {
    private static final Map<Long, GuildAudioManager> AUDIO_MANAGERS = new HashMap<>();
    public static final AudioPlayerManager AUDIO_MANAGER = new DefaultAudioPlayerManager();

    static {
        // Local Files (mp3, ogg, etc)
        AudioSourceManagers.registerLocalSource(AUDIO_MANAGER);

        // Spotify
        AUDIO_MANAGER.registerSourceManager(new SpotifySourceManager(null, Environment.INSTANCE.spotifyID(),
            Environment.INSTANCE.spotifySecret(), "US", AUDIO_MANAGER));

        // Clypit, Speech (TTS), PornHub, Reddit, OCRemix, TikTok, Mixcloud, SoundGasm
        DuncteBotSources.registerAll(AUDIO_MANAGER, "en-US");

        // YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, Beam, GetYarn, Http
        final var ytSource = new YoutubeAudioSourceManager(true, null, null);
        ytSource.setPlaylistPageCount(100);
        AUDIO_MANAGER.registerSourceManager(ytSource);
        AudioSourceManagers.registerRemoteSources(AUDIO_MANAGER);

        ShutdownHooks.register(() -> {
            AUDIO_MANAGER.shutdown();
            AUDIO_MANAGERS.values().forEach(manager -> manager.player.destroy());
        });
    }

    private AudioManager() {
    }

    public static void clear(Guild guild) {
        getOrCreate(guild).musicScheduler.clear();
    }

    @Nullable
    public static AudioTrack getCurrentlyPlaying(Guild guild) {
        return getOrCreate(guild).musicScheduler.getCurrentlyPlaying();
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
        return getOrCreate(guild).musicScheduler.getQueue();
    }

    public static int getVolume(Guild guild) {
        return getOrCreate(guild).musicScheduler.getVolume();
    }

    public static boolean isPaused(Guild guild) {
        return getOrCreate(guild).musicScheduler.isPaused();
    }

    public static boolean isPlaying(Guild guild) {
        return getOrCreate(guild).musicScheduler.getCurrentlyPlaying() != null;
    }

    public static void pause(Guild guild) {
        getOrCreate(guild).musicScheduler.pause();
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
                playlist.getTracks().forEach(track -> track.setUserData(user.getIdLong()));

                if (toPlay.startsWith("ytsearch:")) {
                    manager.musicScheduler.queue(playlist.getTracks().get(0));

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
                    playlist.getTracks().forEach(manager.musicScheduler::queue);

                    final var embed = new EmbedBuilder();
                    embed.setTimestamp(Instant.now());
                    embed.setColor(Color.GREEN);
                    embed.setTitle("Added: " + playlist.getTracks().size() + " tracks to the queue!",
                        toPlay.startsWith("http") ? toPlay : null);

                    textChannel.sendMessageEmbeds(embed.build()).queue();
                }

                final AudioTrack track = playlist.getTracks().get(0);
                if (manager.musicScheduler.getCurrentlyPlaying().equals(track)) {
                    manager.musicScheduler.setAudioChannel(audioChannel);

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
                manager.musicScheduler.queue(track);
                track.setUserData(user.getIdLong());

                final AudioTrack nowPlaying = manager.musicScheduler.getCurrentlyPlaying();
                if (nowPlaying.equals(track)) {
                    manager.musicScheduler.setAudioChannel(audioChannel);
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
                manager.musicScheduler.setAudioChannel(channel);
                manager.musicScheduler.queue(track);
            }
        });
    }

    public static boolean removeTrack(Guild guild, AudioTrack track) {
        return getOrCreate(guild).musicScheduler.removeTrack(track);
    }

    @Nullable
    public static AudioTrack removeTrack(Guild guild, int index) {
        return getOrCreate(guild).musicScheduler.removeTrack(index);
    }

    public static void resume(Guild guild) {
        getOrCreate(guild).musicScheduler.resume();
    }

    public static CompletableFuture<Either<List<AudioTrack>, FriendlyException>> search(Guild guild, String term) {
        final GuildAudioManager manager = getOrCreate(guild);
        final var future = new CompletableFuture<Either<List<AudioTrack>, FriendlyException>>();
        AUDIO_MANAGER.loadItemOrdered(manager, term, new AudioLoadResultHandler() {
            @Override
            public void loadFailed(FriendlyException exception) {
                future.complete(Either.ofRight(exception));
            }

            @Override
            public void noMatches() {
                future.complete(Either.ofLeft(List.of()));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                future.complete(Either.ofLeft(List.copyOf(playlist.getTracks())));
            }

            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(Either.ofLeft(List.of(track)));
            }
        });

        return future;
    }

    public static int setVolume(Guild guild, int volume) {
        getOrCreate(guild).musicScheduler.setVolume(volume);
        return getVolume(guild);
    }

    public static void shuffle(Guild guild) {
        getOrCreate(guild).musicScheduler.shuffle();
    }

    @Nullable
    public static AudioTrack skip(Guild guild) {
        return getOrCreate(guild).musicScheduler.skip();
    }

    public static LoopState getLoopState(Guild guild) {
        return getOrCreate(guild).musicScheduler.getLoopState();
    }

    public static void setLoopState(Guild guild, LoopState loopState) {
        getOrCreate(guild).musicScheduler.setLoopState(loopState);
    }

    public static LoopState toggleLoopState(Guild guild) {
        return getOrCreate(guild).musicScheduler.toggleLoopState();
    }

    public static void restartTrack(Guild guild) {
        getOrCreate(guild).musicScheduler.restartTrack();
    }

    public static void seek(Guild guild, int time) {
        getOrCreate(guild).musicScheduler.seek(time);
    }

    public static int getTrackPosition(Guild guild, AudioTrack track) {
        List<AudioTrack> queue = getQueue(guild);
        return queue.indexOf(track);
    }

    public static void moveTrack(Guild guild, int from, int to) {
        getOrCreate(guild).musicScheduler.moveTrack(from, to);
    }
}
