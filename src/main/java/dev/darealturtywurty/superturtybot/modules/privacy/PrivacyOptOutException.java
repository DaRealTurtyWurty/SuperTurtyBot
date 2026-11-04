package dev.darealturtywurty.superturtybot.modules.privacy;

public final class PrivacyOptOutException extends IllegalStateException {
    public PrivacyOptOutException() {
        super("Optional data processing is disabled for this account.");
    }
}
