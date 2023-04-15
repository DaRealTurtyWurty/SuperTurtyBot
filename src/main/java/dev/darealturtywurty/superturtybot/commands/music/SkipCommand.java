package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SkipCommand extends CoreCommand {
    public SkipCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public String getAccess() {
        return "Moderators, Owner of song, Everyone (if owner isn't in VC)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Skips the currently playing track";
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getRichName() {
        return "Skip";
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
        if (voiceState == null || !voiceState.inAudioChannel() || voiceState.getChannel() == null) {
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

        AudioTrack track = AudioManager.getCurrentlyPlaying(event.getGuild());
        if (track == null) {
            reply(event, "❌ There is nothing playing!", false, true);
            return;
        }

        long songOwnerId = track.getUserData(Long.class);
        Member member = event.getMember();
        boolean isModerator = member.hasPermission(channel, Permission.MANAGE_CHANNEL);

        // if they are a moderator or the owner of the song or the only person in the vc
        if (!isModerator && songOwnerId != member.getIdLong() && channel.getMembers().size() > 2) {
            reply(event, "❌ You must be a moderator or the owner of the song to skip it!", false, true);
            return;
        }

        if (AudioManager.skip(event.getGuild()) == null) {
            reply(event, "❌ Unable to skip!", false, true);
            return;
        }

        reply(event, "**" + track.getInfo().title + "** was skipped!");
    }
}
