package dev.darealturtywurty.superturtybot.commands.music.handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum LoopState {
    NONE, SINGLE, ALL;

    public static @Nullable LoopState fromString(@Nullable String state) {
        if (state == null || state.isBlank()) {
            return null;
        }

        return switch (state.toLowerCase(Locale.ROOT)) {
            case "none" -> NONE;
            case "single" -> SINGLE;
            case "all" -> ALL;
            default -> null;
        };
    }

    public static String asString(@NotNull LoopState state) {
        return switch (state) {
            case NONE -> "None";
            case SINGLE -> "Single";
            case ALL -> "All";
        };
    }
}
