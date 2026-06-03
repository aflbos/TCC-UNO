package uno.network.ui;

public final class ConsoleStyle {
    public enum ColorMode {
        AUTO,
        ON,
        OFF
    }

    private static volatile ColorMode colorMode;
    private static volatile boolean colorsEnabled;
    private static volatile boolean lastAutoSupport;

    public static String RESET;
    public static String BOLD;
    public static String CYAN;
    public static String GREEN;
    public static String YELLOW;
    public static String RED;
    public static String BLUE;
    public static String MAGENTA;

    static {
        colorMode = resolveModeFromEnv();
        colorsEnabled = computeColorsEnabled(colorMode);
        refreshCodes();
    }

    private ConsoleStyle() {}

    private static String code(String raw) {
        return colorsEnabled ? raw : "";
    }

    private static void refreshCodes() {
        RESET = code("\u001B[0m");
        BOLD = code("\u001B[1m");
        CYAN = code("\u001B[36m");
        GREEN = code("\u001B[32m");
        YELLOW = code("\u001B[33m");
        RED = code("\u001B[31m");
        BLUE = code("\u001B[34m");
        MAGENTA = code("\u001B[35m");
    }

    public static synchronized void setColorMode(ColorMode mode) {
        if (mode == null) return;
        colorMode = mode;
        colorsEnabled = computeColorsEnabled(mode);
        refreshCodes();
    }

    public static ColorMode getColorMode() {
        return colorMode;
    }

    public static boolean isColorsEnabled() {
        return colorsEnabled;
    }

    public static boolean isAutoSupported() {
        return lastAutoSupport;
    }

    public static ColorMode parseColorMode(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase();
        switch (value) {
            case "auto":
                return ColorMode.AUTO;
            case "on":
            case "true":
            case "1":
            case "yes":
            case "always":
                return ColorMode.ON;
            case "off":
            case "false":
            case "0":
            case "no":
            case "never":
                return ColorMode.OFF;
            default:
                return null;
        }
    }

    private static ColorMode resolveModeFromEnv() {
        if (System.getenv("NO_COLOR") != null) {
            return ColorMode.OFF;
        }
        String raw = System.getProperty("uno.color");
        if (raw == null || raw.trim().isEmpty()) {
            raw = System.getenv("UNO_COLOR");
        }
        ColorMode parsed = parseColorMode(raw);
        return parsed != null ? parsed : ColorMode.AUTO;
    }

    private static boolean computeColorsEnabled(ColorMode mode) {
        if (mode == ColorMode.ON) {
            return true;
        }
        if (mode == ColorMode.OFF) {
            return false;
        }
        lastAutoSupport = detectColorSupport();
        return lastAutoSupport;
    }

    private static boolean detectColorSupport() {
        if (System.console() == null) {
            return false;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return true;
        }

        if (System.getenv("WT_SESSION") != null) return true;
        if (System.getenv("ANSICON") != null) return true;
        if ("ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))) return true;
        String term = System.getenv("TERM");
        return term != null && !term.trim().isEmpty();
    }

    public static String stripANSI(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    public static String padRight(String s, int n) {
        int visibleLength = stripANSI(s).length();
        if (visibleLength >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        for (int i = visibleLength; i < n; i++) sb.append(' ');
        return sb.toString();
    }
}