package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import net.dv8tion.jda.api.JDA;

public class ChestFinding extends Finding {
    public ChestFinding() {
        super(Outcome.POSITIVE, "You found a chest! Lets hope you get some nice goodies.");
    }

    @Override
    protected ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel) {
        return null;
    }
}
