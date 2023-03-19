package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.handler.LoopState;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class LoopCommand extends CoreCommand {

    public LoopCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("get", "Get the current loop state"),
                new SubcommandData("set", "Set the loop state"), new SubcommandData("toggle", "Toggle the loop state"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Change the loop state of the music player";
    }

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getRichName() {
        return "Loop";
    }

    @Override
    public String getHowToUse() {
        return "loop <get/set/toggle>";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel() || voiceState.getChannel() == null) {
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

        String sub = event.getSubcommandName();
        if (sub == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        if (!checkOwnsAll(AudioManager.getQueue(event.getGuild()), event.getMember()) && !event.getMember()
                .hasPermission(channel, Permission.MANAGE_CHANNEL) && channel.getMembers().size() > 2) {
            reply(event, "❌ You must have the `Manage Channel` permission to use this command!", false, true);
            return;
        }

        switch (sub) {
            case "get" ->
                    reply(event, "The current loop state is: " + AudioManager.getLoopState(event.getGuild()), false,
                            true);
            case "set" -> {
                String loopStateStr = event.getOption("state", null, OptionMapping::getAsString);
                LoopState loopState = LoopState.fromString(loopStateStr);
                if (loopState == null) {
                    reply(event, "❌ You must specify a loop state!", false, true);
                    return;
                }

                AudioManager.setLoopState(event.getGuild(), loopState);
                reply(event, "The loop state has been set to: " + LoopState.asString(loopState), false, true);
            }
            case "toggle" -> {
                LoopState loopState = AudioManager.toggleLoopState(event.getGuild());
                reply(event, "The loop state has been set to: " + LoopState.asString(loopState), false, true);
            }
        }
    }

    private static boolean checkOwnsAll(List<AudioTrack> queue, Member member) {
        long trackMatch = queue.stream().map(track -> track.getUserData(Long.class))
                .filter(owner -> owner != null && owner == member.getIdLong()).count();
        return trackMatch == queue.size();
    }
}
