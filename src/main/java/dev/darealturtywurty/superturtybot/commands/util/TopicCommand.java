package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
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

public class TopicCommand extends CoreCommand {
    private static final List<String> TOPICS = new ArrayList<>();

    static {
        new Thread(() -> {
            try(final InputStream stream = TurtyBot.loadResource("topics.txt")) {
                if (stream == null)
                    throw new IllegalStateException("Unable to load topics.txt");

                final var reader = new BufferedReader(new InputStreamReader(stream));
                if (reader.ready()) {
                    reader.lines().forEach(TOPICS::add);
                }
            } catch (final IOException exception) {
                Constants.LOGGER.error("❌ There has been an issue parsing topics.txt", exception);
            }
        }).start();
    }

    public TopicCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets a random topic to talk about. Great for a conversation starter.";
    }

    @Override
    public String getName() {
        return "topic";
    }

    @Override
    public String getRichName() {
        return "Topic";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, getRandomTopic());
    }

    public static String getRandomTopic() {
        return TOPICS.get(ThreadLocalRandom.current().nextInt(TOPICS.size()));
    }
}
