package uno.network;

import java.util.ArrayList;
import java.util.List;

public final class NetworkProtocol {
    private NetworkProtocol() {}

    public static final int DEFAULT_GAME_PORT   = 7777;
    public static final int UDP_DISCOVERY_PORT  = 4446;

    public static final String SEP = "\t";

    public static final String S_NOTIFY = "NOTIFY";
    public static final String S_STATE = "STATE";
    public static final String S_PLAYERS = "PLAYERS";
    public static final String S_GAME_OVER = "GAME_OVER";
    public static final String S_LOBBY_UPDATE = "LOBBY_UPDATE";
    public static final String S_GAME_START = "GAME_START";

    public static final String C_NAME = "NAME";
    public static final String C_ACTION = "ACTION";
    
    public static final String C_LEAVE_LOBBY = "LEAVE_LOBBY";

    public static final String UDP_MAGIC = "UNO_V1";

    public static final int MASK_COLOR_RED = 55;
    public static final int MASK_COLOR_GREEN = 56;
    public static final int MASK_COLOR_BLUE = 57;
    public static final int MASK_COLOR_YELLOW = 58;
    public static final int MASK_BINARY_YES = 68;
    public static final int MASK_BINARY_NO = 69;

    public static final String D_UNKNOWN = "UNKNOWN";
    public static final String D_CHALLENGE_DRAW_FOUR = "CHALLENGE_DRAW_FOUR";
    public static final String D_STACK_OR_DRAW = "STACK_OR_DRAW";
    public static final String D_PLAY_OR_DRAW = "PLAY_OR_DRAW";
    public static final String D_PLAY_WHEN_CANNOT_DRAW = "PLAY_WHEN_CANNOT_DRAW";
    public static final String D_PLAY_DRAWN_CARD = "PLAY_DRAWN_CARD";
    public static final String D_CHOOSE_CARD = "CHOOSE_CARD";
    public static final String D_SWAP_HANDS_TARGET = "SWAP_HANDS_TARGET";
    public static final String D_CHOOSE_COLOR = "CHOOSE_COLOR";
    public static final String D_PLAY_IDENTICAL = "PLAY_IDENTICAL";

    // Host transition notice templates.
    public static final String N_MATCH_STARTED = "Partida iniciada.";
    public static final String N_MATCH_STOPPED = "Partida encerrada pelo host. Aguardando no lobby.";
    public static final String N_MATCH_FINISHED_WINNER = "Partida encerrada. Vencedor: %s.";
    public static final String N_MATCH_FINISHED_DRAW = "Partida encerrada. Resultado: EMPATE.";
    public static final String N_MATCH_ENDED_EARLY = "Partida encerrada antes do fim.";

    // GAME_OVER reason tokens.
    public static final String R_MATCH_STOPPED_BY_HOST = "MATCH_STOPPED_BY_HOST";

    public static String encode(String type, String... fields) {
        String safeType = sanitizeField(type);
        if (fields == null || fields.length == 0) return safeType;

        String[] safeFields = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            safeFields[i] = sanitizeField(fields[i]);
        }

        return safeType + SEP + String.join(SEP, safeFields);
    }

    public static String[] decode(String line) {
        if (line == null) return new String[0];
        return line.split(SEP, -1);
    }

    public static String encodeInts(int[] arr) {
        if (arr == null || arr.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }

        return sb.toString();
    }

    public static int[] decodeInts(String csv) {
        if (csv == null || csv.isEmpty()) return new int[0];
        String[] parts = csv.split(",");
        List<Integer> parsed = new ArrayList<>(parts.length);

        for (int i = 0; i < parts.length; i++) {
            String token = parts[i].trim();
            if (token.isEmpty()) continue;

            try {
                parsed.add(Integer.parseInt(token));
            } catch (NumberFormatException ignored) {
                // Ignore malformed entries to keep network parsing resilient.
            }
        }

        int[] result = new int[parsed.size()];
        for (int i = 0; i < parsed.size(); i++) {
            result[i] = parsed.get(i);
        }

        return result;
    }

    private static String sanitizeField(String value) {
        if (value == null) return "";
        return value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}