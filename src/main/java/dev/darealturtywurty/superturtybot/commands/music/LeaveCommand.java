package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

public class LeaveCommand extends CoreCommand {
    public LeaveCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public String getAccess() {
        return "Moderators, Singular Person in VC";
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
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ I must be in a server for you to be able to use this command!", false, true);
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected() || audioManager.getConnectedChannel() == null) {
            reply(event, "❌ I must be in a voice channel for you to be able to use this command!", false, true);
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel for you to be able to use this command!", false, true);
            return;
        }

        AudioChannel channel = audioManager.getConnectedChannel();
        if (memberVoiceState.getChannel() == null || memberVoiceState.getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me for you to be able to use this command!", false, true);
            return;
        }

        if (!event.getMember().hasPermission(channel, Permission.MANAGE_CHANNEL) && channel.getMembers().size() > 2) {
            reply(event, "❌ You must be a moderator or the only person in the vc for you to be able to use this command!", false, true);
            return;
        }

        audioManager.closeAudioConnection();
        reply(event, "✅ I have left " + channel.getAsMention() + "!", false);
    }
}
