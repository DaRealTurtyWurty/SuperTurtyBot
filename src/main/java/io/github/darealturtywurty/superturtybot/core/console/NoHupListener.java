package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import net.dv8tion.jda.api.JDA;

public class NoHupListener {
    public void start(JDA jda) throws URISyntaxException {
        jda.getTextChannelById(1021457564849942548L)
            .sendMessage("Starting nohup.out listener internally! " + isDevelopmentEnvironment()).queue();
        if (!isDevelopmentEnvironment())
            return;
        
        jda.getTextChannelById(1021457564849942548L).sendMessage("I am in production!").queue();
        
        final var runFolder = new File(TurtyBot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getParentFile();
        jda.getTextChannelById(1021457564849942548L).sendMessage("The run folder is: " + runFolder).queue();
        if (!runFolder.exists())
            return;
        
        jda.getTextChannelById(1021457564849942548L).sendMessage("I've found a valid run folder!").queue();
        
        final Path projectFolder = Path.of(runFolder.getAbsolutePath()).getParent().getParent();
        jda.getTextChannelById(1021457564849942548L).sendMessage("The project folder is: " + projectFolder).queue();
        final Path nohupOut = Path.of(projectFolder.toString(), "nohup.out");
        jda.getTextChannelById(1021457564849942548L).sendMessage("The nohup.out file is: " + nohupOut).queue();
        if (!Files.exists(nohupOut))
            return;
        
        jda.getTextChannelById(1021457564849942548L).sendMessage("I've found the nohup.out!").queue();

        final var monitor = new FileMonitor(1000);
        monitor.monitor(projectFolder.toString(), "nohup.out", new FileChangedAdapter(jda));
        monitor.start();
        
        jda.getTextChannelById(1021457564849942548L).sendMessage("File Monitor added!").queue();
    }
    
    private static boolean isDevelopmentEnvironment() {
        return System.getenv("development") != null;
    }
}
