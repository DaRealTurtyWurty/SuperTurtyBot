package io.github.darealturtywurty.superturtybot.commands.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

// TODO: Fix not working for non-JPGs
public class CatCommand extends ImageCommand {
    public CatCommand() {
        super(new Types(true, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }

    @Override
    public String getDescription() {
        return "Random cat image";
    }

    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }

    @Override
    public String getName() {
        return "cat";
    }

    @Override
    public String getRichName() {
        return "Cat Image";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        try {
            final BufferedImage image = ImageIO.read(new URL("https://cataas.com/cat"));
            final var stream = new ByteArrayOutputStream();
            ImageIO.write(image, image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg", stream);
            event.getMessage()
                .reply(new ByteArrayInputStream(stream.toByteArray()),
                    "cat." + (image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg"))
                .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.getMessage().reply("There has been an issue gathering this cat image.").mentionRepliedUser(false)
                .queue();
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        try {
            final BufferedImage image = ImageIO.read(new URL("https://cataas.com/cat"));
            final var stream = new ByteArrayOutputStream();
            ImageIO.write(image, image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg", stream);
            event.deferReply()
                .addFile(new ByteArrayInputStream(stream.toByteArray()),
                    "cat." + (image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg"))
                .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("There has been an issue gathering this cat image.")
                .mentionRepliedUser(false).queue();
        }
    }
}
