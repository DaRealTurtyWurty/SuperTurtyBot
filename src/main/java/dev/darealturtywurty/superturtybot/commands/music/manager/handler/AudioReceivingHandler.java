package dev.darealturtywurty.superturtybot.commands.music.manager.handler;

import dev.darealturtywurty.superturtybot.commands.music.manager.listener.AudioListener;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AudioReceivingHandler implements AudioReceiveHandler {
    private final List<AudioListener> listeners = new ArrayList<>();

    @Override
    public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
        for (AudioListener listener : this.listeners) {
            listener.onAudioData(combinedAudio);
        }
    }

    public AudioListener addListener(AudioListener listener) {
        this.listeners.add(listener);
        return listener;
    }

    public void removeListener(AudioListener listener) {
        this.listeners.remove(listener);
    }
}
