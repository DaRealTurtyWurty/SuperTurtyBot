package dev.darealturtywurty.superturtybot.commands.music.manager.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.core.util.Either;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SearchLoadHandler implements AudioLoadResultHandler {
    private final CompletableFuture<Either<List<AudioTrack>, FriendlyException>> future;

    public SearchLoadHandler(CompletableFuture<Either<List<AudioTrack>, FriendlyException>> future) {
        this.future = future;
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        this.future.complete(Either.right(exception));
    }

    @Override
    public void noMatches() {
        this.future.complete(Either.left(List.of()));
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        this.future.complete(Either.left(List.copyOf(playlist.getTracks())));
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        this.future.complete(Either.left(List.of(track)));
    }
}
