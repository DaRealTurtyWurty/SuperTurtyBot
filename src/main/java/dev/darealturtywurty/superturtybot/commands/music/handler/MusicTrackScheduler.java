package dev.darealturtywurty.superturtybot.commands.music.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

public class MusicTrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final List<AudioTrack> queue;
    private AudioChannel currentChannel;
    private LoopState loopState = LoopState.NONE;
    
    public MusicTrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedList<>();
    }
    
    public void clear() {
        this.queue.clear();
    }
    
    public AudioTrack getCurrentlyPlaying() {
        return this.player.getPlayingTrack();
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
            if(this.loopState == LoopState.SINGLE) {
                this.player.startTrack(this.queue.get(0), false);
                return;
            }

            AudioTrack track = this.queue.remove(0);
            if(this.loopState == LoopState.ALL) {
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
        this.player.setPaused(true);
    }
    
    public void queue(AudioTrack track) {
        if (!this.player.startTrack(track, true)) {
            this.queue.add(track);
        }
    }
    
    public boolean removeTrack(AudioTrack track) {
        if (track == null)
            return false;
        
        if (getCurrentlyPlaying().equals(track)) {
            nextTrack();
            return true;
        }
        
        return this.queue.remove(track);
    }
    
    @Nullable
    public AudioTrack removeTrack(int index) {
        if (index < 0 || index >= this.queue.size())
            return null;
        
        final AudioTrack track = this.queue.remove(index);
        if (getCurrentlyPlaying().equals(track)) {
            nextTrack();
        }
        
        return track;
    }
    
    public void resume() {
        this.player.setPaused(false);
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
        Collections.shuffle(this.queue);
    }
    
    public AudioTrack skip() {
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
        if(getCurrentlyPlaying() != null) {
            AudioTrack track = getCurrentlyPlaying();
            track.setPosition(0);

            this.player.startTrack(track, false);
        }
    }

    public void seek(int time) {
        if(getCurrentlyPlaying() != null) {
            AudioTrack track = getCurrentlyPlaying();
            track.setPosition(time);

            this.player.startTrack(track, false);
        }
    }

    public void moveTrack(int from, int to) {
        if(from < 0 || from >= this.queue.size() || to <= 0 || to > this.queue.size()) {
            return;
        }

        AudioTrack track = this.queue.remove(from);
        this.queue.add(to, track);
    }
}