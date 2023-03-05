package dev.darealturtywurty.superturtybot.commands.music.handler;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicListener extends ListenerAdapter {
    public static final MusicListener INSTANCE = new MusicListener();

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() == null) {
            if (AudioManager.isPlaying(event.getGuild())) {
                return;
            }

            GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
            if (selfVoiceState == null || !selfVoiceState.inAudioChannel()) return;

            AudioChannelUnion audioChannel = event.getChannelJoined();
            if (selfVoiceState.getChannel().getIdLong() != audioChannel.getIdLong()) return;

            EXECUTOR_SERVICE.schedule(() -> {
                AudioChannel channel = event.getGuild().getAudioManager().getConnectedChannel();
                if (channel != null && !AudioManager.isPlaying(event.getGuild())) {
                    channel.getGuild().getAudioManager().closeAudioConnection();
                }
            }, 30, TimeUnit.SECONDS);

            return;
        }

        GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        if (selfVoiceState == null || !selfVoiceState.inAudioChannel()) return;

        AudioChannelUnion audioChannel = event.getChannelLeft();
        if (selfVoiceState.getChannel().getIdLong() != audioChannel.getIdLong()) return;

        if (audioChannel.getMembers().size() == 1) {
            EXECUTOR_SERVICE.schedule(() -> {
                if (audioChannel.getMembers().size() == 1) {
                    AudioChannel channel = event.getGuild().getAudioManager().getConnectedChannel();
                    if (channel != null) {
                        AudioManager.pause(event.getGuild());
                        channel.getGuild().getAudioManager().closeAudioConnection();
                    }
                }
            }, 10, TimeUnit.SECONDS);
        }
    }
}
