package dev.darealturtywurty.superturtybot.commands.music.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MusicTrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final List<AudioTrack> queue;
    private AudioChannel currentChannel;
    private LoopState loopState = LoopState.NONE;
    private AudioTrack guessTheSongTrack;

    public MusicTrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedList<>();
        player.addListener(event -> {
            if (event instanceof TrackEndEvent trackEndEvent) {
                if(Objects.equals(trackEndEvent.track, guessTheSongTrack)) {
                    guessTheSongTrack = null;
                }
            }
        });
    }

    public boolean addGuessTheSongTrack(AudioTrack track) {
        if (this.player.getPlayingTrack() != null || !this.queue.isEmpty() || isPaused() || this.guessTheSongTrack != null)
            return false;

        // set the track to play
        this.guessTheSongTrack = track;
        this.player.startTrack(track, false);

        return true;
    }

    public void clear() {
        this.queue.clear();
    }

    public @Nullable AudioTrack getCurrentlyPlaying() {
        return guessTheSongTrack == null ? this.player.getPlayingTrack() : null;
    }

    public List<AudioTrack> getQueue() {
        return new ArrayList<>(this.queue);
    }

    public int getVolume() {
        return this.player.getVolume();
    }

    public boolean isPaused() {
        return this.player.isPaused();
    }

    public void nextTrack() {
        if (!this.queue.isEmpty()) {
            if (this.loopState == LoopState.SINGLE) {
                this.player.startTrack(this.queue.get(0), false);
                return;
            }

            AudioTrack track = this.queue.remove(0);
            if (this.loopState == LoopState.ALL) {
                this.queue.add(track);
            }

            this.player.startTrack(track, false);
        } else {
            this.player.startTrack(null, false);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track.getInfo().uri.endsWith(".mp3") || track.getInfo().uri.endsWith(".wav")
                || track.getInfo().uri.endsWith(".ogg")) {
            setAudioChannel(null);
        } else if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    public void pause() {
        if (this.guessTheSongTrack == null) {
            this.player.setPaused(true);
        }
    }

    public void queue(AudioTrack track) {
        if (this.guessTheSongTrack != null)
            return;

        if (!this.player.startTrack(track, true)) {
            this.queue.add(track);
        }
    }

    public boolean removeTrack(AudioTrack track) {
        if (track == null)
            return false;

        if (this.guessTheSongTrack != null)
            return false;

        if (Objects.equals(getCurrentlyPlaying(), track)) {
            nextTrack();
            return true;
        }

        return this.queue.remove(track);
    }

    public @Nullable AudioTrack removeTrack(int index) {
        if (index < 0 || index >= this.queue.size())
            return null;

        if (this.guessTheSongTrack != null)
            return null;

        final AudioTrack track = this.queue.remove(index);
        if (Objects.equals(getCurrentlyPlaying(), track)) {
            nextTrack();
        }

        return track;
    }

    public void resume() {
        if (this.guessTheSongTrack == null) {
            this.player.setPaused(false);
        }
    }

    public void setAudioChannel(AudioChannel channel) {
        if (channel == null) {
            this.currentChannel.getGuild().getAudioManager().closeAudioConnection();
            this.currentChannel = null;
            return;
        }

        this.currentChannel = channel;
        channel.getGuild().getAudioManager().openAudioConnection(channel);
    }

    public void setVolume(int volume) {
        int newVol = volume;
        if (volume < 0) {
            newVol = 0;
        } else if (volume > 1000) {
            newVol = 1000;
        }

        this.player.setVolume(newVol);
    }

    public void shuffle() {
        if (this.guessTheSongTrack != null)
            return;

        Collections.shuffle(this.queue);
    }

    public AudioTrack skip() {
        if (this.guessTheSongTrack != null)
            return null;

        if (getCurrentlyPlaying() != null) {
            final AudioTrack current = getCurrentlyPlaying();
            nextTrack();
            return current;
        }

        return null;
    }

    public @NotNull LoopState getLoopState() {
        return this.loopState;
    }

    public LoopState toggleLoopState() {
        return this.loopState = LoopState.values()[(this.loopState.ordinal() + 1) % LoopState.values().length];
    }

    public void setLoopState(@NotNull LoopState loopState) {
        this.loopState = loopState;
    }

    public void restartTrack() {
        if (this.guessTheSongTrack != null)
            return;

        if (getCurrentlyPlaying() != null) {
            AudioTrack track = getCurrentlyPlaying();
            track.setPosition(0);

            this.player.startTrack(track, false);
        }
    }

    public void seek(int time) {
        if (this.guessTheSongTrack != null)
            return;

        if (getCurrentlyPlaying() != null) {
            AudioTrack track = getCurrentlyPlaying();
            track.setPosition(time);

            this.player.startTrack(track, false);
        }
    }

    public void moveTrack(int from, int to) {
        if (this.guessTheSongTrack != null)
            return;

        if (from < 0 || from >= this.queue.size() || to <= 0 || to > this.queue.size()) {
            return;
        }

        AudioTrack track = this.queue.remove(from);
        this.queue.add(to, track);
    }

    public void endGuessTheSong() {
        this.guessTheSongTrack = null;
        nextTrack();
    }

    public boolean isGuessSongRunning() {
        return this.guessTheSongTrack != null;
    }
}