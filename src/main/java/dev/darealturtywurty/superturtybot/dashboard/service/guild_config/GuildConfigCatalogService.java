package dev.darealturtywurty.superturtybot.dashboard.service.guild_config;

import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigOption;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigRegistry;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuildConfigCatalogService {
    private final List<Map.Entry<String, GuildConfigOption>> sortedOptions;
    private final List<GuildConfigOptionDescriptor> descriptors;

    public GuildConfigCatalogService() {
        this.sortedOptions = GuildConfigRegistry.GUILD_CONFIG_OPTIONS.getRegistry().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        this.descriptors = this.sortedOptions.stream()
                .map(entry -> new GuildConfigOptionDescriptor(
                        entry.getKey(),
                        entry.getValue().getRichName(),
                        entry.getValue().getSaveName(),
                        entry.getValue().getDataType()
                ))
                .toList();
    }

    public List<GuildConfigOptionDescriptor> listOptions() {
        return this.descriptors;
    }

    public Map<String, Object> extractValues(GuildData guildData) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, GuildConfigOption> entry : this.sortedOptions) {
            values.put(entry.getKey(), normalizeValue(entry.getValue().getValueFromConfig().apply(guildData)));
        }

        return Collections.unmodifiableMap(values);
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Long || value instanceof BigInteger)
            return value.toString();

        if (value instanceof List<?> list)
            return list.stream().map(GuildConfigCatalogService::normalizeValue).toList();

        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> normalized.put(String.valueOf(key), normalizeValue(entryValue)));
            return Collections.unmodifiableMap(normalized);
        }

        return value;
    }
}
