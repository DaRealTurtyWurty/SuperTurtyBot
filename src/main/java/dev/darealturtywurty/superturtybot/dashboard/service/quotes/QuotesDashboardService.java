package dev.darealturtywurty.superturtybot.dashboard.service.quotes;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Quote;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.List;

public final class QuotesDashboardService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 25;

    private final JDA jda;

    public QuotesDashboardService(JDA jda) {
        this.jda = jda;
    }

    public DashboardQuotesPageResponse getQuotes(long guildId, int page, int pageSize) {
        Guild guild = requireGuild(guildId);
        long totalCount = Database.getDatabase().quotes.countDocuments(Filters.eq("guild", guildId));
        int safePageSize = clampPageSize(pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) safePageSize));
        int safePage = clampPage(page, totalPages);
        long offset = (long) (safePage - 1) * safePageSize;

        return new DashboardQuotesPageResponse(
                safePage,
                safePageSize,
                totalCount,
                totalPages,
                listQuotes(guild, offset, safePageSize, totalCount)
        );
    }

    public DashboardQuotesPageResponse deleteQuote(long guildId, int quoteNumber, int page, int pageSize) {
        Guild guild = requireGuild(guildId);
        List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guildId))
                .sort(Sorts.ascending("timestamp"))
                .into(new ArrayList<>());

        if (quoteNumber < 1 || quoteNumber > quotes.size()) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "quote_not_found",
                    "That quote could not be found.");
        }

        Quote quote = quotes.get(quoteNumber - 1);
        DeleteResult result = Database.getDatabase().quotes.deleteOne(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("timestamp", quote.getTimestamp()),
                Filters.eq("addedBy", quote.getAddedBy()),
                Filters.eq("text", quote.getText()),
                Filters.eq("user", quote.getUser()),
                Filters.eq("channel", quote.getChannel()),
                Filters.eq("message", quote.getMessage())
        ));

        if (result.getDeletedCount() == 0) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "quote_not_found",
                    "That quote could not be found.");
        }

        return getQuotes(guildId, page, pageSize);
    }

    private List<DashboardQuoteRecord> listQuotes(Guild guild, long offset, int pageSize, long totalCount) {
        List<Quote> quotes = Database.getDatabase().quotes.find(Filters.eq("guild", guild.getIdLong()))
                .sort(Sorts.ascending("timestamp"))
                .skip((int) offset)
                .limit(pageSize)
                .into(new ArrayList<>());

        List<DashboardQuoteRecord> records = new ArrayList<>(quotes.size());
        for (int index = 0; index < quotes.size(); index++) {
            records.add(toRecord(guild, quotes.get(index), Math.toIntExact(offset + index + 1)));
        }

        return records;
    }

    private DashboardQuoteRecord toRecord(Guild guild, Quote quote, int number) {
        ResolvedUser saidBy = resolveUser(quote.getUser());
        ResolvedUser addedBy = resolveUser(quote.getAddedBy());

        String channelId = quote.getChannel() > 0L ? Long.toString(quote.getChannel()) : null;
        String messageId = quote.getMessage() > 0L ? Long.toString(quote.getMessage()) : null;
        String messageUrl = channelId == null || messageId == null
                ? null
                : "https://discord.com/channels/%d/%s/%s".formatted(guild.getIdLong(), channelId, messageId);

        return new DashboardQuoteRecord(
                number,
                quote.getText(),
                Long.toString(quote.getUser()),
                saidBy.displayName(),
                saidBy.avatarUrl(),
                Long.toString(quote.getAddedBy()),
                addedBy.displayName(),
                addedBy.avatarUrl(),
                channelId,
                messageId,
                messageUrl,
                quote.getTimestamp()
        );
    }

    private ResolvedUser resolveUser(long userId) {
        User user = this.jda.getUserById(userId);
        if (user == null) {
            return new ResolvedUser("Unknown User", null);
        }

        return new ResolvedUser(user.getEffectiveName(), user.getEffectiveAvatarUrl());
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private static int clampPageSize(int pageSize) {
        return Math.clamp(pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize, 1, MAX_PAGE_SIZE);
    }

    private static int clampPage(int page, int totalPages) {
        int safePage = page <= 0 ? 1 : page;
        return Math.clamp(safePage, 1, Math.max(1, totalPages));
    }

    private record ResolvedUser(String displayName, String avatarUrl) {
    }
}
