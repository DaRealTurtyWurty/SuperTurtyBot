package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InternetRulesCommand extends CoreCommand {
    private static final List<String> RULES = new ArrayList<>();

    static {
        new Thread(() -> {
            try(final InputStream stream = TurtyBot.loadResource("rules_of_the_internet.txt")) {
                if (stream == null)
                    throw new IllegalStateException("Could not find rules_of_the_internet.txt!");

                final var reader = new BufferedReader(new InputStreamReader(stream));
                if (reader.ready()) {
                    reader.lines().forEach(RULES::add);
                }
            } catch (final IOException exception) {
                Constants.LOGGER.error("There has been an issue parsing file: rules_of_the_internet.txt", exception);
            }
        }).start();
    }

    public InternetRulesCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.INTEGER, "rule_number", "The rule number", true).setRequiredRange(1, 100));
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 1L);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Gets a rule from The Rules of The Internet.";
    }

    @Override
    public String getHowToUse() {
        return "/internetrule [ruleNumber]";
    }

    @Override
    public String getName() {
        return "internetrule";
    }

    @Override
    public String getRichName() {
        return "Internet Rule";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final int number = event.getOption("rule_number", 0, OptionMapping::getAsInt);
        if(number < 1 || number > RULES.size()) {
            reply(event, "❌ You must supply a rule number between 1 and " + RULES.size() + "!", false, true);
            return;
        }

        reply(event, RULES.get(number - 1), false);
    }
}
