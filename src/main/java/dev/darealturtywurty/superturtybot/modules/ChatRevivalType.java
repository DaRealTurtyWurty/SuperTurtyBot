package dev.darealturtywurty.superturtybot.modules;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum ChatRevivalType {
    DRAWING,
    TOPIC,
    WOULD_YOU_RATHER;

    public static EnumSet<ChatRevivalType> defaults() {
        return EnumSet.allOf(ChatRevivalType.class);
    }

    public static EnumSet<ChatRevivalType> fromStorage(String stored) {
        if (stored == null || stored.isBlank())
            return defaults();

        EnumSet<ChatRevivalType> types = EnumSet.noneOf(ChatRevivalType.class);
        for (String value : stored.split("[,; ]")) {
            if (value == null || value.isBlank())
                continue;

            try {
                types.add(ChatRevivalType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Ignore bad persisted values and fall back to whatever remains valid.
            }
        }

        return types.isEmpty() ? defaults() : types;
    }

    public static String toStorage(Set<ChatRevivalType> types) {
        if (types == null || types.isEmpty())
            return "";

        return types.stream()
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(";"));
    }

    public static boolean isValidStorage(String stored) {
        if (stored == null || stored.isBlank())
            return false;

        return Arrays.stream(stored.split("[,; ]"))
                .filter(value -> value != null && !value.isBlank())
                .allMatch(value -> {
                    try {
                        ChatRevivalType.valueOf(value.trim().toUpperCase(Locale.ROOT));
                        return true;
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                });
    }
}
