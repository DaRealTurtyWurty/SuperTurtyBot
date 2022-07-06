package io.github.darealturtywurty.superturtybot.commands.music;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class JoinCommand extends CoreCommand {
    public JoinCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Joins the user's vc";
    }
    
    @Override
    public String getName() {
        return "joinvc";
    }
    
    @Override
    public String getRichName() {
        return "Join VC";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        if (event.getGuild().getAudioManager().isConnected()) {
            event.deferReply(true)
                .setContent("❌ I am already connected to "
                    + event.getGuild().getAudioManager().getConnectedChannel().getAsMention() + "!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        event.getGuild().getAudioManager().openAudioConnection(channel);
        event.deferReply().setContent("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
            .queue();
    }
}
