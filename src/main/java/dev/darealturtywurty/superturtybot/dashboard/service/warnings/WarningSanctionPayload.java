package dev.darealturtywurty.superturtybot.dashboard.service.warnings;

public record WarningSanctionPayload(
        String id,
        String type,
        int warningCount,
        long durationMinutes,
        int deleteMessageDays
) {
}
