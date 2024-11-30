package dev.darealturtywurty.superturtybot.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

public class MinecraftHighlightingCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        Level level = event.getLevel();
        if (getFirstOption().equals("thread")) {
            return switch (level.toInt()) {
                case Level.ERROR_INT -> RED_FG;
                case Level.WARN_INT -> YELLOW_FG;
                case Level.INFO_INT -> GREEN_FG;
                case Level.DEBUG_INT -> GREEN_FG;
                case Level.TRACE_INT -> BLUE_FG;
                default -> "";
            };
        } else if (getFirstOption().equals("msg")) {
            return level.toInt() == Level.ERROR_INT ? RED_FG : "";
        }
        return "";
    }
}