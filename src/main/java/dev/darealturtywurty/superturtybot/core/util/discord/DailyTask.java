package dev.darealturtywurty.superturtybot.core.util.discord;

import lombok.Getter;

public record DailyTask(Runnable task, int hour, int minute) {
    public void run() {
        task.run();
    }
}
