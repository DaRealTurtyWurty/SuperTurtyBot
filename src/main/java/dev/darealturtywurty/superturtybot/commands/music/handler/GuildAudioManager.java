package dev.darealturtywurty.superturtybot.commands.music.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildAudioManager {
    private final AudioPlayer player;
    private final MusicTrackScheduler musicScheduler;
    
    public GuildAudioManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.musicScheduler = new MusicTrackScheduler(this.getPlayer());
        this.getPlayer().addListener(this.getMusicScheduler());
    }
    
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(this.getPlayer());
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public MusicTrackScheduler getMusicScheduler() {
        return musicScheduler;
    }
}
