package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record CommunityExplorerConfig(int triviaQuestions, int minigames, int validCounts) {
    public CommunityExplorerConfig {
        if (triviaQuestions < 0 || minigames < 0 || validCounts < 0)
            throw new IllegalArgumentException("Community explorer requirements cannot be negative");

        if (triviaQuestions + minigames + validCounts == 0)
            throw new IllegalArgumentException("Community explorer must require at least one activity");
    }
}
