package dev.darealturtywurty.superturtybot.commands.core;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class RestartCommand extends CoreCommand {
    public RestartCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
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
        if(event.getAuthor().getIdLong() != Environment.INSTANCE.ownerId().orElseThrow(() -> new IllegalStateException("Owner ID is not set!")))
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
