package dev.darealturtywurty.superturtybot.modules.quest;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class QuestManager {
    public static final String ECONOMY_JOB_SHIFT = "job_shift";
    public static final String ECONOMY_JOB_PROMOTION = "job_promotion";
    public static final String ECONOMY_LOAN_REPAID = "loan_repaid";
    public static final String ECONOMY_PROPERTY_PURCHASED = "property_purchased";
    public static final String ECONOMY_PROPERTY_UPGRADED = "property_upgraded";
    public static final String ECONOMY_PROPERTY_RENTED_TO_USER = "property_rented_to_user";
    public static final String ECONOMY_HEIST_COMPLETED = "heist_completed";
    public static final Registry<Quest<?, ?>> QUESTS = new Registry<>();
    public static final QuestManager INSTANCE = new QuestManager();

    private QuestManager() {
        QuestRegistry.registerQuests();
    }

    void awardXP(QuestPlayer player, int amount) {
        Guild guild = TurtyBot.getJDA().getGuildById(player.getGuild());
        if (guild == null)
            throw new IllegalStateException("Quest guild no longer exists.");

        User user = TurtyBot.getJDA().getUserById(player.getUser());
        if (user == null)
            throw new IllegalStateException("Quest user is not available.");

        if (LevellingManager.INSTANCE.addXP(guild, user, amount) < 0)
            throw new IllegalStateException("Quest user is no longer a member of the guild.");
    }

    public static long currentWeekStart() {
        return Instant.now().atZone(ZoneOffset.UTC)
                .toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public QuestPlayer getOrCreateWeeklyQuests(Guild guild, Member member) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            throw new IllegalStateException("Quests are disabled in this guild.");

        if (QUESTS.size() == 0)
            throw new IllegalStateException("No quests are registered.");

        long weekStart = currentWeekStart();
        QuestPlayer previous = Database.getDatabase().questPlayers.find(Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", member.getIdLong()),
                        Filters.lt("weekStart", weekStart)
                ))
                .sort(Sorts.descending("weekStart"))
                .first();
        if (previous != null)
            rewardCompletedQuests(guild, member, previous);

        Bson filter = playerFilter(guild.getIdLong(), member.getIdLong(), weekStart);
        QuestPlayer existing = Database.getDatabase().questPlayers.find(filter).first();
        if (existing != null) {
            rewardCompletedQuests(guild, member, existing);
            return existing;
        }

        List<String> assignedQuestIds = generateWeeklyQuestsForPlayer(guild, member, Set.of())
                .stream()
                .map(Quest::getId)
                .toList();

        Database.getDatabase().questPlayers.updateOne(filter, Updates.combine(
                Updates.setOnInsert("guild", guild.getIdLong()),
                Updates.setOnInsert("user", member.getIdLong()),
                Updates.setOnInsert("weekStart", weekStart),
                Updates.setOnInsert("assignedQuestIds", assignedQuestIds),
                Updates.setOnInsert("rewardedQuestIds", List.of()),
                Updates.setOnInsert("rerollUsed", false)
        ), new UpdateOptions().upsert(true));

        QuestPlayer player = Database.getDatabase().questPlayers.find(filter).first();
        rewardCompletedQuests(guild, member, player);
        return player;
    }

    public void saveProgress(QuestPlayer player) {
        Bson filter = playerFilter(player.getGuild(), player.getUser(), player.getWeekStart());
        if (Database.getDatabase().questPlayers.updateOne(filter, Updates.combine(
                        Updates.set("assignedQuestIds", player.getAssignedQuestIds()),
                        Updates.set("rewardedQuestIds", player.getRewardedQuestIds()),
                        Updates.set("rerollUsed", player.isRerollUsed())
                ))
                .getMatchedCount() == 0)
            throw new IllegalStateException("Weekly quest assignment no longer exists.");
    }

    public List<Quest<?, ?>> rerollQuests(Guild guild, Member member, QuestPlayer player) {
        if (player.isRerollUsed())
            throw new IllegalStateException("You have already used your reroll this week.");

        Set<String> previousQuestIds = new HashSet<>(player.getAssignedQuestIds());
        List<Quest<?, ?>> replacements = generateWeeklyQuestsForPlayer(guild, member, previousQuestIds);
        List<String> updatedQuestIds = replacements.stream().map(Quest::getId).toList();

        Bson filter = Filters.and(
                playerFilter(player.getGuild(), player.getUser(), player.getWeekStart()),
                Filters.ne("rerollUsed", true),
                Filters.eq("assignedQuestIds", player.getAssignedQuestIds()),
                Filters.nin("rewardedQuestIds", previousQuestIds)
        );
        if (Database.getDatabase().questPlayers.updateOne(filter, Updates.combine(
                Updates.set("assignedQuestIds", updatedQuestIds),
                Updates.set("rerollUsed", true)
        )).getModifiedCount() == 0)
            throw new IllegalStateException("Your weekly quests changed before the reroll could be completed.");

        player.setAssignedQuestIds(updatedQuestIds);
        player.setRerollUsed(true);
        rewardCompletedQuests(guild, member, player);
        return replacements;
    }

    private boolean rewardQuest(QuestPlayer player, Quest<?, ?> quest) {
        Bson filter = Filters.and(
                playerFilter(player.getGuild(), player.getUser(), player.getWeekStart()),
                Filters.eq("assignedQuestIds", quest.getId()),
                Filters.ne("rewardedQuestIds", quest.getId())
        );
        if (Database.getDatabase().questPlayers.updateOne(
                filter,
                Updates.addToSet("rewardedQuestIds", quest.getId())
        ).getModifiedCount() == 0)
            return false;

        try {
            quest.getReward().giveReward(quest, player);
        } catch (RuntimeException exception) {
            Database.getDatabase().questPlayers.updateOne(
                    playerFilter(player.getGuild(), player.getUser(), player.getWeekStart()),
                    Updates.pull("rewardedQuestIds", quest.getId())
            );
            throw exception;
        }

        if (player.getRewardedQuestIds() == null)
            player.setRewardedQuestIds(new ArrayList<>());

        player.getRewardedQuestIds().add(quest.getId());
        return true;
    }

    private void rewardCompletedQuests(QuestActivity activity) {
        Guild guild = TurtyBot.getJDA().getGuildById(activity.getGuild());
        if (guild == null)
            return;

        Member member = guild.getMemberById(activity.getUser());
        if (member == null)
            return;

        QuestPlayer player = Database.getDatabase().questPlayers.find(
                playerFilter(activity.getGuild(), activity.getUser(), activity.getWeekStart())
        ).first();
        if (player == null)
            return;

        rewardCompletedQuests(guild, member, player);
    }

    private void rewardCompletedQuests(Guild guild, Member member, QuestPlayer player) {
        List<QuestEvent> events = getQuestEvents(guild, member, player.getWeekStart());
        List<String> rewardedQuests = new ArrayList<>();
        for (String questId : player.getAssignedQuestIds()) {
            if (player.hasBeenRewarded(questId))
                continue;

            Quest<?, ?> quest = QUESTS.get(questId);
            if (quest == null || !quest.evaluate(events).complete())
                continue;

            if (rewardQuest(player, quest))
                rewardedQuests.add("**" + quest.getDisplayName() + "** ("
                        + quest.getReward().getDescription() + ")");
        }

        if (!rewardedQuests.isEmpty())
            sendRewardMessage(guild, member, rewardedQuests);
    }

    private void sendRewardMessage(Guild guild, Member member, List<String> rewardedQuests) {
        String questNames = String.join(", ", rewardedQuests);
        String message = member.getAsMention() + " completed " + questNames
                + " and received the quest rewards!";

        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        TextChannel levelUpChannel = guildData.isHasLevelUpChannel()
                ? guild.getTextChannelById(guildData.getLevelUpMessageChannel())
                : null;
        if (levelUpChannel != null) {
            levelUpChannel.sendMessage(message).queue();
            return;
        }

        member.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessage("In **" + guild.getName() + "**, you completed "
                        + questNames + " and received the quest rewards!"))
                .queue(null, exception -> Constants.LOGGER.debug(
                        "Unable to notify user {} about completed quests in guild {}.",
                        member.getId(),
                        guild.getId(),
                        exception
                ));
    }

    public void recordTriviaAnswer(Guild guild, User user, String sourceId, boolean correct) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "trivia_answered",
                sourceId,
                System.currentTimeMillis(),
                new Document("correct", correct)
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordCompletedMultiplayerMatch(
            Guild guild,
            String gameType,
            long matchId,
            long player1Id,
            long player2Id,
            long winnerId
    ) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        String sourceId = gameType + ":" + matchId;

        recordCompletedMultiplayerMatch(
                guild.getIdLong(),
                player1Id,
                gameType,
                sourceId,
                player2Id,
                player1Id == winnerId
        );

        recordCompletedMultiplayerMatch(
                guild.getIdLong(),
                player2Id,
                gameType,
                sourceId,
                player1Id,
                player2Id == winnerId
        );
    }

    private void recordCompletedMultiplayerMatch(
            long guildId,
            long userId,
            String gameType,
            String sourceId,
            long opponentId,
            boolean won
    ) {
        var activity = new QuestActivity(
                guildId,
                userId,
                currentWeekStart(),
                "multiplayer_match_completed",
                sourceId,
                System.currentTimeMillis(),
                new Document("gameType", gameType)
                        .append("opponentId", Long.toString(opponentId))
                        .append("won", won)
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordMinigameCompletion(
            long guildId,
            long userId,
            String gameType,
            long sourceId
    ) {
        if (guildId == 0L || !GuildData.getOrCreateGuildData(guildId).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guildId,
                userId,
                currentWeekStart(),
                "minigame_completed",
                gameType + ":" + sourceId,
                System.currentTimeMillis(),
                new Document("gameType", gameType)
        );
        insertActivity(activity);
    }

    public void record2048Progress(
            Guild guild,
            User user,
            long gameId,
            int highestTile,
            boolean finished
    ) {
        if (guild == null || !GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        String sourceId = gameId + (finished ? ":finished" : ":tile:" + highestTile);
        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "2048_progress",
                sourceId,
                System.currentTimeMillis(),
                new Document("highestTile", highestTile).append("finished", finished)
        );
        insertActivity(activity);
    }

    public void recordShowcaseThresholdReached(long guildId, long userId, long messageId) {
        recordCommunityActivity(guildId, userId, "showcase_threshold", Long.toString(messageId));
    }

    public void recordSuggestionAccepted(long guildId, long userId, long messageId) {
        recordCommunityActivity(guildId, userId, "suggestion_accepted", Long.toString(messageId));
    }

    public void recordConversationPromptUsed(Guild guild, User user, long interactionId) {
        if (guild == null)
            return;

        recordCommunityActivity(
                guild.getIdLong(),
                user.getIdLong(),
                "conversation_prompt",
                Long.toString(interactionId)
        );
    }

    private void recordCommunityActivity(long guildId, long userId, String activityType, String sourceId) {
        if (guildId == 0L || !GuildData.getOrCreateGuildData(guildId).isQuestEnabled())
            return;

        LocalDate activityDate = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        var activity = new QuestActivity(
                guildId,
                userId,
                currentWeekStart(),
                "community_activity_completed",
                activityType + ":" + sourceId,
                System.currentTimeMillis(),
                new Document("activityType", activityType).append("date", activityDate.toString())
        );
        insertActivity(activity);
    }

    public void recordPollCreated(Guild guild, User creator, long pollMessageId) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        String pollId = Long.toString(pollMessageId);
        var activity = new QuestActivity(
                guild.getIdLong(),
                creator.getIdLong(),
                currentWeekStart(),
                "poll_activity",
                pollId + ":created",
                System.currentTimeMillis(),
                new Document("pollId", pollId).append("voterId", "")
        );
        insertActivity(activity);
    }

    public void recordPollVote(Guild guild, long pollMessageId, User voter) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        String pollId = Long.toString(pollMessageId);
        QuestActivity pollCreation = Database.getDatabase().questActivities.find(Filters.and(
                Filters.eq("guild", guild.getIdLong()),
                Filters.eq("weekStart", currentWeekStart()),
                Filters.eq("type", "poll_activity"),
                Filters.eq("sourceId", pollId + ":created")
        )).first();
        if (pollCreation == null || pollCreation.getUser() == voter.getIdLong())
            return;

        String voterId = voter.getId();
        var activity = new QuestActivity(
                guild.getIdLong(),
                pollCreation.getUser(),
                currentWeekStart(),
                "poll_activity",
                pollId + ":voter:" + voterId,
                System.currentTimeMillis(),
                new Document("pollId", pollId).append("voterId", voterId)
        );
        insertActivity(activity);
    }

    public void recordReminderCreated(long guildId, long userId, String reminderId) {
        recordReminderActivity(guildId, userId, reminderId, "reminder_created");
    }

    public void recordReminderFired(long guildId, long userId, String reminderId) {
        recordReminderActivity(guildId, userId, reminderId, "reminder_fired");
    }

    private void recordReminderActivity(long guildId, long userId, String reminderId, String activityType) {
        if (guildId == 0L || !GuildData.getOrCreateGuildData(guildId).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guildId,
                userId,
                currentWeekStart(),
                activityType,
                reminderId,
                System.currentTimeMillis(),
                new Document()
        );
        insertActivity(activity);
    }

    private void insertActivity(QuestActivity activity) {
        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordWordleCompletion(long guildId, long userId, LocalDate completedOn) {
        if (guildId == 0L || !GuildData.getOrCreateGuildData(guildId).isQuestEnabled())
            return;

        long weekStart = completedOn
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        String completedDate = completedOn.toString();
        var activity = new QuestActivity(
                guildId,
                userId,
                weekStart,
                "wordle_completed",
                completedDate,
                System.currentTimeMillis(),
                new Document("date", completedDate)
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordCollectableEarned(
            Guild guild,
            User user,
            long questionMessageId,
            String collectionType,
            String collectableId,
            int rarityOrdinal,
            long questionAppearedAt,
            long earnedAt
    ) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "collectable_earned",
                Long.toString(questionMessageId),
                earnedAt,
                new Document()
                        .append("collectionType", collectionType)
                        .append("collectableId", collectableId)
                        .append("rarityOrdinal", rarityOrdinal)
                        .append("responseTimeMillis", Math.max(0, earnedAt - questionAppearedAt))
        );
        insertActivity(activity);
    }

    public void recordMarketplaceItemListed(Guild guild, User seller, String listingId) {
        recordMarketplaceActivity(guild, seller.getIdLong(), "marketplace_item_listed", listingId);
    }

    public void recordMarketplaceItemSold(Guild guild, long sellerId, String listingId) {
        recordMarketplaceActivity(guild, sellerId, "marketplace_item_sold", listingId);
    }

    public void recordEconomyAction(long guildId, long userId, String actionType, String sourceId) {
        if (!GuildData.getOrCreateGuildData(guildId).isQuestEnabled())
            return;

        LocalDate date = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        var activity = new QuestActivity(
                guildId,
                userId,
                currentWeekStart(),
                "economy_action",
                sourceId,
                System.currentTimeMillis(),
                new Document("actionType", actionType).append("date", date.toString())
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordCommandUsed(Guild guild, User user, String commandType, String category) {
        if (guild == null)
            return;

        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        LocalDate date = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "command_used",
                commandType + ":" + date,
                System.currentTimeMillis(),
                new Document()
                        .append("commandType", commandType)
                        .append("category", category)
                        .append("date", date.toString())
        );
        insertActivity(activity);
    }

    private void recordMarketplaceActivity(Guild guild, long userId, String type, String listingId) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guild.getIdLong(),
                userId,
                currentWeekStart(),
                type,
                listingId,
                System.currentTimeMillis(),
                new Document()
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordValidCount(Guild guild, User user, long messageId, Instant countedAt) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        LocalDate countDate = countedAt.atZone(ZoneOffset.UTC).toLocalDate();
        long weekStart = countDate
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                weekStart,
                "valid_count",
                Long.toString(messageId),
                countedAt.toEpochMilli(),
                new Document("date", countDate.toString())
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordGeographyAnswer(
            Guild guild,
            User user,
            String gameType,
            long sourceId,
            boolean correct
    ) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "geography_answered",
                Long.toString(sourceId),
                System.currentTimeMillis(),
                new Document()
                        .append("gameType", gameType)
                        .append("correct", correct)
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    public void recordGeographyGameCompleted(
            Guild guild,
            User user,
            String gameType,
            long sourceId,
            int gameSize,
            int attempts
    ) {
        if (!GuildData.getOrCreateGuildData(guild).isQuestEnabled())
            return;

        var activity = new QuestActivity(
                guild.getIdLong(),
                user.getIdLong(),
                currentWeekStart(),
                "geography_game_completed",
                Long.toString(sourceId),
                System.currentTimeMillis(),
                new Document()
                        .append("gameType", gameType)
                        .append("gameSize", gameSize)
                        .append("attempts", attempts)
        );

        Database.getDatabase().questActivities.updateOne(
                Filters.and(
                        Filters.eq("guild", activity.getGuild()),
                        Filters.eq("user", activity.getUser()),
                        Filters.eq("weekStart", activity.getWeekStart()),
                        Filters.eq("type", activity.getType()),
                        Filters.eq("sourceId", activity.getSourceId())
                ),
                Updates.combine(
                        Updates.setOnInsert("guild", activity.getGuild()),
                        Updates.setOnInsert("user", activity.getUser()),
                        Updates.setOnInsert("weekStart", activity.getWeekStart()),
                        Updates.setOnInsert("type", activity.getType()),
                        Updates.setOnInsert("sourceId", activity.getSourceId()),
                        Updates.setOnInsert("occurredAt", activity.getOccurredAt()),
                        Updates.setOnInsert("details", activity.getDetails())
                ),
                new UpdateOptions().upsert(true)
        );
        rewardCompletedQuests(activity);
    }

    private static Bson playerFilter(long guild, long user, long weekStart) {
        return Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("user", user),
                Filters.eq("weekStart", weekStart)
        );
    }

    private List<Quest<?, ?>> generateWeeklyQuestsForPlayer(Guild guild, Member member,
                                                            Set<String> excludedQuestIds) {
        List<Quest<?, ?>> availableQuests = QUESTS.values().stream()
                .filter(quest -> quest.isAvailableFor(guild, member))
                .filter(quest -> !excludedQuestIds.contains(quest.getId()))
                .collect(Collectors.toList());

        Collections.shuffle(availableQuests);
        List<Quest<?, ?>> selectedQuests = new ArrayList<>();
        if (!selectCompatibleQuests(availableQuests, selectedQuests, 0, 3))
            throw new IllegalStateException("Unable to generate three compatible weekly quests.");

        return List.copyOf(selectedQuests);
    }

    private boolean selectCompatibleQuests(List<Quest<?, ?>> availableQuests, List<Quest<?, ?>> selectedQuests,
                                           int index, int target) {
        if (selectedQuests.size() == target)
            return true;

        for (int current = index; current < availableQuests.size(); current++) {
            Quest<?, ?> candidate = availableQuests.get(current);
            if (selectedQuests.stream().anyMatch(candidate::conflictsWith))
                continue;

            selectedQuests.add(candidate);
            if (selectCompatibleQuests(availableQuests, selectedQuests, current + 1, target))
                return true;

            selectedQuests.removeLast();
        }

        return false;
    }

    public List<QuestActivity> getQuestActivityDuringWeek(Guild guild, Member member, long weekStart) {
        return Database.getDatabase().questActivities.find(
                Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", member.getIdLong()),
                        Filters.eq("weekStart", weekStart)
                )
        ).into(new ArrayList<>());
    }

    private QuestEvent toEvent(QuestActivity activity) {
        return switch (activity.getType()) {
            case "trivia_answered" -> new QuestEvent.TriviaAnswered(
                    activity.getSourceId(),
                    activity.getDetails().getBoolean("correct", false)
            );
            case "multiplayer_match_completed" -> new QuestEvent.MultiplayerMatchCompleted(
                    activity.getSourceId(),
                    activity.getDetails().getString("gameType"),
                    activity.getDetails().getString("opponentId"),
                    activity.getDetails().getBoolean("won", false)
            );
            case "minigame_completed" -> new QuestEvent.MinigameCompleted(
                    activity.getSourceId(),
                    activity.getDetails().getString("gameType")
            );
            case "2048_progress" -> new QuestEvent.Game2048Progress(
                    activity.getSourceId(),
                    activity.getDetails().getInteger("highestTile", 0),
                    activity.getDetails().getBoolean("finished", false)
            );
            case "wordle_completed" -> new QuestEvent.WordleCompleted(
                    LocalDate.parse(activity.getDetails().getString("date"))
            );
            case "collectable_earned" -> new QuestEvent.CollectableEarned(
                    activity.getSourceId(),
                    activity.getDetails().getString("collectionType"),
                    activity.getDetails().getInteger("rarityOrdinal", -1),
                    activity.getDetails().getLong("responseTimeMillis") == null
                            ? Long.MAX_VALUE
                            : activity.getDetails().getLong("responseTimeMillis"),
                    Instant.ofEpochMilli(activity.getOccurredAt()).atZone(ZoneOffset.UTC).toLocalDate()
            );
            case "marketplace_item_listed" -> new QuestEvent.MarketplaceItemListed(activity.getSourceId());
            case "marketplace_item_sold" -> new QuestEvent.MarketplaceItemSold(activity.getSourceId());
            case "valid_count" -> new QuestEvent.ValidCount(
                    activity.getSourceId(),
                    activity.getDetails().getString("date") == null
                            ? Instant.ofEpochMilli(activity.getOccurredAt()).atZone(ZoneOffset.UTC).toLocalDate()
                            : LocalDate.parse(activity.getDetails().getString("date"))
            );
            case "geography_answered" -> new QuestEvent.GeographyAnswered(
                    activity.getSourceId(),
                    activity.getDetails().getString("gameType"),
                    activity.getDetails().getBoolean("correct", false)
            );
            case "geography_game_completed" -> new QuestEvent.GeographyGameCompleted(
                    activity.getSourceId(),
                    activity.getDetails().getString("gameType"),
                    activity.getDetails().getInteger("gameSize", 0),
                    activity.getDetails().getInteger("attempts", 0)
            );
            case "community_activity_completed" -> new QuestEvent.CommunityActivityCompleted(
                    activity.getSourceId(),
                    activity.getDetails().getString("activityType"),
                    LocalDate.parse(activity.getDetails().getString("date"))
            );
            case "poll_activity" -> new QuestEvent.PollActivity(
                    activity.getDetails().getString("pollId"),
                    activity.getDetails().getString("voterId")
            );
            case "reminder_created" -> new QuestEvent.ReminderCreated(activity.getSourceId());
            case "reminder_fired" -> new QuestEvent.ReminderFired(activity.getSourceId());
            case "economy_action" -> new QuestEvent.EconomyAction(
                    activity.getSourceId(),
                    activity.getDetails().getString("actionType"),
                    activity.getDetails().getString("date") == null
                            ? Instant.ofEpochMilli(activity.getOccurredAt()).atZone(ZoneOffset.UTC).toLocalDate()
                            : LocalDate.parse(activity.getDetails().getString("date"))
            );
            case "command_used" -> new QuestEvent.CommandUsed(
                    activity.getDetails().getString("commandType"),
                    activity.getDetails().getString("category"),
                    activity.getDetails().getString("date") == null
                            ? Instant.ofEpochMilli(activity.getOccurredAt()).atZone(ZoneOffset.UTC).toLocalDate()
                            : LocalDate.parse(activity.getDetails().getString("date"))
            );
            default -> throw new IllegalArgumentException("Unknown quest activity type: " + activity.getType());
        };
    }

    public List<QuestEvent> getQuestEvents(Guild guild, Member member, long weekStart) {
        List<QuestActivity> activities = Database.getDatabase().questActivities
                .find(Filters.and(
                        Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", member.getIdLong()),
                        Filters.eq("weekStart", weekStart)
                ))
                .into(new ArrayList<>());

        return activities.stream()
                .sorted(Comparator
                        .comparingLong(QuestActivity::getOccurredAt)
                        .thenComparing(QuestActivity::getSourceId))
                .map(this::toEvent)
                .toList();
    }
}
