package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class QueueCommand extends CoreCommand {
    public QueueCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Gets the current queue of the bot";
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getRichName() {
        return "Queue";
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

        if (!event.getGuild().getAudioManager().isConnected()) {
            reply(event, "❌ I am not in a voice channel right now! Use `/joinvc` to put me in a voice channel.", false, true);
            return;
        }

        if (event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
        }

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue == null || queue.isEmpty()) {
            reply(event, "There are currently no items in the queue. Use `/play` to add something to the queue!", false, true);
            return;
        }

        event.deferReply().queue();

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (AudioTrack track : queue) {
            Member trackMember = event.getGuild().getMemberById(track.getUserData(TrackData.class).getUserId());
            TrackData trackData = track.getUserData(TrackData.class);
            contents.field("[" + TimeUtils.millisecondsFormatted(track.getDuration()) + "] - " + track.getInfo().title.trim(),
                    "[Link](%s)\nAdded by: %s".formatted(track.getInfo().uri, trackData == null || trackMember == null ? "Unknown" : trackMember.getAsMention()));
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                .title("Music queue for: " + event.getGuild().getName())
                .color(Color.BLUE)
                .timestamp(Instant.now())
                .footer("Requested by: " + event.getUser().getName(), event.getMember().getEffectiveAvatarUrl())
                .authorOnly(event.getUser().getIdLong())
                .thumbnail(event.getGuild().getIconUrl())
                .build(event.getJDA());

        embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ The queue has failed to load!").queue());
    }
}
