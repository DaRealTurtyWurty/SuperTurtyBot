package io.github.darealturtywurty.superturtybot.commands.music;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.tuple.Pair;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PlayCommand extends CoreCommand {
    public PlayCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "search_term", "The song that you want to play", true));
    }
    
    @Override
    public String getAccess() {
        return "Everyone (unless queue is locked)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Plays the supplied music";
    }

    @Override
    public String getHowToUse() {
        return "/play [songName|ytURL|spotifyURL|soundcloudURL]";
    }
    
    @Override
    public String getName() {
        return "play";
    }
    
    @Override
    public String getRichName() {
        return "Play Music";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (!event.getGuild().getAudioManager().isConnected()) {
            event.getGuild().getAudioManager().openAudioConnection(channel);
            event.getChannel().sendMessage("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
                .queue();
        }
        
        if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            event.deferReply(true).setContent("❌ You must be in the same voice channel as me to play a song!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final String search = event.getOption("search_term").getAsString().trim();
        final CompletableFuture<Pair<Boolean, String>> future = AudioManager.play(channel,
            event.getChannel().asTextChannel(), search);
        future.thenAccept(pair -> event.deferReply(true).setContent(switch (pair.getValue()) {
            case "load_failed" -> "This track has failed to load. Please refer to the above message for more information!";
            case "playlist_loaded", "track_loaded" -> "Successfully added to the queue!";
            default -> "An unknown error has occured. Please notify the bot owner as this should not be possible!";
        }).mentionRepliedUser(false).queue());
    }
}
