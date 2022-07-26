package io.github.darealturtywurty.superturtybot.commands.music;

import java.util.List;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ShuffleCommand extends CoreCommand {
    public ShuffleCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public String getAccess() {
        return "Moderators, Owner of whole queue, Everyone (if the owners arent in VC)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Shuffles the music queue";
    }
    
    @Override
    public String getName() {
        return "shuffle";
    }
    
    @Override
    public String getRichName() {
        return "Shuffle";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
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
        
        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue.isEmpty()) {
            reply(event, "❌ The music queue is currently empty!", false, true);
            return;
        }
        
        AudioManager.shuffle(event.getGuild());
        reply(event, "✅ The queue has now been shuffled!");
    }
}
