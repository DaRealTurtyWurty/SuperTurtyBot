package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.Instant;

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
            return;
        }
        
        final AudioTrack nowPlaying = AudioManager.getCurrentlyPlaying(event.getGuild());
        if (nowPlaying == null) {
            reply(event, "❌ I am not playing anything right now!", false, true);
            return;
        }
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        embed.setTitle("Now Playing: " + nowPlaying.getInfo().title,
            nowPlaying.getInfo().uri.startsWith("http") ? nowPlaying.getInfo().uri : null);
        final int percentage = Math.round((float) nowPlaying.getPosition() / nowPlaying.getDuration() * 100);
        embed.setDescription("[**" + TimeUtils.millisecondsFormatted(nowPlaying.getPosition()) + "**/**"
            + TimeUtils.millisecondsFormatted(nowPlaying.getDuration()) + "**] "
            + StringUtils.makeProgressBar(nowPlaying.getDuration(), nowPlaying.getPosition(), 12, "▬", "🔘") + " (" + percentage + "%)");
        embed.setThumbnail("http://img.youtube.com/vi/" + nowPlaying.getIdentifier() + "/maxresdefault.jpg");
        embed.setFooter(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());

        reply(event, embed, false);
    }
}
