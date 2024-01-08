package dev.darealturtywurty.superturtybot.commands.music;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
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
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (event.getGuild().getAudioManager().isConnected() && event.getGuild().getAudioManager().getConnectedChannel() != null) {
            reply(event, "❌ I am already connected to "
                    + event.getGuild().getAudioManager().getConnectedChannel().getAsMention() + "!", false, true);
            return;
        }

        if (event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if(channel == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        event.getGuild().getAudioManager().openAudioConnection(channel);
        reply(event, "✅ I have joined " + channel.getAsMention() + "!", false);
    }
}
