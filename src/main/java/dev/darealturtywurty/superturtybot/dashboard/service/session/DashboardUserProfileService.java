package dev.darealturtywurty.superturtybot.dashboard.service.session;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.WordleStreakData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Birthday;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.database.pojos.collections.WordleProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DashboardUserProfileService {
    public DashboardUserProfileResponse getUserProfile(long userId) {
        Birthday birthday = Database.getDatabase().birthdays.find(Filters.eq("user", userId)).first();
        WordleProfile wordleProfile = Database.getDatabase().wordleProfiles.find(Filters.eq("user", userId)).first();
        UserCollectables userCollectables = Database.getDatabase().userCollectables.find(Filters.eq("user", userId)).first();
        List<Economy> economies = Database.getDatabase().economy.find(Filters.eq("user", userId)).into(new ArrayList<>());

        return new DashboardUserProfileResponse(
                Long.toString(userId),
                birthday == null ? null : new DashboardUserProfileResponse.Birthday(
                        birthday.getDay(),
                        birthday.getMonth(),
                        birthday.getYear()
                ),
                wordleProfile == null ? null : new DashboardUserProfileResponse.Wordle(
                        wordleProfile.getStreaks().stream()
                                .sorted(Comparator
                                        .comparingLong((WordleStreakData streak) -> streak.getGuild() == 0L ? 0L : 1L)
                                        .thenComparing(WordleStreakData::getBestStreak, Comparator.reverseOrder()))
                                .map(streak -> new DashboardUserProfileResponse.Streak(
                                        Long.toString(streak.getGuild()),
                                        streak.getStreak(),
                                        streak.getBestStreak(),
                                        streak.isHasPlayedToday()
                                ))
                                .toList()
                ),
                userCollectables == null ? null : new DashboardUserProfileResponse.Collectables(
                        userCollectables.getCollectables().stream()
                                .map(collectables -> new DashboardUserProfileResponse.Group(
                                        collectables.getType(),
                                        List.copyOf(collectables.getCollectables())
                                ))
                                .toList()
                ),
                economies.stream()
                        .map(economy -> new DashboardUserProfileResponse.EconomyEntry(
                                Long.toString(economy.getGuild()),
                                findCurrency(economy.getGuild()),
                                economy.getBank().toString(),
                                economy.getWallet().toString(),
                                economy.getCrimeLevel(),
                                economy.getHeistLevel(),
                                economy.getJob() == null ? null : economy.getJob().name(),
                                economy.getJobLevel()
                        ))
                        .sorted(Comparator.comparing(DashboardUserProfileResponse.EconomyEntry::guildId))
                        .toList()
        );
    }

    private static String findCurrency(long guildId) {
        GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        if (guildData == null || guildData.getEconomyCurrency() == null || guildData.getEconomyCurrency().isBlank())
            return "$";

        return guildData.getEconomyCurrency();
    }
}
