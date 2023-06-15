package dev.darealturtywurty.superturtybot.commands.fun;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
    protected void runSlash(SlashCommandInteractionEvent event) {
        String url = event.getOption("image").getAsString();
        event.deferReply().queue();

        try {
            BufferedImage deepfried = deepfry(url);
            FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
            event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
        } catch(IOException exception) {
            event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
        }
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        Message message = event.getTarget();
        if(message.getAttachments().isEmpty()) {
            event.reply("❌ You must provide an image to deepfry!").mentionRepliedUser(false).queue();
            return;
        }

        Message.Attachment attachment = message.getAttachments().stream().filter(Message.Attachment::isImage).findFirst().orElse(null);
        if(attachment == null) {
            event.reply("❌ You must provide an image to deepfry!").mentionRepliedUser(false).queue();
            return;
        }

        event.deferReply().queue();

        try {
            BufferedImage deepfried = deepfry(attachment.getUrl());
            FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
            event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
        } catch(IOException exception) {
            event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
        }
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        Member member = event.getTargetMember();
        String url = member != null ? member.getEffectiveAvatarUrl() : event.getTarget().getEffectiveAvatarUrl();

        event.deferReply().queue();

        try {
            BufferedImage deepfried = deepfry(url);
            FileUpload upload = FileUpload.fromData(toInputStream(deepfried), "deepfried.png");
            event.getHook().editOriginal("🔥 Deepfried Image 🔥").setFiles(upload).mentionRepliedUser(false).queue();
        } catch(IOException exception) {
            exception.printStackTrace();
            event.getHook().editOriginal("❌ Failed to deepfry image!").mentionRepliedUser(false).queue();
        }
    }

    private static BufferedImage deepfry(String url) throws IOException {
        String reqUrl = "https://nekobot.xyz/api/imagegen?type=deepfry&image=%s".formatted(url);
        InputStream stream = new URL(reqUrl).openStream();
        JsonObject json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
        String message = json.has("message") ? json.get("message").getAsString() : null;
        if(message == null) {
            Constants.LOGGER.error("Failed to deepfry image: {}", json);
            throw new IOException("No message present");
        }

        return ImageIO.read(new URL(json.get("message").getAsString()));
    }

    private static InputStream toInputStream(final RenderedImage image) throws IOException {
        final var outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
