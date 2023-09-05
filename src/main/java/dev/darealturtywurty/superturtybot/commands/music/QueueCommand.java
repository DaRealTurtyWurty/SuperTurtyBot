package dev.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.data.TrackData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getGuild().getAudioManager().isConnected()) {
            event.deferReply(true)
                .setContent("❌ I am not in a voice channel right now! Use `/joinvc` to put me in a voice channel.")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getMember().getVoiceState().inAudioChannel()) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                .mentionRepliedUser(false).queue();
        }

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue == null || queue.isEmpty()) {
            event.deferReply(true)
                .setContent("There are currently no items in the queue. Use `/play` to add something to the queue!")
                .mentionRepliedUser(false).queue();
            return;
        }

        event.deferReply().queue();

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (AudioTrack track : queue) {
            TrackData trackData = track.getUserData(TrackData.class);
            contents.field("[" + StringUtils.millisecondsFormatted(track.getDuration()) + "] - " + track.getInfo().title.trim(),
                    "[Link](%s)\nAdded by: %s".formatted(track.getInfo().uri, trackData == null ? "Unknown" : event.getGuild().getMemberById(trackData.getUserId()).getAsMention()));
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
