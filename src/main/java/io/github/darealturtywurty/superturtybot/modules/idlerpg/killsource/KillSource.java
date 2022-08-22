package io.github.darealturtywurty.superturtybot.modules.idlerpg.killsource;

import io.github.darealturtywurty.superturtybot.registry.Registerable;

public class KillSource implements Registerable {
    private String name;
    private final String killMessage;

    public KillSource(String message) {
        this.killMessage = message;
    }
    
    public String getKillMessage() {
        return this.killMessage;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }
}
