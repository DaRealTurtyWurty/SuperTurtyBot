package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.WouldYouRather;
import dev.darealturtywurty.superturtybot.core.api.request.WouldYouRatherRequest;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WouldYouRatherCommand extends CoreCommand {
    public WouldYouRatherCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.BOOLEAN, "nsfw", "Whether or not to get a NSFW question.", false),
                new OptionData(OptionType.BOOLEAN, "include-nsfw", "Whether or not to include NSFW questions.", false)
        );
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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    public String getHowToUse() {
        return "/wouldyourather [nsfw (default=false)] [include-nsfw (default=true)]";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        boolean nsfw = event.getOption("nsfw", false, OptionMapping::getAsBoolean);
        boolean includeNsfw = event.getOption("include-nsfw", true, OptionMapping::getAsBoolean);

        if(nsfw && !NSFWCommand.isValidChannel(event.getChannel())) {
            event.getHook().editOriginal("❌ This command can only be used in NSFW channels!").queue();
            return;
        }

        String question = getRandomQuestion(nsfw, includeNsfw);
        if (question.equals("An error occurred while fetching a question!")) {
            event.getHook().editOriginal(question).queue();
            return;
        }

        event.getHook().editOriginal(question).queue(msg -> {
            msg.addReaction(Emoji.fromUnicode("U+1F170")).queue();
            msg.addReaction(Emoji.fromUnicode("U+1F171")).queue();
        });
    }

    public static String getRandomQuestion(boolean nsfw, boolean includeNsfw) {
        WouldYouRatherRequest request = nsfw ?
                WouldYouRatherRequest.nsfw() :
                includeNsfw ?
                        WouldYouRatherRequest.randomlyNsfw() :
                        WouldYouRatherRequest.sfw();

        Either<WouldYouRather, HttpStatus> response = ApiHandler.getRandomWouldYouRather(request);
        int attempts = 0;
        while (response.isRight() && attempts++ < 5) {
            response = ApiHandler.getRandomWouldYouRather(request);
        }

        if (response.isRight()) {
            return "An error occurred while fetching a question!";
        }

        WouldYouRather wouldYouRather = response.getLeft();
        return "**Would you rather...**\n\n" + wouldYouRather.optionA() + " 🅰️\n\n**OR**\n\n" + wouldYouRather.optionB() + " 🅱️";
    }
}
