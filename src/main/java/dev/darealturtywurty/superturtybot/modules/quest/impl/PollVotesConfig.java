package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record PollVotesConfig(int requiredVotes) {
    public PollVotesConfig {
        if (requiredVotes < 1)
            throw new IllegalArgumentException("Required votes must be greater than 0.");
    }
}
