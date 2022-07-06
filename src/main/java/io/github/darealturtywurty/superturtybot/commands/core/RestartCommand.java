package io.github.darealturtywurty.superturtybot.commands.core;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RestartCommand extends CoreCommand {
    public RestartCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Restarts the bot";
    }
    
    @Override
    public String getName() {
        return "restart";
    }
    
    @Override
    public String getRichName() {
        return "Restart";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId())
            return;
        
        event.getMessage().reply("I am restarting! 👍 😎").mentionRepliedUser(false).queue();
        event.getJDA().shutdown();
        try {
            restartApplication();
        } catch (final URISyntaxException | IOException exception) {
            System.exit(-1);
        }
    }
    
    private static void restartApplication() throws URISyntaxException, IOException {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final File currentJar = new File(TurtyBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        
        /* is it a jar file? */
        if (!currentJar.getName().endsWith(".jar"))
            return;
        
        /* Build command: java -jar application.jar */
        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentJar.getPath());
        
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        System.exit(0);
    }
}
