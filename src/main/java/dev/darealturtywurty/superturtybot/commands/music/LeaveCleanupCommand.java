package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.handler.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

public class LeaveCleanupCommand extends CoreCommand {

    public LeaveCleanupCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Removes all songs in the queue that were added by users that are no longer in the voice channel.";
    }

    @Override
    public String getName() {
        return "leavecleanup";
    }

    @Override
    public String getRichName() {
        return "Leave Cleanup";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            event.deferReply(true).setContent("❌ I must be in a server for you to be able to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        net.dv8tion.jda.api.managers.AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() || audioManager.getConnectedChannel() == null) {
            event.deferReply(true).setContent("❌ I must be in a voice channel for you to be able to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            event.deferReply(true)
                    .setContent("❌ You must be in a voice channel for you to be able to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        AudioChannel channel = audioManager.getConnectedChannel();
        if (memberVoiceState.getChannel() == null || memberVoiceState.getChannel().getIdLong() != channel.getIdLong()) {
            event.deferReply(true)
                    .setContent("❌ You must be in the same voice channel as me for you to be able to use this command!")
                    .mentionRepliedUser(false).queue();
            return;
        }

        List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue.isEmpty()) {
            event.deferReply(true).setContent("❌ There are no songs in the queue!").mentionRepliedUser(false).queue();
            return;
        }

        int queueSize = queue.size();
        queue.forEach(track -> {
            if (track.equals(AudioManager.getCurrentlyPlaying(event.getGuild()))) {
                return;
            }

            TrackData trackData = track.getUserData(TrackData.class);
            if (trackData == null)
                return;

            long owner = trackData.getUserId();
            if (channel.getMembers().stream().noneMatch(member -> member.getIdLong() == owner)) {
                AudioManager.removeTrack(event.getGuild(), track);
            }
        });

        int removed = queueSize - queue.size();
        if (removed > 0) {
            event.deferReply(false).setContent("✅ Removed " + removed + " songs from the queue!")
                    .mentionRepliedUser(false).queue();
        } else {
            event.deferReply(true).setContent(
                            "❌ There were no songs in the queue that were added by users that are no longer in the voice channel!")
                    .mentionRepliedUser(false).queue();
        }
    }
}
