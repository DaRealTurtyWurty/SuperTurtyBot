package dev.darealturtywurty.superturtybot.dashboard.service.chat_revival;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.ChatRevivalType;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class ChatRevivalSettingsService {
    private final JDA jda;

    public ChatRevivalSettingsService(JDA jda) {
        this.jda = jda;
    }

    public ChatRevivalSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public ChatRevivalSettingsResponse updateSettings(long guildId, ChatRevivalSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(guild, request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setChatRevivalEnabled(request.isChatRevivalEnabled());
        guildData.setChatRevivalChannel(parseChannelId(request.getChatRevivalChannelId()));
        guildData.setChatRevivalTime(request.getChatRevivalTime());
        guildData.setChatRevivalTypes(toStorage(normalizeTypes(request.getChatRevivalTypes())));
        guildData.setChatRevivalAllowNsfw(request.isChatRevivalAllowNsfw());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static ChatRevivalSettingsResponse toResponse(GuildData guildData) {
        return new ChatRevivalSettingsResponse(
                guildData.isChatRevivalEnabled(),
                guildData.getChatRevivalChannel() == 0L ? null : Long.toString(guildData.getChatRevivalChannel()),
                guildData.getChatRevivalTime(),
                ChatRevivalType.fromStorage(guildData.getChatRevivalTypes()).stream()
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .toList(),
                guildData.isChatRevivalAllowNsfw()
        );
    }

    private static void validateRequest(Guild guild, ChatRevivalSettingsRequest request) {
        if (request.getChatRevivalTime() <= 0) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_chat_revival_time",
                    "Chat revival time must be greater than zero hours.");
        }

        long channelId = parseChannelId(request.getChatRevivalChannelId());
        if (request.isChatRevivalEnabled() && channelId == 0L) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_chat_revival_channel",
                    "A chat revival channel is required while chat revival is enabled.");
        }

        if (channelId != 0L && guild.getTextChannelById(channelId) == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_chat_revival_channel",
                    "The supplied chat revival channel was not a text channel in this guild.");
        }

        if (normalizeTypes(request.getChatRevivalTypes()).isEmpty()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_chat_revival_types",
                    "At least one chat revival type is required.");
        }
    }

    private static long parseChannelId(String channelId) {
        if (channelId == null || channelId.isBlank())
            return 0L;

        try {
            long parsed = Long.parseLong(channelId.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_channel_id",
                    "One of the supplied channel IDs was not a valid Discord snowflake.");
        }
    }

    private static List<ChatRevivalType> normalizeTypes(List<String> inputTypes) {
        if (inputTypes == null || inputTypes.isEmpty())
            return List.of();

        LinkedHashSet<ChatRevivalType> types = new LinkedHashSet<>();
        for (String inputType : inputTypes) {
            if (inputType == null || inputType.isBlank())
                continue;

            try {
                types.add(ChatRevivalType.valueOf(inputType.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_chat_revival_type",
                        "One or more chat revival types were not recognized.");
            }
        }

        return new ArrayList<>(types);
    }

    private static String toStorage(List<ChatRevivalType> types) {
        if (types.isEmpty())
            return "";

        return types.stream()
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }
}
