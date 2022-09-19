package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class FileChangedAdapter extends FileAlterationListenerAdaptor {
    private final JDA jda;
    private AtomicReference<String> previousLog = new AtomicReference<>("");

    public FileChangedAdapter(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onFileChange(File file) {
        final TextChannel channel = this.jda.getTextChannelById(1021457564849942548L);
        if (channel == null)
            return;
        
        try {
            final String content = Files.readString(file.toPath()).replace(this.previousLog.get(), "");
            channel.sendMessage(content).queue();
        } catch (final IOException exception) {
            
        }
    }
}
