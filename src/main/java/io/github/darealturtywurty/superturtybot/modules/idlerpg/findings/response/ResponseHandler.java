package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response;

import io.github.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public abstract class ResponseHandler extends ListenerAdapter {
    private final RPGPlayer player;
    private final long channel;
    
    protected ResponseHandler(RPGPlayer player, long channel) {
        this.player = player;
        this.channel = channel;
    }
    
    public abstract void start(JDA jda);
    
    public void stop(JDA jda) {
        jda.removeEventListener(this);
    }
}
