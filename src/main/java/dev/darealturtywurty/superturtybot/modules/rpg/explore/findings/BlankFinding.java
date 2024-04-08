package dev.darealturtywurty.superturtybot.modules.rpg.explore.findings;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Outcome;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import net.dv8tion.jda.api.JDA;

public class BlankFinding extends Finding {
    public BlankFinding() {
        super(Outcome.UNDEFINED, "Unfortunately, you did not find anything. Better luck next time!");
    }

    @Override
    public ResponseBuilder getResponse(RPGPlayer player, JDA jda, long guild, long channel) {
        return null;
    }
}
