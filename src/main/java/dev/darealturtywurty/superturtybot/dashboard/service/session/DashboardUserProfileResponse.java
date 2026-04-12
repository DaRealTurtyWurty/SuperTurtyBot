package dev.darealturtywurty.superturtybot.dashboard.service.session;

import java.util.List;

public record DashboardUserProfileResponse(
        String userId,
        Birthday birthday,
        Wordle wordle,
        Collectables collectables,
        List<EconomyEntry> economy
) {
    public record Birthday(int day, int month, int year) {
    }

    public record Wordle(List<Streak> streaks) {
    }

    public record Streak(String guildId, int streak, int bestStreak, boolean hasPlayedToday) {
    }

    public record Collectables(List<Group> collectables) {
    }

    public record Group(String type, List<String> collectables) {
    }

    public record EconomyEntry(
            String guildId,
            String currency,
            String bank,
            String wallet,
            int crimeLevel,
            int heistLevel,
            String job,
            int jobLevel
    ) {
    }
}
