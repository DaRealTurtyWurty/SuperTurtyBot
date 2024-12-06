package dev.darealturtywurty.superturtybot.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

public class TurtyBotHighlightingCompositeConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        Level level = event.getLevel();
        if (getFirstOption().equals("thread")) {
            return switch (level.toInt()) {
                case Level.ERROR_INT -> ANSIConstants.RED_FG;
                case Level.WARN_INT -> ANSIConstants.YELLOW_FG;
                case Level.INFO_INT -> ANSIConstants.GREEN_FG;
                case Level.DEBUG_INT -> ANSIConstants.GREEN_FG;
                case Level.TRACE_INT -> ANSIConstants.BLUE_FG;
                default -> "";
            };
        } else if (getFirstOption().equals("msg")) {
            return level.toInt() == Level.ERROR_INT ? ANSIConstants.RED_FG : "";
        }
        return "";
    }
}