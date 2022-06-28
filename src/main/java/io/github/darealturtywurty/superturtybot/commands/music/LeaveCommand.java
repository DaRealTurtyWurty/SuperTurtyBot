package io.github.darealturtywurty.superturtybot.commands.music;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class LeaveCommand extends CoreCommand {
    public LeaveCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Leaves the current vc";
    }
    
    @Override
    public String getName() {
        return "leavevc";
    }
    
    @Override
    public String getRichName() {
        return "Leave VC";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ I must be in a server for you to be able to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getGuild().getAudioManager().isConnected()) {
            event.deferReply(true).setContent("❌ I must be in a voice channel for you to be able to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final AudioChannel channel = event.getGuild().getAudioManager().getConnectedChannel();
        event.getGuild().getAudioManager().closeAudioConnection();
        event.deferReply().setContent("✅ I have left " + channel.getAsMention() + "!").mentionRepliedUser(false)
            .queue();
    }
}
