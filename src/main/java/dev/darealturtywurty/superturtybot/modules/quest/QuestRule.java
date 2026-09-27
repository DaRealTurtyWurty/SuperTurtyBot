package dev.darealturtywurty.superturtybot.modules.quest;

public interface QuestRule<C, S> {
    S createState();

    void apply(C config, S state, QuestEvent event);

    QuestStatus status(C config, S state);
}
