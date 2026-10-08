package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

import java.util.HashSet;
import java.util.Set;

public final class PollVotesRule implements QuestRule<PollVotesConfig, PollVotesState> {
    @Override
    public PollVotesState createState() {
        return new PollVotesState();
    }

    @Override
    public void apply(PollVotesConfig config, PollVotesState state, QuestEvent event) {
        if (event instanceof QuestEvent.PollActivity(String pollId, String voterId) && !voterId.isBlank()) {
            state.votersByPoll.computeIfAbsent(pollId, _ -> new HashSet<>()).add(voterId);
        }
    }

    @Override
    public QuestStatus status(PollVotesConfig config, PollVotesState state) {
        int mostVotes = state.votersByPoll.values().stream().mapToInt(Set::size).max().orElse(0);
        int progress = Math.min(mostVotes, config.requiredVotes());
        return new QuestStatus(progress, config.requiredVotes(), progress >= config.requiredVotes());
    }
}
