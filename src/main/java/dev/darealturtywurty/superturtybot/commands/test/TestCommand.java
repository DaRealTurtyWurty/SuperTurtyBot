package dev.darealturtywurty.superturtybot.commands.test;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.util.WikipediaCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import io.github.fastily.jwiki.core.NS;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        try (InputStream celebritiesStream = TurtyBot.loadResource("celebrities.txt")) {
            if(celebritiesStream == null) {
                Constants.LOGGER.error("The celebrities file was not found!");
                reply(event, "❌ The celebrities file was not found!", false, true);
                return;
            }

            String[] celebrities = new String(celebritiesStream.readAllBytes()).split("\n");

            for (String celebrity : celebrities) {
                celebrity = celebrity.strip();

                Path imagePath = Path.of("E:\\celebrities\\" + celebrity + ".png");
                try {
                    if(Files.exists(imagePath)) {
                        Constants.LOGGER.info("The image for {} already exists!", celebrity);
                        continue;
                    }
                } catch (InvalidPathException exception) {
                    Constants.LOGGER.error("Invalid path for: {}", celebrity, exception);
                    continue;
                }

                Optional<String> found = WikipediaCommand.WIKI.search(celebrity, 1, NS.MAIN)
                        .stream()
                        .filter(page -> page != null && !page.isBlank())
                        .findFirst();

                if (found.isEmpty()) {
                    Constants.LOGGER.warn("No Wikipedia page found for: {}", celebrity);
                    continue;
                }

                String page = found.get();
                String imageUrl = WikipediaCommand.getImageUrl(page);
                if (imageUrl == null) {
                    Constants.LOGGER.warn("No image found for: {}", celebrity);
                    continue;
                }

                BufferedImage asImage = ImageIO.read(new URI(imageUrl).toURL());
                try {
                    Files.createDirectories(imagePath.getParent());
                    ImageIO.write(asImage, "png", imagePath.toFile());
                } catch (IOException exception) {
                    Constants.LOGGER.error("An error occurred while trying to save the image for: {}", celebrity, exception);
                }

                Constants.LOGGER.info("Successfully loaded the image for: {}", celebrity);
            }

            Constants.LOGGER.info("Successfully saved the images!");
        } catch (IOException | URISyntaxException exception) {
            Constants.LOGGER.error("An error occurred while trying to load the celebrities file!", exception);
            reply(event, "❌ An error occurred while trying to load the celebrities file!", false, true);
        }
    }
}
