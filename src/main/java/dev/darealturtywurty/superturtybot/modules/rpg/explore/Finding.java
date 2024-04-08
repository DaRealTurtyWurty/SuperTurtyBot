package dev.darealturtywurty.superturtybot.modules.rpg.explore;

import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;

import java.util.concurrent.ThreadLocalRandom;

@Getter
public abstract class Finding implements Registerable {
    private String name;

    private final Outcome outcome;
    private final String[] foundMessages;

    protected Finding(Outcome outcome, String... foundMessages) {
        this.outcome = outcome;
        this.foundMessages = foundMessages;
    }

    public abstract ResponseBuilder getResponse(RPGPlayer player, JDA jda, long guild, long channel);

    public String randomFoundMessage() {
        return this.foundMessages[ThreadLocalRandom.current().nextInt(this.foundMessages.length)];
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }

    public void start(RPGPlayer player, JDA jda, long guild, long channel) {
        final ResponseBuilder builder = getResponse(player, jda, guild, channel);

        if (builder != null && !jda.getRegisteredListeners().contains(builder)) {
            jda.addEventListener(builder);
            builder.run();
        }
    }
}