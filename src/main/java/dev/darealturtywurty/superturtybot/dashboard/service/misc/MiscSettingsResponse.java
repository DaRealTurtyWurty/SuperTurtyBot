package dev.darealturtywurty.superturtybot.dashboard.service.misc;

public record MiscSettingsResponse(
        boolean shouldCreateGists,
        boolean shouldSendStartupMessage,
        boolean shouldSendChangelog,
        boolean stickyRolesEnabled,
        String patronRoleId
) {
}
