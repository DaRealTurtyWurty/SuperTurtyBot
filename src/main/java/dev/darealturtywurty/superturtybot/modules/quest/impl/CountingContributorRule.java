package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class CountingContributorRule implements QuestRule<CountingContributorConfig, CountingContributorState> {
    @Override
    public CountingContributorState createState() {
        return new CountingContributorState();
    }

    @Override
    public void apply(CountingContributorConfig config, CountingContributorState state, QuestEvent event) {
        if (event instanceof QuestEvent.ValidCount(String messageId, var date)) {
            state.counts.putIfAbsent(messageId, date);
        }
    }

    @Override
    public QuestStatus status(CountingContributorConfig config, CountingContributorState state) {
        int countProgress = Math.min(state.counts.size(), config.requiredCounts());
        int dayProgress = Math.min(
            Math.toIntExact(state.counts.values().stream().distinct().count()),
            config.requiredDays());
        boolean complete = countProgress >= config.requiredCounts() && dayProgress >= config.requiredDays();
        String progressText = countProgress + "/" + config.requiredCounts() + " counts • "
            + dayProgress + "/" + config.requiredDays() + " days";
        return new QuestStatus(countProgress, config.requiredCounts(), complete, progressText);
    }
}
