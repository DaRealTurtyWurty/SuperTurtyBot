package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class VolumeCommand extends CoreCommand {
    public VolumeCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "volume", "The volume of the music player", false).setRequiredRange(
                        0, 1000));
    }

    @Override
    public String getAccess() {
        return "Moderators";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Sets the volume of the music player";
    }

    @Override
    public String getHowToUse() {
        return "/volume [amount]";
    }

    @Override
    public String getName() {
        return "volume";
    }

    @Override
    public String getRichName() {
        return "Volume";
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

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || voiceState.getChannel() == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = voiceState.getChannel();
        if (!event.getGuild().getAudioManager().isConnected() || event.getGuild().getSelfMember()
                .getVoiceState() == null || event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }

        if (event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }

        if (!event.getMember().hasPermission(channel, Permission.MANAGE_CHANNEL) && channel.getMembers().size() > 2) {
            reply(event, "❌ You must have the `Manage Channel` permission to use this command!", false, true);
            return;
        }

        final OptionMapping volume = event.getOption("volume");
        if (volume == null) {
            reply(event, "The current volume is `" + AudioManager.getVolume(event.getGuild()) + "`");
            return;
        }

        reply(event,
                "The volume has been set to `" + AudioManager.setVolume(event.getGuild(), volume.getAsInt()) + "`");
    }
}
