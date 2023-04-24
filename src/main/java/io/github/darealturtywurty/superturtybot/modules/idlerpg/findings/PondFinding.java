package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class PondFinding extends Finding {
    public PondFinding() {
        super(Outcome.UNKNOWN, "Lets go fishing!!!! You found a pond.");
    }

    @Override
    public ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
