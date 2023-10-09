package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class WouldYouRatherCommand extends CoreCommand {
    private static final List<String> QUESTIONS = new ArrayList<>();

    static {
        try {
            final InputStream stream = TurtyBot.class.getResourceAsStream("/would_you_rathers.txt");
            if (stream != null) {
                final var reader = new BufferedReader(new InputStreamReader(stream));
                if (reader.ready()) {
                    reader.lines().forEach(QUESTIONS::add);
                }
            }
        } catch (final IOException exception) {
            Constants.LOGGER.error("❌ There has been an issue parsing would_you_rathers.txt", exception);
        }
    }

    public WouldYouRatherCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
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
        event.deferReply().setContent(getRandomQuestion()).mentionRepliedUser(false)
            .queue(hook -> hook.retrieveOriginal().queue(msg -> {
                msg.addReaction(Emoji.fromUnicode("U+1F170")).queue();
                msg.addReaction(Emoji.fromUnicode("U+1F171")).queue();
            }));
    }

    public static String getRandomQuestion() {
        return QUESTIONS.get(ThreadLocalRandom.current().nextInt(QUESTIONS.size()))
                .replace("{a}", "🅰️")
                .replace("{b}", "🅱️");
    }
}
