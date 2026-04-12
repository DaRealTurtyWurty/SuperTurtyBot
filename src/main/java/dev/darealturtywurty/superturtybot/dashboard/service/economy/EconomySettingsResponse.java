package dev.darealturtywurty.superturtybot.dashboard.service.economy;

public record EconomySettingsResponse(
        String economyCurrency,
        boolean economyEnabled,
        boolean donateEnabled,
        String defaultEconomyBalance,
        float incomeTax
) {
}
