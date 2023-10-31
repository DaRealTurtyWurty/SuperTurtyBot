package dev.darealturtywurty.superturtybot.core.util.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyTaskScheduler {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final List<DailyTask> TASKS = new ArrayList<>();

    public static void addTask(DailyTask task) {
        TASKS.add(task);
    }

    public static void removeTask(DailyTask task) {
        TASKS.remove(task);
    }

    public static void start() {
        SCHEDULER.scheduleAtFixedRate(
                DailyTaskScheduler::runTasks,
                getTimeUntilNextMinute(),
                1,
                TimeUnit.MINUTES);
    }

    private static void runTasks() {
        long now = System.currentTimeMillis();
        long currentHour = now % 86400000 / 3600000;
        long currentMinute = now % 3600000 / 60000;

        for (DailyTask task : TASKS) {
            if (task.hour() == currentHour && task.minute() == currentMinute) {
                task.run();
            }
        }
    }

    private static long getTimeUntilNextMinute() {
        long now = System.currentTimeMillis();
        return 60000 - (now % 60000);
    }
}
