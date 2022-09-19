package io.github.darealturtywurty.superturtybot.core.console;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.util.BotUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class NoHupListener {
    public void start(JDA jda) throws URISyntaxException {
        final TextChannel channel = jda.getTextChannelById(1021457564849942548L);
        if (channel == null || !channel.canTalk())
            return;
        
        channel.sendMessage("Starting nohup.out listener! Development: " + BotUtils.isDevelopmentEnvironment()).queue();
        if (BotUtils.isDevelopmentEnvironment())
            return;
        
        channel.sendMessage("I am in production!").queue();
        
        final var runFolder = new File(TurtyBot.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getParentFile();
        channel.sendMessage("The run folder is: " + runFolder).queue();
        if (!runFolder.exists()) {
            channel.sendMessage("The run folder was not found!").queue();
            return;
        }
        
        channel.sendMessage("I've found a valid run folder!").queue();
        
        final Path projectFolder = Path.of(runFolder.getAbsolutePath()).getParent().getParent();
        channel.sendMessage("The project folder is: " + projectFolder).queue();
        final Path nohupOut = Path.of(projectFolder.toString(), "nohup.out");
        channel.sendMessage("The nohup.out file is: " + nohupOut).queue();
        if (!Files.exists(nohupOut)) {
            channel.sendMessage("nohup.out was not found!").queue();
            return;
        }
        
        channel.sendMessage("I've found the nohup.out!").queue();

        final var monitor = new FileMonitor(1000);
        monitor.monitor(projectFolder.toString(), "nohup.out", new FileChangedAdapter(jda));
        monitor.start();
        
        channel.sendMessage("File Monitor added!").queue();
    }
}
