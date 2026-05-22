package uno.game.loggers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class StructuredLog {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private StructuredLog() {}

    static String line(String level, String event, String message) {
        String safeLevel = level == null ? "INFO" : level;
        String safeEvent = event == null ? "general" : event;
        String safeMessage = message == null ? "" : message;
        return String.format("ts=%s level=%s event=%s msg=\"%s\"",
                LocalDateTime.now().format(TS),
                safeLevel,
                safeEvent,
                escape(safeMessage));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

