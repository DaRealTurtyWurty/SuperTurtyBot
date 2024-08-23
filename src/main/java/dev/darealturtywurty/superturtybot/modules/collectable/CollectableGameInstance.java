package dev.darealturtywurty.superturtybot.modules.collectable;

public record CollectableGameInstance<T extends Collectable>(long guildId, long channelId, long messageId, T collectable) {
    public CollectableGameInstance {
        if (guildId <= 0 || channelId <= 0 || messageId <= 0)
            throw new IllegalArgumentException("Guild ID, Channel ID, and Message ID must be greater than 0!");
    }

    public boolean isSameMessage(long guildId, long channelId, long messageId) {
        return isSameChannel(guildId, channelId) && this.messageId == messageId;
    }

    public boolean isSameChannel(long guildId, long channelId) {
        return this.guildId == guildId && this.channelId == channelId;
    }
}
