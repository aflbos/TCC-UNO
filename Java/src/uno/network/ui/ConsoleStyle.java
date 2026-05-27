package uno.network.ui;

final class ConsoleStyle {
    private static final boolean COLORS_ENABLED = true;

    static final String RESET = code("\u001B[0m");
    static final String BOLD = code("\u001B[1m");
    static final String CYAN = code("\u001B[36m");
    static final String GREEN = code("\u001B[32m");
    static final String YELLOW = code("\u001B[33m");
    static final String RED = code("\u001B[31m");
    static final String BLUE = code("\u001B[34m");
    static final String MAGENTA = code("\u001B[35m");

    private ConsoleStyle() {}

    private static String code(String raw) {
        return COLORS_ENABLED ? raw : "";
    }

    private static boolean detectColorSupport() {
        return true;
    }
}

