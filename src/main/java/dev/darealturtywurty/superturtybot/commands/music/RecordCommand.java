package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.commands.music.manager.handler.AudioReceivingHandler;
import dev.darealturtywurty.superturtybot.commands.music.manager.listener.AudioListener;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RecordCommand extends CoreCommand {
    public RecordCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Records the current voice channel for 30 seconds.";
    }

    @Override
    public String getName() {
        return "record";
    }

    @Override
    public String getRichName() {
        return "Record";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if(!event.isFromGuild() || guild == null || member == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = voiceState.getChannel();
        if (channel == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        AudioManager manager = guild.getAudioManager();
        if (!manager.isConnected()) {
            manager.openAudioConnection(channel);
            event.getChannel()
                    .sendMessage("✅ I have joined " + channel.getAsMention() + "!")
                    .mentionRepliedUser(false)
                    .queue();
        }

        if (voiceState.getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to play a song!", false, true);
            return;
        }

        AudioReceivingHandler handler;
        if (!(manager.getReceivingHandler() instanceof AudioReceivingHandler receivingHandler)) {
            handler = new AudioReceivingHandler();
            manager.setReceivingHandler(handler);
        } else {
            handler = receivingHandler;
        }

        List<CombinedAudio> audioPackets = new ArrayList<>();

        AtomicReference<AudioListener> listenerRef = new AtomicReference<>();
        final AudioListener listener = handler.addListener(audio -> {
            audioPackets.add(audio);
            if(audioPackets.size() >= 20 * 50 * 30) {
                handler.removeListener(listenerRef.get());

            }
        });

        listenerRef.set(listener);
    }
}
