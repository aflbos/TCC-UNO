package uno.network.ui;

final class ConsoleStyle {
    private static final boolean COLORS_ENABLED = detectColorSupport();

    static final String RESET = code("\u001B[0m");
    static final String BOLD = code("\u001B[1m");
    static final String CYAN = code("\u001B[36m");
    static final String GREEN = code("\u001B[32m");
    static final String YELLOW = code("\u001B[33m");
    static final String RED = code("\u001B[31m");
    static final String BLUE = code("\u001B[34m");
    static final String MAGENTA = code("\u001B[35m");

    private ConsoleStyle() {}

    static boolean isColorEnabled() {
        return COLORS_ENABLED;
    }

    private static String code(String raw) {
        return COLORS_ENABLED ? raw : "";
    }

    private static boolean detectColorSupport() {
        String configured = System.getProperty("uno.ui.color", "auto").trim().toLowerCase();
        if ("on".equals(configured) || "true".equals(configured)) {
            return true;
        }
        if ("off".equals(configured) || "false".equals(configured)) {
            return false;
        }

        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        if (System.console() == null) {
            return false;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return true;
        }

        return System.getenv("WT_SESSION") != null
                || System.getenv("ANSICON") != null
                || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))
                || System.getenv("TERM") != null;
    }
}

