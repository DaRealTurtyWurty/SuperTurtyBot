package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SmashOrPassCommand extends CoreCommand {
    public SmashOrPassCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Decide whether you would smash or pass on a celebrity!";
    }

    @Override
    public String getName() {
        return "smashorpass";
    }

    @Override
    public String getRichName() {
        return "Smash or Pass";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        Either<Pair<String, byte[]>, HttpStatus> result = ApiHandler.getRandomCelebrity();
        if (result.isRight()) {
            event.getHook().sendMessage("❌ An error occurred while trying to get a random celebrity!").queue();
            Constants.LOGGER.error("An error occurred while trying to get a random celebrity! Code: {}", result.getRight());
            return;
        }

        Pair<String, byte[]> pair = result.getLeft();
        String name = pair.getLeft();
        byte[] image = pair.getRight();

        try (FileUpload upload = FileUpload.fromData(image, name + ".jpg")) {
            event.getHook()
                    .sendMessage("Would you smash or pass on **" + name + "**?")
                    .setFiles(upload)
                    .queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while trying to send the image!").queue();
            Constants.LOGGER.error("An error occurred while trying to send the image!", exception);
        }
    }
}
