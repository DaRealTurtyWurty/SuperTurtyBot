package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class TrapFinding extends Finding {
    public TrapFinding() {
        super(Outcome.NEGATIVE, "Oh dear. You stumbled into a trap. Silly you!");
    }

    @Override
    public ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
