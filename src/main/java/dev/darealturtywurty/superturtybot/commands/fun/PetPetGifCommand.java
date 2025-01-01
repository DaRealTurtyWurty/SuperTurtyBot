package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.object.PetPetGifCreator;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PetPetGifCommand extends CoreCommand {
    public PetPetGifCommand() {
        super(new Types(true, false, true, true));
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
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("user", "Creates a pet pet gif with the given user avatar.")
                        .addOption(OptionType.USER, "user", "The user to pet pet gif", true),
                new SubcommandData("image", "Creates a pet pet gif with the given image.")
                        .addOption(OptionType.STRING, "image", "The image to pet pet gif", true)
        );
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        reply(event, "⏸️ Creating pet pet gif...");
        String imageStr;
        switch (subcommand) {
            case "user":
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                if (user == null) {
                    reply(event, "❌ You must provide a user to pet pet gif!", false, true);
                    return;
                }
                imageStr = user.getEffectiveAvatar().getUrl();
                break;
            case "image":
                imageStr = event.getOption("image", null, OptionMapping::getAsString);
                if (imageStr == null) {
                    reply(event, "❌ You must provide an image to pet pet gif!", false, true);
                    return;
                }
                break;
            default:
                imageStr = "";
                break;
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new URI(imageStr).toURL());
        } catch (IOException | URISyntaxException | IllegalArgumentException exception) {
            event.getHook().editOriginal("❌ The image you provided is invalid!").queue();
            return;
        }
        createPetPetGif(image).thenAccept(baos -> {
            FileUpload upload = FileUpload.fromData(baos.toByteArray(), "petpet.gif");
            event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
        });
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Message message = event.getTarget();
        if (message.getAttachments().isEmpty()) {
            reply(event, "❌ You must provide an image to pet pet gif!", false);
            return;
        }

        reply(event, "⏸️ Creating pet pet gif...");

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

                    createPetPetGif(image).thenAccept(baos -> {
                        FileUpload upload = FileUpload.fromData(baos.toByteArray(), "petpet.gif");
                        event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
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

        reply(event, "⏸️ Creating pet pet gif...");

        BufferedImage image;
        try {
            image = ImageIO.read(new URI(avatar).toURL());
        } catch (IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ The image you provided is invalid!").queue();
            return;
        }

        createPetPetGif(image).thenAccept(baos -> {
            FileUpload upload = FileUpload.fromData(baos.toByteArray(), "petpet.gif");
            event.getHook().editOriginal("✅ Here is your pet pet gif!").setFiles(upload).queue();
        });
    }

    private CompletableFuture<ByteArrayOutputStream> createPetPetGif(BufferedImage inputImage) {
        return new PetPetGifCreator(new ByteArrayOutputStream(), inputImage).start();
    }
}
