package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.util.UUID;

import net.dv8tion.jda.api.entities.Member;

public record WarnInfo(Member warner, long warnTime, String reason, UUID uuid) {
    public WarnInfo(Member warner, long warnTime, String reason) {
        this(warner, warnTime, reason, UUID.randomUUID());
    }
}
