package dev.darealturtywurty.superturtybot.modules.rpg.explore.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Operation {
    void run(MessageReceivedEvent event, int index);
}