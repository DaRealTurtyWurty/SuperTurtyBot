package io.github.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SkipCommand extends CoreCommand {
    public SkipCommand() {
        super(new Types(true, false, false, false));
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }
        
        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (!event.getGuild().getAudioManager().isConnected()) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }
        
        if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }
        
        final AudioTrack removed = AudioManager.skip(event.getGuild());
        if (removed == null) {
            reply(event, "❌ Unable to skip!", false, true);
            return;
        }
        
        reply(event, "**" + removed.getInfo().title + "** was skipped!");
    }
}
