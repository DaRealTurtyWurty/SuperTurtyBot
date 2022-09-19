package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import net.dv8tion.jda.api.JDA;

public class NoHupListener {
    public void start(JDA jda) throws URISyntaxException {
        if (!isDevelopmentEnvironment())
            return;

        final var runFolder = new File(TurtyBot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getParentFile();
        if (!runFolder.exists())
            return;
        
        final Path projectFolder = Path.of(runFolder.getAbsolutePath()).getParent().getParent();
        final Path nohupOut = Path.of(projectFolder.toString(), "nohup.out");
        if (!Files.exists(nohupOut))
            return;

        final var monitor = new FileMonitor(1000);
        monitor.monitor(projectFolder.toString(), "nohup.out", new FileChangedAdapter(jda));
        monitor.start();
    }
    
    private static boolean isDevelopmentEnvironment() {
        return System.getenv("development") != null;
    }
}
