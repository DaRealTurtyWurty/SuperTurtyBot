package dev.darealturtywurty.superturtybot.dashboard.service.guild_config;

import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigOption;

public record GuildConfigOptionDescriptor(
        String key,
        String richName,
        String saveName,
        GuildConfigOption.DataType dataType
) {
}
