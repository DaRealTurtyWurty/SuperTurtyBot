package dev.darealturtywurty.superturtybot.commands.test;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class TestCommand extends CoreCommand {
    public TestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "A test command!";
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getRichName() {
        return "Test";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Optional<Long> ownerId = Environment.INSTANCE.ownerId();
        if (ownerId.isEmpty()) {
            reply(event, "❌ The owner ID is not set!", false, true);
            return;
        }

        if (event.getUser().getIdLong() != ownerId.get()) {
            reply(event, "❌ You must be the owner of the bot to use this command!", false, true);
            return;
        }

        reply(event, "✅ Test command!");
    }
}
