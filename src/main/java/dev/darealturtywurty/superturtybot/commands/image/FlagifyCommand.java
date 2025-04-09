package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.request.ImageFlagifyRequestData;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FlagifyCommand extends CoreCommand {
    public FlagifyCommand() {
        super(new Types(false, false, false, true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Turns a user's avatar into a flag!";
    }

    @Override
    public String getName() {
        return "flagify";
    }

    @Override
    public String getRichName() {
        return "Flagify";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        Guild guild = event.getGuild();
        Member target = event.getTargetMember();
        if(!event.isFromGuild() || guild == null || target == null) {
            event.reply("❌ This command can only be used in a server!")
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply().queue();

        Either<BufferedImage, HttpStatus> response = ApiHandler.flagify(
                new ImageFlagifyRequestData.Builder(target.getEffectiveAvatar().getUrl())
                        .colors(5)
                        .build());
        if(response.isRight()) {
            event.getHook().editOriginal("❌ Failed to flagify user!")
                    .queue();
            Constants.LOGGER.error("Failed to flagify user: {}", response.getRight());
            return;
        }

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(response.getLeft(), "png", baos);
        } catch (IOException exception) {
            event.getHook().editOriginal("❌ Failed to flagify user!")
                    .queue();
            Constants.LOGGER.error("Failed to flagify user: {}", exception.getMessage());
            return;
        }

        byte[] data = baos.toByteArray();

        var upload = FileUpload.fromData(data, "flag.png");
        event.getHook().sendMessage("Here is %s as a flag!".formatted(target.getAsMention()))
                .setAllowedMentions(null)
                .addFiles(upload)
                .queue();
    }
}
