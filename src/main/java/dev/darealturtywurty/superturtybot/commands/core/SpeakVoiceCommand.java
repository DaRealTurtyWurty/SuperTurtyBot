package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpeakVoiceCommand extends CoreCommand {
    private static final LocalMaryInterface MARY;

    static {
        try {
            MARY = new LocalMaryInterface();
        } catch (MaryConfigurationException exception) {
            throw new IllegalStateException("Could not initialize MaryTTS interface", exception);
        }
    }

    public SpeakVoiceCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Makes the bot speaking using voice!";
    }

    @Override
    public String getName() {
        return "speakvoice";
    }

    @Override
    public String getRichName() {
        return "Speak Voice";
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if(event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId().orElseThrow(() -> new IllegalStateException("Owner ID is not set!")))
            return;

        event.getMessage().delete().queue();

        int commandIndex = event.getMessage().getContentRaw().indexOf(getName());
        String content = event.getMessage().getContentRaw().substring(commandIndex + getName().length());

        byte[] bytes = tts(content, "speakvoice-" + event.getChannel().getId() + "-" + event.getAuthor().getId());
        var upload = FileUpload.fromData(bytes, "audio.wav");
        event.getChannel().sendFiles(upload).queue();
    }

    public static byte[] tts(String text, String fileName) {
        AudioInputStream audio = ttsStream(text);
        return getBytes(audio, fileName + ".wav", true);
    }

    public static AudioInputStream ttsStream(String text) {
        try {
            return MARY.generateAudio(text);
        } catch (SynthesisException exception) {
            throw new IllegalStateException("Could not synthesize audio", exception);
        }
    }

    public static byte[] getBytes(AudioInputStream audio, String fileName, boolean deleteFile) throws IllegalStateException {
        double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);

        var path = new File(fileName);
        try {
            MaryAudioUtils.writeWavFile(samples, path.toString(), audio.getFormat());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write audio to file", exception);
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(path.toURI()));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read audio file", exception);
        }

        if (deleteFile) {
            try {
                Files.deleteIfExists(path.toPath());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not delete audio file", exception);
            }
        }

        return bytes;
    }
}
