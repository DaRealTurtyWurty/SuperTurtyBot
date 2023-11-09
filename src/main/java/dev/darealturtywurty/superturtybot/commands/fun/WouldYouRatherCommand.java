package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.WouldYouRather;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class WouldYouRatherCommand extends CoreCommand {
    public WouldYouRatherCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Would you rather?";
    }

    @Override
    public String getName() {
        return "wouldyourather";
    }

    @Override
    public String getRichName() {
        return "Would You Rather";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        String question = getRandomQuestion();
        if (question.equals("An error occurred while fetching a question!")) {
            event.getHook().editOriginal(question).queue();
            return;
        }

        event.getHook().editOriginal(question).queue(msg -> {
            msg.addReaction(Emoji.fromUnicode("U+1F170")).queue();
            msg.addReaction(Emoji.fromUnicode("U+1F171")).queue();
        });
    }

    public static String getRandomQuestion() {
        Either<WouldYouRather, HttpStatus> response = ApiHandler.getRandomWouldYouRather();
        int attempts = 0;
        while (response.isRight() && attempts++ < 5) {
            response = ApiHandler.getRandomWouldYouRather();
        }

        if (response.isRight()) {
            return "An error occurred while fetching a question!";
        }

        WouldYouRather wouldYouRather = response.getLeft();
        return "**Would you rather...**\n\n" + wouldYouRather.optionA() + " 🅰️\n\n**OR**\n\n" + wouldYouRather.optionB() + " 🅱️";
    }
}
