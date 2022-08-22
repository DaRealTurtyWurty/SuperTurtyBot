package io.github.darealturtywurty.superturtybot.modules.idlerpg.findings.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Operation {
    void run(MessageReceivedEvent event, int index);
}
