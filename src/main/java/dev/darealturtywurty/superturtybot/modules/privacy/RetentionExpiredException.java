package dev.darealturtywurty.superturtybot.modules.privacy;

public final class RetentionExpiredException extends IllegalStateException {
    public RetentionExpiredException() {
        super("Saved data for this inactive account has expired. Interact with the bot to start fresh.");
    }
}
