package dev.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
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
        embed.setDescription("[**" + StringUtils.millisecondsFormatted(nowPlaying.getPosition()) + "**/**"
            + StringUtils.millisecondsFormatted(nowPlaying.getDuration()) + "**] "
            + makeProgresssBar(nowPlaying.getDuration(), nowPlaying.getPosition(), 12) + " (" + percentage + "%)");
        embed.setThumbnail("http://img.youtube.com/vi/" + nowPlaying.getIdentifier() + "/maxresdefault.jpg");
        embed.setFooter(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
        
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
}
