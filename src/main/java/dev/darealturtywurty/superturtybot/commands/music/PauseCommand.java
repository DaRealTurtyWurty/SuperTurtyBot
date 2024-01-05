package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PauseCommand extends CoreCommand {
    public PauseCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public String getAccess() {
        return "Moderators, Owner of Song";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Pauses the music player";
    }
    
    @Override
    public String getName() {
        return "pause";
    }
    
    @Override
    public String getRichName() {
        return "Pause";
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
        
        if (event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }
        
        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null || !event.getGuild().getAudioManager().isConnected()) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }
        
        if (event.getMember().getVoiceState().getChannel() == null || event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }
        
        if (!AudioManager.isPlaying(event.getGuild())) {
            reply(event, "❌ I am currently not playing anything!", false, true);
            return;
        }
        
        AudioManager.pause(event.getGuild());
        reply(event, "✅ The music player has been paused!");
    }
}
