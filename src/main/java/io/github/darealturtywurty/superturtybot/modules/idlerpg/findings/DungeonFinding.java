package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class DungeonFinding extends Finding {
    public DungeonFinding() {
        super(Outcome.UNKNOWN, "Hey! Look! Is that a dungeon? Is it worth the risk though?");
    }
    
    @Override
    public ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
