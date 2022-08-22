package io.github.darealturtywurty.superturtybot.modules.idlerpg.killsource;

import io.github.darealturtywurty.superturtybot.registry.Registry;

public class KillSourceRegistry {
    public static final Registry<KillSource> KILL_SOURCES = new Registry<>();
    
    public static final KillSource DEFAULT_ENEMY = KILL_SOURCES.register("default_enemy",
        new KillSource("%s was killed by %s!"));
}
