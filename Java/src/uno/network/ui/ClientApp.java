package uno.network.ui;

import uno.network.GameClient;
import uno.network.GameClientListener;
import uno.network.NetworkProtocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientApp {
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    private static final String RESET = ConsoleStyle.RESET;
    private static final String BOLD = ConsoleStyle.BOLD;
    private static final String CYAN = ConsoleStyle.CYAN;
    private static final String GREEN = ConsoleStyle.GREEN;
    private static final String YELLOW = ConsoleStyle.YELLOW;
    private static final String RED = ConsoleStyle.RED;
    private static final String BLUE = ConsoleStyle.BLUE;
    private static final String MAGENTA = ConsoleStyle.MAGENTA;
    private static final Object CONSOLE_LOCK = new Object();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        printWelcome();
        runClient(scanner);
    }

    private static void printWelcome() {
        System.out.println("\n" + BOLD + CYAN + "╔════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + CYAN +        "║      UNO Network Card Game         ║" + RESET);
        System.out.println(BOLD + CYAN +        "║        Player Client 1.0           ║" + RESET);
        System.out.println(BOLD + CYAN +        "╚════════════════════════════════════╝" + RESET + "\n");
    }

    private static void runClient(Scanner scanner) {
        String name = "";
        String host = "127.0.0.1";
        int port = NetworkProtocol.DEFAULT_GAME_PORT;

        printConnectHelp();
        while (true) {
            String line = prompt(scanner, BOLD + "player> " + RESET);
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "h":
                case "help":
                    printConnectHelp();
                    continue;

                case "color":
                    System.out.println(CYAN + "Color output is " + (ConsoleStyle.isColorEnabled() ? "enabled" : "disabled")
                            + ". Use -Duno.ui.color=on|off|auto to configure." + RESET);
                    continue;

                case "clear":
                    for (int i = 0; i < 30; i++) System.out.println();
                    continue;

                case "show":
                    System.out.println(CYAN + "\nCurrent settings:" + RESET);
                    System.out.println("  name = " + (name.isEmpty() ? RED + "(not set)" + RESET : GREEN + name + RESET));
                    System.out.println("  host = " + GREEN + host + RESET);
                    System.out.println("  port = " + GREEN + port + RESET + "\n");
                    continue;

                case "setname":
                    if (parts.length < 2) {
                        System.out.println(YELLOW + "Usage: setname <player-name>" + RESET);
                        continue;
                    }

                    name = line.substring(line.indexOf(' ') + 1).trim();

                    if (name.isEmpty()) {
                        System.out.println(RED + "Name cannot be empty." + RESET);
                    } else {
                        System.out.println(GREEN + "✓ Name set to: " + name + RESET);
                    }
                    continue;

                case "prompt": {
                    String shownName = name.isEmpty() ? "not set" : name;
                    String promptedName = prompt(scanner, "Your name [" + shownName + "]: ");

                    if (!promptedName.isEmpty()) {
                        name = promptedName;
                    }

                    host = prompt(scanner, "Server IP [" + host + "]: ");
                    if (host.isEmpty()) host = "127.0.0.1";
                    port = promptForPort(scanner, "Port", port);
                    continue;
                }

                case "c":
                case "connect":
                    String connectTarget = parts.length >= 2 ? parts[1] : "";
                    if (!connectTarget.isEmpty()) {
                        String[] hp = connectTarget.split(":", 2);

                        if (hp.length != 2 || hp[0].trim().isEmpty()) {
                            System.out.println(YELLOW + "Usage: connect <ip>:<port>" + RESET);
                            continue;
                        }

                        Integer parsed = tryParsePort(hp[1]);

                        if (parsed == null) {
                            printPortError(hp[1]);
                            continue;
                        }

                        host = hp[0].trim();
                        port = parsed;
                    }

                    if (name.trim().isEmpty()) {
                        String promptedName = prompt(scanner, "Set your player name first: ");

                        if (promptedName.trim().isEmpty()) {
                            System.out.println(RED + "Name required. Use `setname <name>`." + RESET);
                            continue;
                        }

                        name = promptedName.trim();
                    }

                    if (host.trim().isEmpty()) {
                        System.out.println(RED + "Host cannot be empty. Use `sethost` first." + RESET);
                        continue;
                    }

                    GameClient client = new GameClient(name);
                    client.setDisconnectAfterGameOver(false);
                    CliClientListener sessionListener = new CliClientListener(client, scanner);
                    client.setListener(sessionListener);

                    try {
                        System.out.println(CYAN + "Connecting to " + host + ":" + port + "..." + RESET);
                        client.connect(host, port);
                        client.startReadLoop();
                        System.out.println(GREEN + "✓ Connected! Waiting in lobby..." + RESET);
                    } catch (IOException e) {
                        System.err.println(RED + "✗ Connection failed: " + e.getMessage() + RESET);
                        continue;
                    }

                    runConnectedLobbyLoop(scanner, sessionListener);

                    System.out.println(CYAN + "Back at connection menu." + RESET);

                    continue;
                case "quit":
                case "q":
                case "exit":
                case "menu":
                    System.out.println(CYAN + "Goodbye!" + RESET + "\n");
                    return;
            }

            System.out.println(RED + "Unknown command. Type `help`." + RESET);
        }
    }

    private static void printConnectHelp() {
        System.out.println(BOLD + CYAN + "\n═══ Connection Menu ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "              Show commands");
        System.out.println(GREEN + "  show" + RESET + "                  Show current name/host/port");
        System.out.println(GREEN + "  setname <name>" + RESET + "        Set player name");
        System.out.println(GREEN + "  prompt" + RESET + "                Interactive prompts for all fields");
        System.out.println(GREEN + "  connect | c" + RESET + "           Connect using current settings");
        System.out.println(GREEN + "  connect <ip>:<port>" + RESET + "   Set target and connect immediately");
        System.out.println(GREEN + "  color" + RESET + "                 Show color output status");
        System.out.println(GREEN + "  clear" + RESET + "                 Clear screen");
        System.out.println(GREEN + "  quit | q" + RESET + "              Exit application");
        System.out.println();
    }

    private static void runConnectedLobbyLoop(Scanner scanner, CliClientListener listener) {
        System.out.println(CYAN + "Connected lobby mode: type 'help' for lobby commands." + RESET);

        while (listener.isSessionActive()) {
            if (listener.isAwaitingDecision()) {
                sleepQuietly(100);
                continue;
            }

            String line = tryReadConsoleLine(scanner, BOLD + "lobby> " + RESET, listener);
            if (line == null) {
                sleepQuietly(100);
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }

            String cmd = line.trim().toLowerCase();
            switch (cmd) {
                case "help":
                    printLobbyHelp();
                    break;
                case "players":
                case "lobby":
                case "status":
                    listener.printLobbySnapshot();
                    break;
                case "disconnect":
                case "menu":
                case "leave":
                    listener.requestDisconnect("Disconnected and returned to menu.");
                    break;
                default:
                    System.out.println(YELLOW + "Unknown lobby command. Type 'help'." + RESET);
                    break;
            }
        }
    }

    private static String tryReadConsoleLine(Scanner scanner, String promptText, CliClientListener listener) {
        synchronized (CONSOLE_LOCK) {
            try {
                if (!listener.isSessionActive() || listener.isAwaitingDecision()) {
                    return null;
                }
                if (System.in.available() <= 0) {
                    return null;
                }
            } catch (IOException ignored) {
                return null;
            }

            return prompt(scanner, promptText);
        }
    }

    private static void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printLobbyHelp() {
        System.out.println(BOLD + CYAN + "\n=== Lobby Commands ===" + RESET);
        System.out.println(GREEN + "  help" + RESET + "          Show this command list");
        System.out.println(GREEN + "  players" + RESET + "       Show latest players summary");
        System.out.println(GREEN + "  lobby" + RESET + "         Alias for players");
        System.out.println(GREEN + "  status" + RESET + "        Alias for players");
        System.out.println(GREEN + "  disconnect" + RESET + "    Leave server and return to connection menu");
        System.out.println();
    }

    private static String prompt(Scanner scanner, String text) {
        System.out.print(text);
        return scanner.nextLine().trim();
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int promptForPort(Scanner scanner, String label, int fallback) {
        while (true) {
            String raw = prompt(scanner, label + " [" + fallback + "]: ");
            if (raw.isEmpty()) return fallback;
            Integer parsed = tryParsePort(raw);
            if (parsed != null) return parsed;
            printPortError(raw);
        }
    }

    private static Integer tryParsePort(String raw) {
        int value;

        try {
            value = Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return null;
        }

        if (value < MIN_PORT || value > MAX_PORT) return null;

        return value;
    }

    private static void printPortError(String raw) {
        System.out.println(RED + "Invalid port: '" + raw + "'. Enter a number in range " + MIN_PORT + "-" + MAX_PORT + "." + RESET);
    }

    private static final class CliClientListener implements GameClientListener {
        private final GameClient client;
        private final Scanner scanner;
        private String lastPlayersSummary = "";
        private volatile boolean sessionActive = true;
        private volatile boolean awaitingDecision = false;

        private CliClientListener(GameClient client, Scanner scanner) {
            this.client = client;
            this.scanner = scanner;
        }

        @Override
        public void onNotify(String message) {
            if (!message.isEmpty()) System.out.println(BLUE + "[notify] " + message + RESET);
        }

        @Override
        public void onLobbyUpdate(String summary) {
            if (summary == null || summary.isEmpty()) {
                System.out.println(CYAN + "[lobby] (empty)" + RESET);
                return;
            }

            System.out.println(CYAN + "[lobby]" + RESET);

            for (String entry : summary.split(",")) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    System.out.println("  - " + GREEN + parts[0] + RESET + " [" + parts[1] + " / " + parts[2] + "]");
                }
            }
        }

        @Override
        public void onPlayers(String summary) {
            if (summary != null && !summary.isEmpty()) {
                lastPlayersSummary = summary;
                System.out.println(CYAN + "[players] " + summary + RESET);
            }
        }

        @Override
        public void onGameStart() {
            System.out.println(GREEN + BOLD + "🎮 Game started!" + RESET);
        }

        @Override
        public void onState(String handDesc, String topCard, int[] validInputs, String decisionId, String prompt, int[] playableCardIds) {
            awaitingDecision = true;
            System.out.println();
            System.out.println(BOLD + YELLOW + "┌─ Game State ─┐" + RESET);
            System.out.println(MAGENTA + "Top card: " + RESET + BOLD + topCard + RESET);
            printHand(handDesc);
            System.out.println(CYAN + prompt + RESET);
            printDecisionLabels(handDesc, validInputs, playableCardIds, decisionId, prompt);

            try {
                while (true) {
                    String raw;
                    synchronized (CONSOLE_LOCK) {
                        raw = prompt(scanner, BOLD + "player> " + RESET);
                    }
                    if (raw.isEmpty()) continue;

                    String[] parts = raw.split("\\s+");
                    String cmd = parts[0].toLowerCase();

                    switch (cmd) {
                        case "help":
                            printPlayerHelp();
                            continue;

                        case "state":
                            System.out.println(MAGENTA + "Top card: " + RESET + BOLD + topCard + RESET);
                            System.out.println(CYAN + prompt + RESET);
                            printDecisionLabels(handDesc, validInputs, playableCardIds, decisionId, prompt);
                            continue;

                        case "hand":
                            printHand(handDesc);
                            continue;

                        case "players":
                            printLobbySnapshot();
                            continue;

                        case "actions":
                            printDecisionLabels(handDesc, validInputs, playableCardIds, decisionId, prompt);
                            continue;

                        case "disconnect":
                        case "menu":
                            requestDisconnect("Disconnected and returned to menu.");
                            return;
                    }

                    int action;

                    if ("choose".equals(cmd)) {
                        if (parts.length < 2) {
                            System.out.println(YELLOW + "Usage: choose <action-id>" + RESET);
                            continue;
                        }

                        action = parseIntOrDefault(parts[1], Integer.MIN_VALUE);
                    } else {
                        action = parseIntOrDefault(raw, Integer.MIN_VALUE);
                    }

                    if (contains(validInputs, action)) {
                        client.submitAction(action);
                        return;
                    }

                    System.out.println(RED + "Invalid action. Use `actions` to see valid choices." + RESET);
                }
            } finally {
                awaitingDecision = false;
            }
        }

        @Override
        public void onGameOver(String winnerName) {
            if (NetworkProtocol.R_MATCH_STOPPED_BY_HOST.equals(winnerName)) {
                System.out.println(BOLD + YELLOW + "⚠ Match stopped by host." + RESET);
            } else {
                System.out.println(BOLD + GREEN + "✓ Game over. Winner: " + winnerName + RESET);
            }
            System.out.println(CYAN + "Staying connected in lobby. Waiting for next match..." + RESET);
        }

        @Override
        public void onDisconnect(String reason) {
            System.out.println(RED + "Disconnected: " + reason + RESET);
            shutdownClient("Connection closed.");
        }

        private void shutdownClient(String reason) {
            client.disconnect();
            sessionActive = false;
            awaitingDecision = false;
            System.out.println(CYAN + reason + RESET);
        }

        private void requestDisconnect(String reason) {
            shutdownClient(reason);
        }

        private boolean isSessionActive() {
            return sessionActive;
        }

        private boolean isAwaitingDecision() {
            return awaitingDecision;
        }

        private void printLobbySnapshot() {
            if (lastPlayersSummary == null || lastPlayersSummary.isEmpty()) {
                System.out.println(YELLOW + "Players: (no update yet)" + RESET);
            } else {
                System.out.println(CYAN + "Players: " + lastPlayersSummary + RESET);
            }
        }

        private static boolean contains(int[] values, int target) {
            for (int value : values) {
                if (value == target) return true;
            }
            return false;
        }

        private static void printHand(String handDesc) {
            if (handDesc == null || handDesc.isEmpty()) {
                System.out.println(YELLOW + "Hand: (empty)" + RESET);
                return;
            }

            System.out.println(YELLOW + "Hand:" + RESET);

            String[] cards = handDesc.split(",");

            for (int i = 0; i < cards.length; i++) {
                String[] fields = cards[i].split(":", 2);
                String card = fields.length > 1 ? fields[1] : cards[i];
                System.out.println("  " + GREEN + i + RESET + ") " + card);
            }
        }

        private static void printPlayerHelp() {
            System.out.println(BOLD + CYAN + "\n═══ Available Commands ═══" + RESET);
            System.out.println(GREEN + "  help" + RESET + "                Show command list");
            System.out.println(GREEN + "  state" + RESET + "               Reprint top card and prompt");
            System.out.println(GREEN + "  hand" + RESET + "                Reprint your hand");
            System.out.println(GREEN + "  players" + RESET + "             Show player summary");
            System.out.println(GREEN + "  actions" + RESET + "             Show labeled options");
            System.out.println(GREEN + "  choose <action-id>" + RESET + "  Submit one action");
            System.out.println(GREEN + "  <number>" + RESET + "            Shortcut for choose <number>");
            System.out.println(GREEN + "  disconnect" + RESET + "          Leave and return to menu");
            System.out.println();
        }

        private static void printDecisionLabels(String handDesc, int[] validInputs, int[] playableCardIds, String decisionId, String promptText) {
            List<CardEntry> cards = parseHand(handDesc);
            System.out.println(BOLD + CYAN + "Options:" + RESET);

            for (int action : validInputs) {
                String description = describeAction(action, cards, playableCardIds, decisionId, promptText);
                System.out.println("  " + BOLD + GREEN + action + RESET + " => " + description);
            }
        }

        private static String describeAction(int action, List<CardEntry> cards, int[] playableCardIds, String decisionId, String promptText) {
            String id = decisionId == null || decisionId.isEmpty() ? NetworkProtocol.D_UNKNOWN : decisionId;

            if (NetworkProtocol.D_CHOOSE_COLOR.equals(id)) {
                if (action == 1 || action == NetworkProtocol.MASK_COLOR_RED) {
                    return RED + "Choose color RED" + RESET;
                }

                if (action == 2 || action == NetworkProtocol.MASK_COLOR_GREEN) {
                    return GREEN + "Choose color GREEN" + RESET;
                }

                if (action == 3 || action == NetworkProtocol.MASK_COLOR_BLUE) {
                    return BLUE + "Choose color BLUE" + RESET;
                }

                if (action == 4 || action == NetworkProtocol.MASK_COLOR_YELLOW) {
                    return YELLOW + "Choose color YELLOW" + RESET;
                }
            }

            if (isBinaryDecision(id)) {
                if (action == 1 || action == NetworkProtocol.MASK_BINARY_YES) {
                    return describeBinaryChoice(true, id, promptText);
                }
                if (action == 2 || action == NetworkProtocol.MASK_BINARY_NO) {
                    return describeBinaryChoice(false, id, promptText);
                }
            }

            if (NetworkProtocol.D_CHOOSE_CARD.equals(id)
                    || (NetworkProtocol.D_UNKNOWN.equals(id) && playableCardIds.length > 0)) {
                String playByPlayableIndex = describePlayableCardIndex(action, cards, playableCardIds);
                if (playByPlayableIndex != null) {
                    return playByPlayableIndex;
                }
            }

            String playFallback = describePlayableCardFallback(action, cards, playableCardIds);
            if (playFallback != null) {
                return playFallback;
            }

            if (NetworkProtocol.D_SWAP_HANDS_TARGET.equals(id)) {
                return "Swap hands with player #" + action;
            }

            if (NetworkProtocol.D_UNKNOWN.equals(id) && action == 1) {
                return GREEN + "Yes / Option 1" + RESET;
            }

            if (NetworkProtocol.D_UNKNOWN.equals(id) && action == 2) {
                return RED + "No / Option 2" + RESET;
            }

            return "Action";
        }

        private static boolean isBinaryDecision(String decisionId) {
            return NetworkProtocol.D_PLAY_OR_DRAW.equals(decisionId)
                    || NetworkProtocol.D_PLAY_WHEN_CANNOT_DRAW.equals(decisionId)
                    || NetworkProtocol.D_PLAY_DRAWN_CARD.equals(decisionId)
                    || NetworkProtocol.D_STACK_OR_DRAW.equals(decisionId)
                    || NetworkProtocol.D_PLAY_IDENTICAL.equals(decisionId)
                    || NetworkProtocol.D_CHALLENGE_DRAW_FOUR.equals(decisionId);
        }

        private static String describePlayableCardIndex(int action, List<CardEntry> cards, int[] playableCardIds) {
            if (action < 0 || action >= playableCardIds.length) {
                return null;
            }

            int cardId = playableCardIds[action];
            String cardLabel = findCardLabelById(cards, cardId);

            if (cardLabel != null) {
                return "Play card #" + GREEN + action + RESET + " (" + cardLabel + ")";
            }

            return "Play card #" + GREEN + action + RESET;
        }

        private static String describePlayableCardFallback(int action, List<CardEntry> cards, int[] playableCardIds) {
            for (int i = 0; i < playableCardIds.length; i++) {
                if (playableCardIds[i] != action) continue;

                if (i >= 0 && i < cards.size()) {
                    return "Play card #" + GREEN + i + RESET + " (" + cards.get(i).label + ")";
                }

                return "Play card";
            }

            for (int i = 0; i < cards.size(); i++) {
                CardEntry card = cards.get(i);
                if (card.cardId == action) {
                    return "Play card #" + GREEN + i + RESET + " (" + card.label + ")";
                }
            }

            return null;
        }

        private static String findCardLabelById(List<CardEntry> cards, int cardId) {
            for (CardEntry card : cards) {
                if (card.cardId == cardId) {
                    return card.label;
                }
            }
            return null;
        }

        private static String describeBinaryChoice(boolean yes, String decisionId, String promptText) {
            String id = decisionId == null || decisionId.isEmpty()
                    ? NetworkProtocol.D_UNKNOWN : decisionId;

            if (NetworkProtocol.D_PLAY_OR_DRAW.equals(id)) {
                return yes ? GREEN + "Play a card" + RESET : RED + "Draw a card" + RESET;
            }

            if (NetworkProtocol.D_PLAY_WHEN_CANNOT_DRAW.equals(id)) {
                return yes ? GREEN + "Play a card" + RESET : RED + "Do not play" + RESET;
            }

            if (NetworkProtocol.D_PLAY_DRAWN_CARD.equals(id)) {
                return yes ? GREEN + "Play the drawn card" + RESET : RED + "Keep it" + RESET;
            }

            if (NetworkProtocol.D_STACK_OR_DRAW.equals(id)) {
                return yes ? GREEN + "Stack a card" + RESET : RED + "Draw penalty cards" + RESET;
            }

            if (NetworkProtocol.D_PLAY_IDENTICAL.equals(id)) {
                return yes ? GREEN + "Play another identical card" + RESET
                        : RED + "Do not play another" + RESET;
            }

            if (NetworkProtocol.D_CHALLENGE_DRAW_FOUR.equals(id)) {
                return yes ? GREEN + "Challenge" + RESET : RED + "Do not challenge" + RESET;
            }

            String prompt = (promptText == null || promptText.isEmpty()) ? "(no prompt)" : promptText;
            System.err.println("[DEBUG] Unmapped binary decision ID: " + id + " | prompt: \"" + prompt + "\"");
            return yes ? GREEN + "Yes" + RESET : RED + "No" + RESET;
        }

        private static List<CardEntry> parseHand(String handDesc) {
            List<CardEntry> result = new ArrayList<>();

            if (handDesc == null || handDesc.isEmpty()) return result;

            String[] cards = handDesc.split(",");

            for (String entry : cards) {
                String[] fields = entry.split(":", 2);
                int cardId = parseIntOrDefault(fields[0], Integer.MIN_VALUE);
                String label = fields.length > 1 ? fields[1] : entry;
                result.add(new CardEntry(cardId, label));
            }

            return result;
        }

        private static final class CardEntry {
            private final int cardId;
            private final String label;

            private CardEntry(int cardId, String label) {
                this.cardId = cardId;
                this.label = label;
            }
        }
    }
}