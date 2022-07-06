package io.github.darealturtywurty.superturtybot.commands.music.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildAudioManager {
    public final AudioPlayer player;
    public final MusicTrackScheduler musicScheduler;
    
    public GuildAudioManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.musicScheduler = new MusicTrackScheduler(this.player);
        this.player.addListener(this.musicScheduler);
    }
    
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(this.player);
    }
}
