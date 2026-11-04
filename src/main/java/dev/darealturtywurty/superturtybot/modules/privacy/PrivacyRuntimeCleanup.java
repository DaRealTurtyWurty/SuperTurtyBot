package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.modules.AIMessageResponder;
import dev.darealturtywurty.superturtybot.modules.LoggingManager;
import dev.darealturtywurty.superturtybot.modules.ReminderManager;
import dev.darealturtywurty.superturtybot.modules.WordleReminderManager;

import java.util.List;
import java.util.function.LongConsumer;

public final class PrivacyRuntimeCleanup {
    private PrivacyRuntimeCleanup() {
    }

    public static void pauseUser(long userId) {
        run(userId, List.of(ExternalSharingConfirmation.INSTANCE::forgetUser,
            AIMessageResponder.INSTANCE::forgetUser, LoggingManager.INSTANCE::forgetUser,
            ReminderManager::pauseReminders, WordleReminderManager::pauseReminders,
            id -> Database.getDatabase().mongoDatabase
                .getCollection("dashboardSessions").deleteMany(Filters.in("user.id", id, Long.toString(id)))));
    }

    public static void deleteUser(long userId) {
        run(userId, List.of(ExternalSharingConfirmation.INSTANCE::forgetUser,
            AIMessageResponder.INSTANCE::forgetUser, LoggingManager.INSTANCE::forgetUser,
            ReminderManager::clearReminders, WordleReminderManager::pauseReminders,
            RetentionActivity::forgetUser));
    }

    public static void resumeUser(long userId) {
        run(userId, List.of(ReminderManager::resumeReminders, WordleReminderManager::resumeReminders));
    }

    private static void run(long userId, List<LongConsumer> steps) {
        boolean failed = false;
        for (LongConsumer step : steps) {
            try {
                step.accept(userId);
            } catch (RuntimeException exception) {
                failed = true;
            }
        }
        if (failed)
            throw new IllegalStateException("A runtime privacy cleanup step failed");
    }
}
