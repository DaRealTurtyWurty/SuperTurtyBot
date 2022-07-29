package io.github.darealturtywurty.superturtybot.commands.music;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RemoveDuplicatesCommand extends CoreCommand {
    public RemoveDuplicatesCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Removes duplicate songs from the queue";
    }
    
    @Override
    public String getName() {
        return "removedupes";
    }
    
    @Override
    public String getRichName() {
        return "Remove Duplicates";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        final Set<String> nonDuplicates = new HashSet<>();
        int removeCounter = 0;
        for (int index = 0; index < queue.size(); index++) {
            final AudioTrack track = queue.get(index);
            if (!nonDuplicates.contains(track.getInfo().uri)) {
                nonDuplicates.add(track.getInfo().uri);
                continue;
            }
            
            AudioManager.removeTrack(event.getGuild(), index);
            removeCounter++;
        }
        
        nonDuplicates.clear();
        if (removeCounter > 0) {
            reply(event, "✅ I have removed " + removeCounter + "tracks from the queue!");
        } else {
            reply(event, "❌ There are no duplicate tracks in the queue!", false, true);
        }
    }
}
