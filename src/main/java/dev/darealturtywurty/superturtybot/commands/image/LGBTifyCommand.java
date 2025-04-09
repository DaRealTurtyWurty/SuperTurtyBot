package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
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

public class LGBTifyCommand extends CoreCommand {
    public LGBTifyCommand() {
        super(new Types(false, false, false, true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Turns a user's avatar into a LGBT flag!";
    }

    @Override
    public String getName() {
        return "lgbtify";
    }

    @Override
    public String getRichName() {
        return "LGBTify";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
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

        Either<BufferedImage, HttpStatus> response = ApiHandler.lgbtify(target.getEffectiveAvatar().getUrl());
        if(response.isRight()) {
            event.getHook().editOriginal("❌ Failed to LGBTify user!")
                    .queue();
            Constants.LOGGER.error("Failed to LGBTify user: {}", response.getRight());
            return;
        }

        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(response.getLeft(), "png", baos);
        } catch (IOException exception) {
            event.getHook().editOriginal("❌ Failed to LGBTify user!")
                    .queue();
            Constants.LOGGER.error("Failed to LGBTify user: {}", exception.getMessage());
            return;
        }

        byte[] data = baos.toByteArray();

        var upload = FileUpload.fromData(data, "lgbt.png");
        event.getHook().sendMessage("Here is an LGBTified version of %s!".formatted(target.getAsMention()))
                .setAllowedMentions(null)
                .addFiles(upload)
                .queue();
    }
}
