package io.github.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class NowPlayingCommand extends CoreCommand {
    public NowPlayingCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Gets the song that is currently playing";
    }
    
    @Override
    public String getName() {
        return "nowplaying";
    }
    
    @Override
    public String getRichName() {
        return "Now Playing";
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
            return;
        }
        
        final AudioTrack nowPlaying = AudioManager.getCurrentlyPlaying(event.getGuild());
        if (nowPlaying == null) {
            event.deferReply(true).setContent("❌ I am not playing anything right now!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        embed.setTitle("Now Playing: " + nowPlaying.getInfo().title,
            nowPlaying.getInfo().uri.startsWith("http") ? nowPlaying.getInfo().uri : null);
        final int percentage = Math.round((float) nowPlaying.getPosition() / nowPlaying.getDuration() * 100);
        embed.setDescription("[**" + millisecondsFormatted(nowPlaying.getPosition()) + "**/**"
            + millisecondsFormatted(nowPlaying.getDuration()) + "**] "
            + makeProgresssBar(nowPlaying.getDuration(), nowPlaying.getPosition(), 12) + " (" + percentage + "%)");
        embed.setThumbnail("http://img.youtube.com/vi/" + nowPlaying.getIdentifier() + "/maxresdefault.jpg");
        embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
            event.getUser().getEffectiveAvatarUrl());
        
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
    }
    
    private static String makeProgresssBar(long total, long current, int size) {
        final String line = "▬";
        final String slider = "🔘";
        final var result = new String[size];
        if (current >= total) {
            Arrays.fill(result, line);
            result[size - 1] = slider;
            return String.join("", result);
        }
        
        final double percentage = (float) current / total;
        final int progress = (int) Math.max(0, Math.min(Math.round(size * percentage), size - 1));
        for (int index = 0; index < progress; index++) {
            result[index] = line;
        }
        
        result[progress] = slider;
        for (int index = progress + 1; index < size; index++) {
            result[index] = line;
        }
        
        return String.join("", result);
    }
    
    // TODO: Utility class
    private static String millisecondsFormatted(final long millis) {
        final long hours = TimeUnit.MILLISECONDS.toHours(millis)
            - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        final String ret = String.format("%s%s%s", hours > 0 ? String.format("%02d", hours) + ":" : "",
            minutes > 0 ? String.format("%02d", minutes) + ":" : "00:",
            seconds > 0 ? String.format("%02d", seconds) : "00").trim();
        return ret.endsWith(":") ? ret.substring(0, ret.length() - 1) : ret;
    }
}
