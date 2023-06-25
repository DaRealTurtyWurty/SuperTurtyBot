package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.handler.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class SeekCommand extends CoreCommand {
    public SeekCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "time", "The time to seek to in the song (in seconds).", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Seeks to a specific time in the current song.";
    }

    @Override
    public String getName() {
        return "seek";
    }

    @Override
    public String getRichName() {
        return "Seek";
    }

    @Override
    public String getHowToUse() {
        return "seek <time>";
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

        TrackData data = track.getUserData(TrackData.class);
        if (data == null || (data.getUserId() != event.getMember().getIdLong() && !event.getMember()
                .hasPermission(channel, Permission.MANAGE_CHANNEL)) || channel.getMembers().size() > 2) {
            reply(event, "❌ You must be the owner of the song or a moderator to seek through this track!", false, true);
            return;
        }

        Integer time = event.getOption("time", null, OptionMapping::getAsInt);
        if (time == null) {
            reply(event, "❌ You must provide a time to seek to!", false, true);
            return;
        }

        if (time <= 0 || time > track.getDuration() / 1000) {
            reply(event, "❌ You must provide a time between 0 and the duration of the song!", false, true);
            return;
        }

        AudioManager.seek(event.getGuild(), time * 1000);
        reply(event, "✅ Seeked to " + time + " seconds!", false, true);
    }
}
