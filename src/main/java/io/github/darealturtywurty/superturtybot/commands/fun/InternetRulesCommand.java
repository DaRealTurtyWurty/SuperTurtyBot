package io.github.darealturtywurty.superturtybot.commands.fun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class InternetRulesCommand extends CoreCommand {
    private static final List<String> RULES = new ArrayList<>();
    
    static {
        if (RULES.isEmpty()) {
            try {
                final InputStream stream = TurtyBot.class.getResourceAsStream("/rules_of_the_internet.txt");
                final var reader = new BufferedReader(new InputStreamReader(stream));
                if (reader.ready()) {
                    reader.lines().forEach(RULES::add);
                }
            } catch (final IOException exception) {
                Constants.LOGGER.error("There has been an issue parsing file:\nException: {}\n{}",
                    exception.getMessage(), ExceptionUtils.getStackTrace(exception));
            }
        }
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
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "Gets a rule from The Rules of The Internet.";
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
        final int number = event.getOption("rule_number").getAsInt();
        event.deferReply().setContent(RULES.get(number - 1)).mentionRepliedUser(false).queue();
    }
}
