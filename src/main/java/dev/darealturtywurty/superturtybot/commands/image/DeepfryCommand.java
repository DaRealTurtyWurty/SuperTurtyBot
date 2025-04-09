package dev.darealturtywurty.superturtybot.commands.image;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeepfryCommand extends CoreCommand {
    public DeepfryCommand() {
        super(new Types(true, false, true, true));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "image", "The image to deepfry", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Deepfries an image";
    }

    @Override
    public String getName() {
        return "deepfry";
    }

    @Override
    public String getRichName() {
        return "Deepfry";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String url = event.getOption("image", "how tf did you manage this?", OptionMapping::getAsString);
        event.deferReply().queue();

        try {
            BufferedImage deepfried = deepfry(url);
            FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
            event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
        } catch(IOException | URISyntaxException exception) {
            event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
            Constants.LOGGER.error("Failed to deepfry image", exception);
        }
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Message message = event.getTarget();
        if(message.getAttachments().isEmpty()) {
            event.reply("❌ You must provide an image to deepfry!").mentionRepliedUser(false).queue();
            return;
        }

        try(Message.Attachment attachment = message.getAttachments()
                .stream()
                .filter(Message.Attachment::isImage)
                .findFirst()
                .orElse(null)) {
            if (attachment == null) {
                event.reply("❌ You must provide an image to deepfry!").mentionRepliedUser(false).queue();
                return;
            }

            event.deferReply().queue();

            try {
                BufferedImage deepfried = deepfry(attachment.getUrl());
                FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
                event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
            } catch (IOException | URISyntaxException exception) {
                event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
                Constants.LOGGER.error("Failed to deepfry image", exception);
            }
        }
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        Member member = event.getTargetMember();
        String url = member != null ? member.getEffectiveAvatar().getUrl() : event.getTarget().getEffectiveAvatar().getUrl();

        event.deferReply().queue();

        try {
            BufferedImage deepfried = deepfry(url);
            FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
            event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
        } catch(IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to deepfry image", exception);
            event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
        }
    }

    private static BufferedImage deepfry(String url) throws IOException, URISyntaxException {
        String reqUrl = "https://nekobot.xyz/api/imagegen?type=deepfry&image=%s".formatted(url);
        InputStream stream = new URI(reqUrl).toURL().openStream();
        JsonObject json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
        String message = json.has("message") ? json.get("message").getAsString() : null;
        if(message == null) {
            Constants.LOGGER.error("Failed to deepfry image: {}", json);
            throw new IOException("No message present");
        }

        return ImageIO.read(new URI(json.get("message").getAsString()).toURL());
    }

    private static InputStream toInputStream(final RenderedImage image) throws IOException {
        final var outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
