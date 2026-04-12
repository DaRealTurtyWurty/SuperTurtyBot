package dev.darealturtywurty.superturtybot.dashboard.service.counting;

public record DashboardCountingChannelInfo(
        String channelId,
        String channelName,
        String mode,
        boolean connected,
        int currentCount,
        int highestCount
) {
}
