package dev.darealturtywurty.superturtybot.commands.music.manager.data;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.MusicTrackScheduler;
import dev.darealturtywurty.superturtybot.commands.music.manager.handler.AudioPlayerSendHandler;
import lombok.Getter;

@Getter
public class GuildAudioManager {
    private final AudioPlayer player;
    private final MusicTrackScheduler musicScheduler;

    public GuildAudioManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.musicScheduler = new MusicTrackScheduler(this.getPlayer());

        getPlayer().addListener(getMusicScheduler());
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(this.getPlayer());
    }
}
