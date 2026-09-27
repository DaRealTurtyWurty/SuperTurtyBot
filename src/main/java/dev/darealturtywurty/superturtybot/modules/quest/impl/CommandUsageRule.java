package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.time.LocalDate;

public final class CommandUsageRule implements QuestRule<CommandUsageConfig, CommandUsageState> {
    @Override
    public CommandUsageState createState() {
        return new CommandUsageState();
    }

    @Override
    public void apply(CommandUsageConfig config, CommandUsageState state, QuestEvent event) {
        if (!(event instanceof QuestEvent.CommandUsed(String commandType, String category, LocalDate date)))
            return;

        if (config.requiredCategory() != null && !config.requiredCategory().equalsIgnoreCase(category))
            return;

        if (!config.acceptedCommandTypes().isEmpty() && !config.acceptedCommandTypes().contains(commandType))
            return;

        state.commandTypes.add(commandType);
        state.days.add(date);
    }

    @Override
    public QuestStatus status(CommandUsageConfig config, CommandUsageState state) {
        int commandProgress = Math.min(state.commandTypes.size(), config.requiredDistinctCommandTypes());
        int dayProgress = Math.min(state.days.size(), config.requiredDays());
        int progress = commandProgress + dayProgress;
        int target = config.requiredDistinctCommandTypes() + config.requiredDays();
        return new QuestStatus(progress, target, progress >= target);
    }
}
