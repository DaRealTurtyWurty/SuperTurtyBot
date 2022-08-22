package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings;

import java.util.concurrent.ThreadLocalRandom;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response.ResponseBuilder;
import io.github.darealturtywurty.superturtybot.modules.idlerpg.pojo.Outcome;
import io.github.darealturtywurty.superturtybot.registry.Registerable;
import net.dv8tion.jda.api.JDA;

public abstract class Finding implements Registerable {
    private String name;

    private final Outcome outcome;
    private final String[] foundMessages;
    
    protected Finding(Outcome outcome, String... foundMessages) {
        this.outcome = outcome;
        this.foundMessages = foundMessages;
    }

    public String[] getFoundMessages() {
        return this.foundMessages;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    public Outcome getOutcome() {
        return this.outcome;
    }
    
    public String randomFoundMessage() {
        return this.foundMessages[ThreadLocalRandom.current().nextInt(this.foundMessages.length)];
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }
    
    public void start(JDA jda, RPGPlayer player, long channel) {
        final ResponseBuilder builder = getResponse(null, player, channel);
        
        if (builder != null && !jda.getRegisteredListeners().contains(builder)) {
            jda.addEventListener(builder);
            builder.run();
        }
    }
    
    protected abstract ResponseBuilder getResponse(JDA jda, RPGPlayer player, long channel);
}