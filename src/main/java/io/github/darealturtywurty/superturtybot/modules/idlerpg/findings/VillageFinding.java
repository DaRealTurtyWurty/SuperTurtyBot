package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class VillageFinding extends Finding {
    public VillageFinding() {
        super(Outcome.POSITIVE, "Wait. There are civilizations on this planet? You found a village!");
    }

    @Override
    protected ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
