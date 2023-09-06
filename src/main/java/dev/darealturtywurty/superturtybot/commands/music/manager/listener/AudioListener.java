package dev.darealturtywurty.superturtybot.commands.music.manager.listener;

import net.dv8tion.jda.api.audio.CombinedAudio;

@FunctionalInterface
public interface AudioListener {
    void onAudioData(CombinedAudio audio);
}