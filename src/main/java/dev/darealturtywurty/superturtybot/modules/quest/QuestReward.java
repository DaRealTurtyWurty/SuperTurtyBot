package dev.darealturtywurty.superturtybot.modules.quest;

@FunctionalInterface
public interface QuestReward {
    void giveReward(Quest quest, QuestPlayer player);

    default String getDescription() {
        return "Quest reward";
    }

    static QuestReward xp(int amount) {
        if (amount < 1)
            throw new IllegalArgumentException("XP reward must be greater than 0");

        return new QuestReward() {
            @Override
            public void giveReward(Quest quest, QuestPlayer player) {
                QuestManager.INSTANCE.awardXP(player, amount);
            }

            @Override
            public String getDescription() {
                return amount + " XP";
            }
        };
    }
}
