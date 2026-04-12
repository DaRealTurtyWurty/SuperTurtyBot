package dev.darealturtywurty.superturtybot.dashboard.service.collectables;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollector;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollectorRegistry;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollectablesSettingsService {
    private final JDA jda;

    public CollectablesSettingsService(JDA jda) {
        this.jda = jda;
    }

    public CollectablesSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public CollectablesSettingsResponse updateSettings(long guildId, CollectablesSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        long collectorChannelId = parseChannelId(request.getCollectorChannelId());
        if (collectorChannelId != 0L && guild.getTextChannelById(collectorChannelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_collector_channel",
                    "The supplied collector channel was not a text channel in this guild.");
        }

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setCollectorChannel(collectorChannelId);
        guildData.setCollectingEnabled(request.isCollectingEnabled());

        List<String> enabledTypes = sanitizeTypes(request.getEnabledCollectableTypeIds());
        guildData.setCollectableTypesRestricted(request.isCollectableTypesRestricted());
        guildData.setCollectableTypesList(enabledTypes);

        Map<String, String> disabledByType = new HashMap<>();
        if (request.getDisabledCollectablesByType() != null) {
            for (Map.Entry<String, List<String>> entry : request.getDisabledCollectablesByType().entrySet()) {
                String type = entry.getKey();
                if (!hasCollector(type)) {
                    throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_collectable_type",
                            "One of the supplied collectable types was not recognized.");
                }

                disabledByType.put(type, joinCollectables(validateCollectables(type, entry.getValue())));
            }
        }
        guildData.setDisabledCollectablesByType(disabledByType);

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private CollectablesSettingsResponse toResponse(GuildData guildData) {
        List<DashboardCollectableCollection> collections = new ArrayList<>();
        for (CollectableGameCollector<?> collector : sortedCollectors()) {
            List<DashboardCollectableItem> collectables = collector.getRegistry().getRegistry().values().stream()
                    .sorted(Comparator.comparing(Collectable::getRichName, String.CASE_INSENSITIVE_ORDER))
                    .map(this::toItem)
                    .toList();

            collections.add(new DashboardCollectableCollection(
                    collector.getName(),
                    collector.getDisplayName(),
                    guildData.getDisabledCollectables(collector.getName()),
                    collectables
            ));
        }

        return new CollectablesSettingsResponse(
                guildData.getCollectorChannel() == 0L ? null : Long.toString(guildData.getCollectorChannel()),
                guildData.isCollectingEnabled(),
                guildData.isCollectableTypesRestricted(),
                guildData.getCollectableTypesList(),
                collections
        );
    }

    private DashboardCollectableItem toItem(Collectable collectable) {
        return new DashboardCollectableItem(
                collectable.getName(),
                collectable.getRichName(),
                collectable.getEmoji(),
                collectable.getRarity().getName(),
                collectable.getNote()
        );
    }

    private List<CollectableGameCollector<?>> sortedCollectors() {
        return CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.getRegistry().values().stream()
                .sorted(Comparator.comparing(CollectableGameCollector::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean hasCollector(String type) {
        return CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.getRegistry().containsKey(type);
    }

    private List<String> validateCollectables(String type, List<String> collectables) {
        if (collectables == null) {
            return List.of();
        }

        CollectableGameCollector<?> collector = CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.getRegistry().get(type);
        if (collector == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_collectable_type",
                    "One of the supplied collectable types was not recognized.");
        }

        Map<String, ? extends Collectable> registry = collector.getRegistry().getRegistry();
        List<String> sanitized = new ArrayList<>();
        for (String collectable : collectables) {
            if (collectable == null || collectable.isBlank()) {
                continue;
            }

            String trimmed = collectable.trim();
            if (!registry.containsKey(trimmed)) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_collectable",
                        "One of the supplied collectables was not recognized.");
            }

            if (!sanitized.contains(trimmed)) {
                sanitized.add(trimmed);
            }
        }

        return sanitized;
    }

    private List<String> sanitizeTypes(List<String> types) {
        if (types == null) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>();
        for (String type : types) {
            if (type == null || type.isBlank()) {
                continue;
            }

            String trimmed = type.trim();
            if (!hasCollector(trimmed)) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_collectable_type",
                        "One of the supplied collectable types was not recognized.");
            }

            if (!sanitized.contains(trimmed)) {
                sanitized.add(trimmed);
            }
        }

        return sanitized;
    }

    private long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return 0L;
        }

        try {
            long parsed = Long.parseLong(channelId.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                    "One of the supplied channel IDs was not a valid Discord snowflake.");
        }
    }

    private static String joinCollectables(List<String> collectables) {
        if (collectables == null || collectables.isEmpty()) {
            return "";
        }

        return String.join(";", collectables);
    }
}
