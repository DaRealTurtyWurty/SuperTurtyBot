package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

public class ClearCommand extends CoreCommand {
    public ClearCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public String getAccess() {
        return "Moderators, Owner of all songs (if present in VC), Everyone (if the owners of the songs are not in VC)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Clears the music queue";
    }

    @Override
    public String getName() {
        return "clearqueue";
    }

    @Override
    public String getRichName() {
        return "Clear Queue";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || voiceState.getChannel() == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = voiceState.getChannel();
        if (!event.getGuild().getAudioManager().isConnected() || event.getGuild().getSelfMember()
                .getVoiceState() == null || event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }

        if (event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue.isEmpty()) {
            reply(event, "❌ The queue is currently empty!", false, true);
            return;
        }

        Member member = event.getMember();
        boolean isModerator = member.hasPermission(channel, Permission.MANAGE_CHANNEL);
        // if they are a moderator or the owner of all the songs or the only person in the vc
        if (!isModerator && !checkOwnsAll(queue, member) && channel.getMembers().size() > 2) {
            reply(event,
                    "❌ You must be the owner of all songs in the queue or have the `Manage Channel` permission to use this command!",
                    false, true);
            return;
        }

        AudioManager.clear(event.getGuild());
        reply(event, "✅ The music queue has now been cleared!");
    }

    private static boolean checkOwnsAll(List<AudioTrack> queue, Member member) {
        long trackMatch = queue.stream()
                .map(track -> {
                    TrackData data = track.getUserData(TrackData.class);
                    return data == null ? 0L : data.getUserId();
                })
                .filter(owner -> owner == member.getIdLong())
                .count();

        return trackMatch == queue.size();
    }
}
