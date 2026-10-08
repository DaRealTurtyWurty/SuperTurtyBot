package dev.darealturtywurty.superturtybot.modules.quest.impl;

import dev.darealturtywurty.superturtybot.modules.quest.QuestEvent;
import dev.darealturtywurty.superturtybot.modules.quest.QuestRule;
import dev.darealturtywurty.superturtybot.modules.quest.QuestStatus;

public final class Game2048Rule implements QuestRule<Game2048Config, Game2048State> {
    @Override
    public Game2048State createState() {
        return new Game2048State();
    }

    @Override
    public void apply(Game2048Config config, Game2048State state, QuestEvent event) {
        if (event instanceof QuestEvent.Game2048Progress(_, int highestTile, boolean finished)
            && (!config.mustFinish() || finished)) {
            state.highestTile = Math.max(state.highestTile, highestTile);
        }
    }

    @Override
    public QuestStatus status(Game2048Config config, Game2048State state) {
        int progress = Math.min(state.highestTile, config.requiredTile());
        return new QuestStatus(
            progress,
            config.requiredTile(),
            progress >= config.requiredTile(),
            state.highestTile + "/" + config.requiredTile() + " tile");
    }
}
