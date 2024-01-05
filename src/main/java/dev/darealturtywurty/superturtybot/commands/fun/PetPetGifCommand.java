package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.object.PetPetGifCreator;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PetPetGifCommand extends CoreCommand {
    public PetPetGifCommand() {
        super(new Types(true, false, true, true));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(
                OptionType.STRING,
                "image",
                "The image to pet pet gif",
                true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Creates a pet pet gif with the given image/user avatar.";
    }

    @Override
    public String getName() {
        return "petpetgif";
    }

    @Override
    public String getRichName() {
        return "Pet Pet Gif";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String imageStr = event.getOption("image", null, OptionMapping::getAsString);
        if (imageStr == null) {
            reply(event, "❌ You must provide an image to pet pet gif!", false, true);
            return;
        }

        event.deferReply().setContent("⏸️ Creating pet pet gif...").queue();

        BufferedImage image;
        try {
            image = ImageIO.read(new URI(imageStr).toURL());
        } catch (IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ The image you provided is invalid!").queue();
            return;
        }

        CompletableFuture<Path> future = createPetPetGif(image);
        future.thenAccept(path -> {
            try {
                InputStream stream = Files.newInputStream(path);
                FileUpload upload = FileUpload.fromData(stream, "petpet.gif");
                event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
            } catch (IOException exception) {
                event.getHook().editOriginal("❌ An error occurred while creating the pet pet gif!")
                        .queue();
            }
        });
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Message message = event.getTarget();
        if (message.getAttachments().isEmpty()) {
            reply(event, "❌ You must provide an image to pet pet gif!", false);
            return;
        }

        event.deferReply().setContent("⏸️ Creating pet pet gif...").queue();

        List<Message.Attachment> attachments = message.getAttachments();
        for (Message.Attachment attachment : attachments) {
            if(attachment.isImage()) {
                attachment.getProxy().download().thenAccept(stream -> {
                    BufferedImage image;
                    try {
                        image = ImageIO.read(stream);
                    } catch (IOException exception) {
                        event.getHook().editOriginal("❌ The image you provided is invalid!").queue();
                        return;
                    }

                    CompletableFuture<Path> future = createPetPetGif(image);
                    future.thenAccept(path -> {
                        try {
                            InputStream outStream = Files.newInputStream(path);
                            FileUpload upload = FileUpload.fromData(outStream, "petpet.gif");
                            event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
                        } catch (IOException exception) {
                            event.getHook().editOriginal("❌ An error occurred while creating the pet pet gif!")
                                    .queue();
                        }
                    });
                }).exceptionally(exception -> {
                    event.getHook().editOriginal("❌ An error occurred while creating the pet pet gif!")
                            .queue();
                    return null;
                });

                return;
            }
        }
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        if(event.getTargetMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false);
            return;
        }

        String avatar = event.isFromGuild() ? event.getTargetMember().getEffectiveAvatarUrl() : event.getTarget().getEffectiveAvatarUrl();

        event.deferReply().setContent("⏸️ Creating pet pet gif...").queue();

        BufferedImage image;
        try {
            image = ImageIO.read(new URI(avatar).toURL());
        } catch (IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ The image you provided is invalid!").queue();
            return;
        }

        CompletableFuture<Path> future = createPetPetGif(image);
        future.thenAccept(path -> {
            try {
                InputStream stream = Files.newInputStream(path);
                FileUpload upload = FileUpload.fromData(stream, "petpet.gif");
                event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
            } catch (IOException exception) {
                event.getHook().editOriginal("❌ An error occurred while creating the pet pet gif!")
                        .queue();
            }
        });
    }

    private CompletableFuture<Path> createPetPetGif(BufferedImage inputImage) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile("petpet", ".gif");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create temp file!", exception);
        }

        return new PetPetGifCreator(tempFile, inputImage).start();
    }
}
