package dev.darealturtywurty.superturtybot.modules.rpg.explore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;

@Getter
@RequiredArgsConstructor
public enum Outcome {
    POSITIVE(Color.GREEN), UNDEFINED(Color.GRAY), UNKNOWN(Color.MAGENTA), NEGATIVE(Color.RED);

    private final Color color;
}