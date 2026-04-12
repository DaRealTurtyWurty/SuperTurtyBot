package dev.darealturtywurty.superturtybot.dashboard.service.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.math.BigInteger;

public final class EconomySettingsService {
    private final JDA jda;

    public EconomySettingsService(JDA jda) {
        this.jda = jda;
    }

    public EconomySettingsResponse getSettings(long guildId) {
        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        return toResponse(guildData);
    }

    public EconomySettingsResponse updateSettings(long guildId, EconomySettingsRequest request) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        validateRequest(request);

        GuildData guildData = GuildData.getOrCreateGuildData(guildId);
        guildData.setEconomyCurrency(request.getEconomyCurrency().trim());
        guildData.setEconomyEnabled(request.isEconomyEnabled());
        guildData.setDonateEnabled(request.isDonateEnabled());
        guildData.setDefaultEconomyBalance(new BigInteger(request.getDefaultEconomyBalance().trim()));
        guildData.setIncomeTax(request.getIncomeTax());

        Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guildId), guildData);
        return toResponse(guildData);
    }

    private static EconomySettingsResponse toResponse(GuildData guildData) {
        return new EconomySettingsResponse(
                guildData.getEconomyCurrency(),
                guildData.isEconomyEnabled(),
                guildData.isDonateEnabled(),
                guildData.getDefaultEconomyBalance().toString(),
                guildData.getIncomeTax()
        );
    }

    private static void validateRequest(EconomySettingsRequest request) {
        if (request.getEconomyCurrency() == null || request.getEconomyCurrency().trim().isEmpty()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_economy_currency",
                    "Economy currency is required.");
        }

        if (request.getEconomyCurrency().trim().length() > 16) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_economy_currency",
                    "Economy currency must be 16 characters or fewer.");
        }

        try {
            if (new BigInteger(request.getDefaultEconomyBalance().trim()).signum() <= 0) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_default_economy_balance",
                        "Default economy balance must be greater than zero.");
            }
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_default_economy_balance",
                    "Default economy balance must be a valid whole number.");
        }

        if (request.getIncomeTax() < 0F || request.getIncomeTax() > 1F) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_income_tax",
                    "Income tax must be between 0 and 1.");
        }
    }
}
