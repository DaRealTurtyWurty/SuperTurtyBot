package dev.darealturtywurty.superturtybot.dashboard.service.nsfw;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

public final class NsfwSettingsService {
    private final JDA jda;

    public NsfwSettingsService(JDA jda) {
        this.jda = jda;
    }

    public NsfwSettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public NsfwSettingsResponse updateSettings(long guildId, NsfwSettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                "TurtyBot is not currently connected to that guild.");

        if (request == null)
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_nsfw_settings",
                "The NSFW settings payload was missing.");

        GuildData.getOrCreateGuildData(guildId);
        Database.getDatabase().guildData.updateOne(Filters.eq("guild", guildId),
            Updates.set("artistNsfwFilterEnabled", request.isArtistNsfwFilterEnabled()));
        return new NsfwSettingsResponse(request.isArtistNsfwFilterEnabled());
    }

    private static NsfwSettingsResponse toResponse(GuildData guildData) {
        return new NsfwSettingsResponse(guildData.isArtistNsfwFilterEnabled());
    }
}
