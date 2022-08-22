package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class BlankFinding extends Finding {
    public BlankFinding() {
        super(Outcome.UNDEFINED, "Unfortunately, you did not find anything. Better luck next time!");
    }

    @Override
    protected ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
