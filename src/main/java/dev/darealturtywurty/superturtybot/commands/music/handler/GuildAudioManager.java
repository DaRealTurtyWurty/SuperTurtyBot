package dev.darealturtywurty.superturtybot.commands.music.handler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.darealturtywurty.superturtybot.commands.music.handler.filter.FilterChainConfiguration;

public class GuildAudioManager {
    private final AudioPlayer player;
    private final MusicTrackScheduler musicScheduler;
    private final FilterChainConfiguration filterChainConfiguration = new FilterChainConfiguration();
    
    public GuildAudioManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.musicScheduler = new MusicTrackScheduler(this.getPlayer());

        getPlayer().addListener(getMusicScheduler());
        getPlayer().setFilterFactory(this.filterChainConfiguration.factory());
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

    public FilterChainConfiguration getFilterChainConfiguration() {
        return filterChainConfiguration;
    }
}
