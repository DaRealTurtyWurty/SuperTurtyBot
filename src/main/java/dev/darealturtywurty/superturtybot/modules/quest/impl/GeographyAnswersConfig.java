package dev.darealturtywurty.superturtybot.modules.quest.impl;

public record GeographyAnswersConfig(int requiredCorrectAnswers, int requiredGameTypes) {
    public GeographyAnswersConfig {
        if (requiredCorrectAnswers < 0 || requiredGameTypes < 0)
            throw new IllegalArgumentException("Geography requirements cannot be negative");

        if (requiredCorrectAnswers + requiredGameTypes == 0)
            throw new IllegalArgumentException("A geography quest must have at least one requirement");
    }
}
