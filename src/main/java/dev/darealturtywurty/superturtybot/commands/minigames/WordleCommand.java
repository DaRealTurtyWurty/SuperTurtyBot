package dev.darealturtywurty.superturtybot.commands.minigames;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WordleCommand extends CoreCommand {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    static {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime next = now.withHour(3).withMinute(0).withSecond(0);
        if (now.compareTo(next) > 0)
            next = next.plusDays(1);

        Duration duration = Duration.between(now, next);
        long initialDelay = duration.getSeconds();

        EXECUTOR.scheduleAtFixedRate(new GenerateWordleTask(), initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    public WordleCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Play the daily game of Wordle! A game where you have to guess today's chosen word.";
    }

    @Override
    public String getName() {
        return "wordle";
    }

    @Override
    public String getRichName() {
        return "Wordle";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    private static class GenerateWordleTask implements Runnable {
        @Override
        public void run() {

        }
    }
}
