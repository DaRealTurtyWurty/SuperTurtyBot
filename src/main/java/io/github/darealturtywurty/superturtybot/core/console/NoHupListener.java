package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.JDA;

public class NoHupListener {
    public void start(JDA jda) throws URISyntaxException {
        System.out.println(isDevelopmentEnvironment());
        if (!isDevelopmentEnvironment())
            return;

        Constants.LOGGER.debug("Not Development");

        final var runFolder = new File(TurtyBot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getParentFile();
        Constants.LOGGER.debug(runFolder.toString());
        if (!runFolder.exists())
            return;

        Constants.LOGGER.debug("Found run folder");

        final Path projectFolder = Path.of(runFolder.getAbsolutePath()).getParent().getParent();
        Constants.LOGGER.debug(projectFolder.toString());
        final Path nohupOut = Path.of(projectFolder.toString(), "nohup.out");
        Constants.LOGGER.debug(nohupOut.toString());
        if (!Files.exists(nohupOut))
            return;

        Constants.LOGGER.debug("nohup.out exists");
        
        final var monitor = new FileMonitor(1000);
        monitor.monitor(projectFolder.toString(), "nohup.out", new FileChangedAdapter(jda));
        monitor.start();

        Constants.LOGGER.debug("File Monitor started");
    }

    private static boolean isDevelopmentEnvironment() {
        return System.getenv("development") != null;
    }
}
