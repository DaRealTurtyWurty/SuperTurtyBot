package dev.darealturtywurty.superturtybot.commands.music.handler;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class MusicRestartCommand extends CoreCommand {

    public MusicRestartCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Restarts the current music track.";
    }

    @Override
    public String getName() {
        return "musicrestart";
    }

    @Override
    public String getRichName() {
        return "Music Restart";
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

        Long owner = track.getUserData(Long.class);
        if ((owner == null || owner != event.getMember().getIdLong() && !event.getMember()
                .hasPermission(channel, Permission.MANAGE_CHANNEL)) || channel.getMembers().size() > 2) {
            reply(event, "❌ You must be the owner of the song or a moderator to restart this track!", false, true);
            return;
        }

        AudioManager.restartTrack(event.getGuild());
        reply(event, "✅ Restarted the current track!", false, true);
    }
}
