package dev.darealturtywurty.superturtybot.modules.quest;

public record QuestStatus(int progress, int target, boolean complete, String progressText) {
    public QuestStatus(int progress, int target, boolean complete) {
        this(progress, target, complete, null);
    }

    public String displayProgress() {
        return progressText == null ? progress + "/" + target : progressText;
    }
}
