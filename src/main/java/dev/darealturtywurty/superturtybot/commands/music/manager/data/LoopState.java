package dev.darealturtywurty.superturtybot.commands.music.manager.data;

import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum LoopState {
    NONE, SINGLE, ALL;

    public static @Nullable LoopState fromString(@Nullable String state) {
        if (state == null || state.isBlank())
            return null;

        try {
            return LoopState.valueOf(state.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String asString(@NotNull LoopState state) {
        return WordUtils.capitalize(state.name().toLowerCase(Locale.ROOT));
    }
}
