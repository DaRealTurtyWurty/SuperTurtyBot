package dev.darealturtywurty.superturtybot.commands.music.handler;

import org.jetbrains.annotations.Nullable;

public class TrackData {
    private long userId;
    private @Nullable String playlist;

    public TrackData(long userId, @Nullable String playlist) {
        this.userId = userId;
        this.playlist = playlist;
    }

    public TrackData(long userId) {
        this(userId, null);
    }

    public long getUserId() {
        return this.userId;
    }

    public @Nullable String getPlaylist() {
        return this.playlist;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setPlaylist(@Nullable String playlist) {
        this.playlist = playlist;
    }
}
