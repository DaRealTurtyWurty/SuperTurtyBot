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

public class MoveCommand extends CoreCommand {
    public MoveCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "from", "The position of the song to move", true),
                new OptionData(OptionType.INTEGER, "to", "The position to move the song to", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Moves a song in the queue to a different position.";
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getRichName() {
        return "Move";
    }

    @Override
    public String getHowToUse() {
        return "move <from> <to>";
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
            reply(event, "❌ You must be the owner of the song or a moderator to skip this track!", false, true);
            return;
        }

        Integer from = event.getOption("from", null, OptionMapping::getAsInt);
        Integer to = event.getOption("to", null, OptionMapping::getAsInt);
        if (from == null || to == null) {
            reply(event, "❌ You must provide a valid position to move the song to!", false, true);
            return;
        }

        if(from.equals(to)) {
            reply(event, "❌ You cannot move a song to the same position!", false, true);
            return;
        }

        if (to < from) {
            reply(event, "❌ You cannot move a song to a position before it!", false, true);
            return;
        }

        if(from < 0) {
            reply(event, "❌ You cannot move a song to a negative position!", false, true);
            return;
        }

        if(from > AudioManager.getQueue(event.getGuild()).size() || to > AudioManager.getQueue(event.getGuild()).size()) {
            reply(event, "❌ You cannot move songs outside the queue bounds.", false, true);
            return;
        }

        if (AudioManager.getQueue(event.getGuild()).get(from) == null) {
            reply(event, "❌ There is no song at position " + from + "!", false, true);
            return;
        }

        AudioManager.moveTrack(event.getGuild(), from, to);
        reply(event, "✅ Moved song from position %d to %d!".formatted(from, to), false, true);
    }
}
