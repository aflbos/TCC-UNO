package uno.network.ui;

import uno.ai.network.ConnectionAI;
import uno.game.event.PrintSpectator;
import uno.network.GameServer;
import uno.network.NetworkProtocol;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class HostApp {
    private static final int RULE_COUNT = 8;
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

    private static final String[] RULE_NAMES = {
            "Stacking (+2/+4)",
            "Skip 'n Flip",
            "Seven / Zero",
            "Force play drawn card",
            "Draw and play after penalties",
            "Draw to match",
            "Bluffing challenge +4",
            "Play identical cards"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        printWelcome();
        runShell(scanner);
    }

    private static void printWelcome() {
        System.out.println("\n" + BOLD + MAGENTA + "╔════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + MAGENTA +        "║      UNO Network Card Game         ║" + RESET);
        System.out.println(BOLD + MAGENTA +        "║         Host Server 1.0            ║" + RESET);
        System.out.println(BOLD + MAGENTA +        "╚════════════════════════════════════╝" + RESET + "\n");
    }

    private static void runShell(Scanner scanner) {
        String hostName = System.getProperty("user.name", "Host");
        GameServer server = null;
        boolean hosting = false;
        int currentPort = NetworkProtocol.DEFAULT_GAME_PORT;

        boolean[] rules = new boolean[RULE_COUNT];
        HostShellState state = new HostShellState();

        printMainHelp();

        while (true) {
            String line = prompt(scanner, BOLD + (hosting ? "host> " : "menu> ") + RESET);
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            if (hosting && state.returnToMenu) {
                server.stop();
                server = null;
                hosting = false;
                state.returnToMenu = false;
                System.out.println(CYAN + "Server stopped. Returning to main menu." + RESET);
                printMainHelp();
                continue;
            }

            if (!hosting) {
                switch (cmd) {
                    case "color":
                        System.out.println(CYAN + "Color output is " + (ConsoleStyle.isColorEnabled() ? "enabled" : "disabled")
                                + ". Use -Duno.ui.color=on|off|auto to configure." + RESET);
                        break;
                    case "h":
                    case "help":
                        printMainHelp();
                        break;

                    case "host": {
                        Integer parsedPort = null;
                        if (parts.length > 1) {
                            parsedPort = tryParsePort(parts[1]);
                            if (parsedPort == null) {
                                printPortError(parts[1]);
                                break;
                            }
                        }

                        int port = parsedPort != null
                                ? parsedPort
                                : promptForPort(scanner, "Port", currentPort);

                        state.resetForHosting();
                        server = createAndStartServer(hostName, port, state);
                        if (server == null) {
                            break;
                        }

                        currentPort = server.getPort();
                        hosting = true;
                        printServerOnline(server);
                        printHostHelp();
                        break;
                    }

                    case "q":
                    case "quit":
                    case "exit":
                        System.out.println(CYAN + "Goodbye!" + RESET);
                        return;

                    default:
                        System.out.println(RED + "Unknown command. Type 'help'." + RESET);
                        break;
                }
                continue;
            }

            switch (cmd) {
                case "h":
                case "help":
                    printHostHelp();
                    break;

                case "host":
                    System.out.println(YELLOW + "Already hosting. Use 'stop' first if you want to host again." + RESET);
                    break;

                case "ls":
                case "list":
                    printLobby(server.getSlots());
                    break;

                case "status":
                    printHostStatus(server, currentPort);
                    break;

                case "aipacing":
                    handleAiPacing(parts);
                    break;

                case "aihost":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot connect AI backend while a match is running." + RESET);
                        break;
                    }
                    handleAiHostConnect(parts, scanner, state);
                    break;

                case "aistatus":
                    printAiStatus(state);
                    break;

                case "aidisconnect":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot disconnect AI backend while a match is running." + RESET);
                        break;
                    }
                    closeAiConnection(state, "AI backend disconnected.");
                    break;

                case "ai":
                case "addai": {
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot change lobby while a match is running." + RESET);
                        break;
                    }
                    int count = parts.length > 1 ? parseIntOrDefault(parts[1], 1) : 1;
                    state.suppressLobbyPrint = true;
                    try {
                        for (int i = 0; i < Math.max(1, count); i++) {
                            server.addAiBot();
                            System.out.println(GREEN + "  + AI bot added" + RESET);
                        }
                    } finally {
                        state.suppressLobbyPrint = false;
                    }
                    printLobby(server.getSlots());
                    break;
                }

                case "rnd":
                case "addrandom": {
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot change lobby while a match is running." + RESET);
                        break;
                    }
                    int count = parts.length > 1 ? parseIntOrDefault(parts[1], 1) : 1;
                    state.suppressLobbyPrint = true;
                    try {
                        for (int i = 0; i < Math.max(1, count); i++) {
                            server.addRandomBot();
                            System.out.println(GREEN + "  + Random bot added" + RESET);
                        }
                    } finally {
                        state.suppressLobbyPrint = false;
                    }
                    printLobby(server.getSlots());
                    break;
                }

                case "rm":
                case "remove": {
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot change lobby while a match is running." + RESET);
                        break;
                    }
                    if (parts.length < 2) {
                        System.out.println(YELLOW + "Usage: remove <name>" + RESET);
                        continue;
                    }
                    String name = line.substring(line.indexOf(' ') + 1).trim();
                    if (!server.removeSlotByName(name)) {
                        System.out.println(RED + "✗ No slot found with name: " + name + RESET);
                    } else {
                        System.out.println(GREEN + "✓ Player removed: " + name + RESET);
                    }
                    break;
                }

                case "role": {
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot change lobby while a match is running." + RESET);
                        break;
                    }
                    if (parts.length < 3) {
                        System.out.println(YELLOW + "Usage: role <name> <player|spectator>" + RESET);
                        continue;
                    }
                    String name = parts[1];
                    String role = parts[2].toLowerCase();
                    if ("player".equals(role)) {
                        server.setPlayerRole(name, GameServer.SlotRole.PLAYER);
                        System.out.println(GREEN + "✓ " + name + " is now a PLAYER" + RESET);
                    } else if ("spectator".equals(role)) {
                        server.setPlayerRole(name, GameServer.SlotRole.SPECTATOR);
                        System.out.println(GREEN + "✓ " + name + " is now a SPECTATOR" + RESET);
                    } else {
                        System.out.println(RED + "✗ Role must be 'player' or 'spectator'." + RESET);
                    }
                    break;
                }

                case "rules":
                    printRules(rules);
                    break;

                case "rule":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Cannot change rules while a match is running." + RESET);
                        break;
                    }
                    if (parts.length < 3) {
                        System.out.println(YELLOW + "Usage: rule <1-8> <on|off>" + RESET);
                        continue;
                    }
                    int idx = parseIntOrDefault(parts[1], -1) - 1;
                    if (idx < 0 || idx >= RULE_COUNT) {
                        System.out.println(RED + "✗ Rule index must be 1-8." + RESET);
                        continue;
                    }
                    rules[idx] = "on".equalsIgnoreCase(parts[2]) || "true".equalsIgnoreCase(parts[2]);
                    System.out.println(GREEN + "✓ Rule " + (idx + 1) + " set to: " + (rules[idx] ? "ON" : "OFF") + RESET);
                    printRules(rules);
                    break;

                case "start":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "A match is already running." + RESET);
                        continue;
                    }
                    if (server.getAiCount() > 0 && state.aiConnection == null) {
                        System.out.println(RED + "✗ AI bots are in lobby but no AI backend is connected." + RESET);
                        System.out.println(YELLOW + "  Use: aihost <port>" + RESET);
                        continue;
                    }
                    if (server.getTotalPlayers() < 2) {
                        System.out.println(RED + "✗ Need at least 2 total players to start." + RESET);
                        continue;
                    }
                    System.out.println(GREEN + BOLD + "🎮 Starting game..." + RESET);
                    if (!server.startGame(state.aiConnection, rules, "game-" + System.currentTimeMillis(), -1)) {
                        System.out.println(RED + "✗ Could not start match. Check lobby player count and server state." + RESET);
                    }
                    break;

                case "stopmatch":
                    if (!server.isGameStarted()) {
                        System.out.println(YELLOW + "No active match to stop." + RESET);
                        break;
                    }
                    if (server.stopCurrentMatch(NetworkProtocol.R_MATCH_STOPPED_BY_HOST)) {
                        System.out.println(CYAN + "Current match stopped. Server remains online." + RESET);
                    } else {
                        System.out.println(RED + "✗ Could not stop current match." + RESET);
                    }
                    break;

                case "stop":
                    closeAiConnection(state, null);
                    server.stop();
                    server = null;
                    hosting = false;
                    System.out.println(CYAN + "Server stopped. Type 'host' to start hosting again." + RESET);
                    printMainHelp();
                    break;

                case "clear":
                    for (int i = 0; i < 30; i++) System.out.println();
                    break;

                case "q":
                case "quit":

                case "exit":
                    closeAiConnection(state, null);
                    server.stop();
                    System.out.println(CYAN + "Server stopped. Goodbye!" + RESET);
                    return;

                case "menu":
                    closeAiConnection(state, null);
                    server.stop();
                    server = null;
                    hosting = false;
                    System.out.println(CYAN + "Server stopped. Returning to main menu." + RESET);
                    printMainHelp();
                    break;

                default:
                    System.out.println(RED + "Unknown command. Type 'help'." + RESET);
                    break;
            }
        }
    }

    private static GameServer createAndStartServer(String hostName, int port, HostShellState state) {
        GameServer server = new GameServer(hostName);
        server.setHostSpectator(new PrintSpectator());

        server.setOnLobbyChanged(slots -> {
            if (!state.suppressLobbyPrint) {
                printLobby(slots);
            }
        });

        server.setOnMatchEnded(() -> {
            System.out.println(GREEN + BOLD + "✓ Match ended. You can start another one." + RESET);
            printHostHelp();
        });

        server.setOnPlayerDisconnected(playerName -> {
            System.out.println(RED + BOLD + "✗ Player '" + playerName + "' disconnected during the game. Returning to menu." + RESET);
            state.returnToMenu = true;
        });

        try {
            server.start(port);
        } catch (IOException e) {
            System.err.println(RED + "✗ Could not start server: " + e.getMessage() + RESET);
            return null;
        }

        return server;
    }

    private static void printMainHelp() {
        System.out.println(BOLD + CYAN + "\n═══ Host Main Commands ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "                         Show commands");
        System.out.println(GREEN + "  host [port]" + RESET + "                      Start hosting (prompts for port if omitted)");
        System.out.println(GREEN + "  color" + RESET + "                            Show color output status");
        System.out.println(GREEN + "  quit | q" + RESET + "                         Exit host app");
        System.out.println();
    }

    private static void printHostHelp() {
        System.out.println(BOLD + CYAN + "\n═══ Host Server Commands ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "                            Show commands");
        System.out.println(GREEN + "  status" + RESET + "                              Show server/match status");
        System.out.println(GREEN + "  list | ls" + RESET + "                           Print lobby slots");
        System.out.println(GREEN + "  addai [n] | ai [n]" + RESET + "                  Add AI bot(s)");
        System.out.println(GREEN + "  addrandom [n] | rnd [n]" + RESET + "             Add random bot(s)");
        System.out.println(GREEN + "  remove <name> | rm <name>" + RESET + "           Remove/kick player");
        System.out.println(GREEN + "  role <name> <player|spectator>" + RESET + "      Set player role");
        System.out.println(GREEN + "  rules" + RESET + "                               Show all rules");
        System.out.println(GREEN + "  rule <1-8> <on|off>" + RESET + "                 Toggle one rule");
        System.out.println(GREEN + "  aipacing [ms|off|default]" + RESET + "           Show/set AI decision delay");
        System.out.println(GREEN + "  aihost [port]" + RESET + "                       Wait for AI backend connection");
        System.out.println(GREEN + "  aistatus" + RESET + "                            Show AI backend status");
        System.out.println(GREEN + "  aidisconnect" + RESET + "                        Disconnect AI backend");
        System.out.println(GREEN + "  start" + RESET + "                               Start the game");
        System.out.println(GREEN + "  stopmatch" + RESET + "                           Stop current match only");
        System.out.println(GREEN + "  stop" + RESET + "                                Stop hosting and return to main menu");
        System.out.println(GREEN + "  clear" + RESET + "                               Clear screen");
        System.out.println(GREEN + "  quit | q" + RESET + "                            Stop server and exit");
        System.out.println();
    }

    private static void printHostStatus(GameServer server, int fallbackPort) {
        String running = server.isRunning() ? GREEN + "ONLINE" + RESET : RED + "OFFLINE" + RESET;
        String match = server.isGameStarted() ? YELLOW + "RUNNING" + RESET : GREEN + "IDLE" + RESET;
        int activePort = server.isRunning() ? server.getPort() : fallbackPort;
        System.out.println(CYAN + "Server: " + running + ", match: " + match + ", port: " + activePort + RESET);
        System.out.println(CYAN + "Lobby humans/total: " + server.getHumanCount() + "/" + server.getTotalPlayers() + RESET);
        System.out.println(CYAN + "Color output: " + (ConsoleStyle.isColorEnabled() ? "enabled" : "disabled") + RESET);
    }

    private static void handleAiPacing(String[] parts) {
        if (parts.length == 1) {
            System.out.println(CYAN + "AI pacing delay: " + getAiDelayMs() + " ms" + RESET);
            return;
        }

        String mode = parts[1].trim().toLowerCase();
        if ("off".equals(mode)) {
            System.setProperty("uno.ai.delay.ms", "0");
            System.out.println(GREEN + "✓ AI pacing disabled." + RESET);
            return;
        }

        if ("default".equals(mode)) {
            System.clearProperty("uno.ai.delay.ms");
            System.out.println(GREEN + "✓ AI pacing reset to default (2000 ms)." + RESET);
            return;
        }

        int ms = parseIntOrDefault(mode, -1);
        if (ms < 0) {
            System.out.println(RED + "✗ Usage: aipacing [ms|off|default]" + RESET);
            return;
        }

        System.setProperty("uno.ai.delay.ms", String.valueOf(ms));
        System.out.println(GREEN + "✓ AI pacing delay set to " + ms + " ms." + RESET);
    }

    private static int getAiDelayMs() {
        String raw = System.getProperty("uno.ai.delay.ms", "2000").trim();
        int parsed = parseIntOrDefault(raw, 2000);
        return Math.max(0, parsed);
    }

    private static void handleAiHostConnect(String[] parts, Scanner scanner, HostShellState state) {
        if (state.aiConnection != null) {
            System.out.println(YELLOW + "AI backend is already connected on port " + state.aiPort + "." + RESET);
            return;
        }

        int port;
        if (parts.length > 1) {
            Integer parsed = tryParsePort(parts[1]);
            if (parsed == null) {
                printPortError(parts[1]);
                return;
            }
            port = parsed;
        } else {
            port = promptForPort(scanner, "AI backend port", NetworkProtocol.DEFAULT_GAME_PORT + 1000);
        }

        ConnectionAI connectionAI = new ConnectionAI();
        System.out.println(CYAN + "Waiting for AI backend on port " + port + "..." + RESET);
        try {
            connectionAI.connect(port, false);
            state.aiConnection = connectionAI;
            state.aiPort = port;
            System.out.println(GREEN + "✓ AI backend connected on port " + port + RESET);
        } catch (IOException e) {
            System.out.println(RED + "✗ Could not connect AI backend: " + e.getMessage() + RESET);
        }
    }

    private static void printAiStatus(HostShellState state) {
        if (state.aiConnection == null) {
            System.out.println(YELLOW + "AI backend: disconnected" + RESET);
        } else {
            System.out.println(GREEN + "AI backend: connected on port " + state.aiPort + RESET);
        }
    }

    private static void closeAiConnection(HostShellState state, String successMessage) {
        if (state.aiConnection == null) {
            return;
        }

        try {
            state.aiConnection.close();
        } catch (Exception ignored) {
            // Ignore shutdown failures to keep host flow resilient.
        }

        state.aiConnection = null;
        state.aiPort = -1;

        if (successMessage != null && !successMessage.isEmpty()) {
            System.out.println(CYAN + successMessage + RESET);
        }
    }

    private static void printServerOnline(GameServer server) {
        System.out.println(GREEN + BOLD + "✓ Server online at:" + RESET);
        for (String desc : server.getLocalAddressDescriptions()) {
            int split = desc.lastIndexOf(" (");
            String ip = split > 0 ? desc.substring(0, split) : desc;
            String adapter = split > 0 && desc.endsWith(")")
                    ? desc.substring(split + 2, desc.length() - 1)
                    : "Unknown adapter";

            System.out.println(CYAN + "  " + ip + ":" + server.getPort() + RESET);
            System.out.println(CYAN + "    adapter: " + adapter + RESET);
        }
    }

    private static void printLobby(List<GameServer.LobbySlot> slots) {
        System.out.println(BOLD + CYAN + "Lobby (" + slots.size() + " players):" + RESET);
        if (slots.isEmpty()) {
            System.out.println(YELLOW + "  (empty)" + RESET);
            return;
        }
        for (GameServer.LobbySlot slot : slots) {
            String typeColor = "AI".equals(slot.type.toString()) ? BLUE :
                             "RANDOM".equals(slot.type.toString()) ? MAGENTA : GREEN;
            System.out.println("  - " + GREEN + slot.name + RESET + " [" + typeColor + slot.type + RESET + " / " + YELLOW + slot.role + RESET + "]");
        }
    }

    private static void printRules(boolean[] rules) {
        System.out.println(BOLD + CYAN + "Rules Configuration:" + RESET);

        for (int i = 0; i < RULE_COUNT; i++) {
            String status = rules[i] ? GREEN + "ON" + RESET : RED + "OFF" + RESET;
            System.out.println("  " + CYAN + (i + 1) + RESET + ") " + RULE_NAMES[i] + " = " + status);
        }
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
        System.out.println(RED + "✗ Invalid port: '" + raw + "'. Enter a number in range " + MIN_PORT + "-" + MAX_PORT + "." + RESET);
    }

    private static final class HostShellState {
        private boolean suppressLobbyPrint;
        private boolean returnToMenu;
        private ConnectionAI aiConnection;
        private int aiPort = -1;

        private void resetForHosting() {
            suppressLobbyPrint = false;
            returnToMenu = false;
        }
    }
}