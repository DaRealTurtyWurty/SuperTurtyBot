package dev.darealturtywurty.superturtybot.modules.quest;

import java.time.LocalDate;

public sealed interface QuestEvent {
    record TriviaAnswered(String sourceId, boolean correct) implements QuestEvent {
    }

    record MultiplayerMatchCompleted(String sourceId, String gameType, String opponentId, boolean won)
        implements
            QuestEvent {
    }

    record MinigameCompleted(String sourceId, String gameType) implements QuestEvent {
    }

    record Game2048Progress(String sourceId, int highestTile, boolean finished) implements QuestEvent {
    }

    record WordleCompleted(LocalDate date) implements QuestEvent {
    }

    record CollectableEarned(
        String questionId,
        String collectionType,
        int rarityOrdinal,
        long responseTimeMillis,
        LocalDate date
    ) implements QuestEvent {
    }

    record MarketplaceItemListed(String listingId) implements QuestEvent {
    }

    record MarketplaceItemSold(String listingId) implements QuestEvent {
    }

    record ValidCount(String messageId, LocalDate date) implements QuestEvent {
    }

    record GeographyAnswered(String sourceId, String gameType, boolean correct) implements QuestEvent {
    }

    record GeographyGameCompleted(String sourceId, String gameType, int gameSize, int attempts)
        implements
            QuestEvent {
    }

    record CommunityActivityCompleted(String sourceId, String activityType, LocalDate date) implements QuestEvent {
    }

    record PollActivity(String pollId, String voterId) implements QuestEvent {
    }

    record ReminderCreated(String reminderId) implements QuestEvent {
    }

    record ReminderFired(String reminderId) implements QuestEvent {
    }

    record EconomyAction(String sourceId, String actionType, LocalDate date) implements QuestEvent {
    }

    record CommandUsed(String commandType, String category, LocalDate date) implements QuestEvent {
    }
}
