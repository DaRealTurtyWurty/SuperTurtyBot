package dev.darealturtywurty.superturtybot.modules.quest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestPlayer {
    private long user;
    private long guild;
    private long weekStart;
    private List<String> assignedQuestIds = new ArrayList<>();
    private List<String> rewardedQuestIds = new ArrayList<>();
    private boolean rerollUsed;

    public boolean hasBeenRewarded(String questId) {
        return this.rewardedQuestIds != null && this.rewardedQuestIds.contains(questId);
    }
}
