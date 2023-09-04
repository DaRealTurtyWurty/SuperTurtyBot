package dev.darealturtywurty.superturtybot.commands.music;

import java.util.List;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RemoveCommand extends CoreCommand {
    public RemoveCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.INTEGER, "index", "The track index in the queue", true).setMinValue(0));
    }
    
    @Override
    public String getAccess() {
        return "Moderators, Owner of song (if present in VC), Everyone (if the owner of the song is not in VC)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Removes a track from the music queue";
    }

    @Override
    public String getHowToUse() {
        return "/removequeue [index]";
    }

    @Override
    public String getName() {
        return "removequeue";
    }

    @Override
    public String getRichName() {
        return "Remove Queue";
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
            reply(event, "❌ The queue is currently empty!", false, true);
            return;
        }

        final int index = event.getOption("index").getAsInt();

        if (index < 1) {
            reply(event, "❌ The provided index must be at least **1**!", false, true);
            return;
        }

        if (index > queue.size()) {
            reply(event, "❌ No track was found at this index! The queue length is " + queue.size() + ".", false, true);
            return;
        }

        final AudioTrack removed = AudioManager.removeTrack(event.getGuild(), index - 1);
        if (removed == null) {
            reply(event, "❌ No track was found at this index!", false, true);
            return;
        }

        reply(event, removed.getInfo().title + " was removed from the queue!");
    }
}
