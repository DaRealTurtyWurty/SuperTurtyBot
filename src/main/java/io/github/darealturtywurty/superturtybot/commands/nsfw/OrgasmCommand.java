package io.github.darealturtywurty.superturtybot.commands.nsfw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class OrgasmCommand extends NSFWCommand {
    public static final String PATH = "src/main/resources/audio/orgasms/";
    
    public OrgasmCommand() {
        super(NSFWCategory.MISC);
    }
    
    @Override
    public String getDescription() {
        return "Plays an orgasm sound in VC!";
    }
    
    @Override
    public String getName() {
        return "orgasm";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getTextChannel().isNSFW())
            return;
        
        // Essential
        super.runNormalMessage(event);
        
        if (!event.isFromGuild() || !event.getMember().getVoiceState().inAudioChannel())
            return;
        
        List<File> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(Path.of(PATH))) {
            files = paths.map(Path::toFile).collect(Collectors.toList());
        } catch (final IOException exception) {
            event.getChannel()
                .sendMessage("There has been an issue with the `" + Environment.INSTANCE.defaultPrefix()
                    + "orgasm` command! Please report the following to the bot owner:\n" + exception.getMessage() + "\n"
                    + ExceptionUtils.getMessage(exception))
                .queue();
            return;
        }
        
        Collections.shuffle(files);
        AudioManager.play(event.getGuild(), event.getMember().getVoiceState().getChannel(), files.get(0));
    }
}
