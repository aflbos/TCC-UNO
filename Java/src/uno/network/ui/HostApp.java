package uno.network.ui;

import uno.ai.network.ConnectionAI;
import uno.network.GameServer;
import uno.network.NetworkProtocol;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class HostApp {
    private static final int RULE_COUNT = 8;
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    private static String RESET = ConsoleStyle.RESET;
    private static String BOLD = ConsoleStyle.BOLD;
    private static String CYAN = ConsoleStyle.CYAN;
    private static String GREEN = ConsoleStyle.GREEN;
    private static String YELLOW = ConsoleStyle.YELLOW;
    private static String RED = ConsoleStyle.RED;
    private static String BLUE = ConsoleStyle.BLUE;
    private static String MAGENTA = ConsoleStyle.MAGENTA;

    private static final String[] RULE_NAMES = {
            "Empilhamento (+2/+4)",
            "Pula e Inverte (Skip 'n Flip)",
            "Sete / Zero",
            "Obrigar jogar carta comprada",
            "Comprar e jogar apos penalidades",
            "Comprar ate combinar",
            "Desafio de blefe +4",
            "Jogar cartas identicas"
    };

    private static final java.util.Map<String, boolean[]> RULE_PRESETS = buildRulePresets();

    private static java.util.Map<String, boolean[]> buildRulePresets() {
        java.util.Map<String, boolean[]> presets = new java.util.LinkedHashMap<>();
        presets.put("official", new boolean[]{
                false, false, false, false, false, false, true, false
        });
        presets.put("house rules", new boolean[]{
                true, false, true, false, false, true, false, true
        });
        return java.util.Collections.unmodifiableMap(presets);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        refreshColors();
        printWelcome();
        runShell(scanner);
    }

    private static void printWelcome() {
        System.out.println("\n" + BOLD + MAGENTA + "==========================================================" + RESET);
        System.out.println(BOLD + MAGENTA +        "|               UNO - Jogo de Cartas em Rede             |" + RESET);
        System.out.println(BOLD + MAGENTA +        "|                     Servidor Host 1.0                  |" + RESET);
        System.out.println(BOLD + MAGENTA +        "==========================================================" + RESET + "\n");
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
                System.out.println(CYAN + "Servidor encerrado. Voltando ao menu principal." + RESET);
                printMainHelp();
                continue;
            }

            if (!hosting) {
                switch (cmd) {
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
                                : promptForPort(scanner, "Porta", currentPort);

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
                        System.out.println(CYAN + "Ate mais!" + RESET);
                        return;

                    case "color":
                    case "colors":
                        handleColorCommand(parts);
                        break;

                    default:
                        System.out.println(RED + "Comando desconhecido. Digite 'help'." + RESET);
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
                    System.out.println(YELLOW + "Ja hospedando. Use 'stop' antes de hospedar novamente." + RESET);
                    break;

                case "ls":
                case "list":
                    printLobby(server.getSlots());
                    break;

                case "status":
                    printHostStatus(server, currentPort);
                    break;

                case "aihost":
                    System.out.println(YELLOW + "Use 'addai' e informe a porta de cada bot de IA." + RESET);
                    break;

                case "aistatus":
                    System.out.println(YELLOW + "Use 'list' para ver os bots de IA no lobby." + RESET);
                    break;

                case "aidisconnect":
                    System.out.println(YELLOW + "Use 'remove <nome>' para remover um bot de IA." + RESET);
                    break;

                case "ai":
                case "addai": {
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Nao e possivel alterar o lobby durante uma partida." + RESET);
                        break;
                    }
                    int count = parts.length > 1 ? parseIntOrDefault(parts[1], 1) : 1;
                    state.suppressLobbyPrint = true;
                    try {
                        for (int i = 0; i < Math.max(1, count); i++) {
                            ConnectionAI aiConnection = promptAndConnectAi(scanner, state);
                            if (aiConnection == null) {
                                System.out.println(RED + "[ERRO] Bot de IA nao adicionado (falha na conexao)." + RESET);
                                continue;
                            }
                            server.addAiBot(aiConnection, state.lastAiPort);
                            System.out.println(GREEN + "  + Bot de IA adicionado (porta " + state.lastAiPort + ")" + RESET);
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
                        System.out.println(YELLOW + "Nao e possivel alterar o lobby durante uma partida." + RESET);
                        break;
                    }
                    int count = parts.length > 1 ? parseIntOrDefault(parts[1], 1) : 1;
                    state.suppressLobbyPrint = true;
                    try {
                        for (int i = 0; i < Math.max(1, count); i++) {
                            server.addRandomBot();
                            System.out.println(GREEN + "  + Bot aleatorio adicionado" + RESET);
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
                        System.out.println(YELLOW + "Nao e possivel alterar o lobby durante uma partida." + RESET);
                        break;
                    }
                    if (parts.length < 2) {
                        System.out.println(YELLOW + "Uso: remove <nome>" + RESET);
                        continue;
                    }
                    String name = line.substring(line.indexOf(' ') + 1).trim();
                    if (!server.removeSlotByName(name)) {
                        System.out.println(RED + "[ERRO] Nenhum slot encontrado com o nome: " + name + RESET);
                    } else {
                        System.out.println(GREEN + "[OK] Jogador removido: " + name + RESET);
                    }
                    break;
                }

                case "rules":
                    printRules(rules);
                    break;

                case "rule":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Nao e possivel alterar regras durante uma partida." + RESET);
                        break;
                    }
                    if (parts.length < 3) {
                        System.out.println(YELLOW + "Uso: rule <1-8> <on|off>" + RESET);
                        continue;
                    }
                    int idx = parseIntOrDefault(parts[1], -1) - 1;
                    if (idx < 0 || idx >= RULE_COUNT) {
                        System.out.println(RED + "[ERRO] O indice da regra deve ser 1-8." + RESET);
                        continue;
                    }
                    rules[idx] = "on".equalsIgnoreCase(parts[2]) || "true".equalsIgnoreCase(parts[2]);
                    System.out.println(GREEN + "[OK] Regra " + (idx + 1) + " definida como: " + (rules[idx] ? "ON" : "OFF") + RESET);
                    printRules(rules);
                    break;

                case "preset":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Nao e possivel alterar regras durante uma partida." + RESET);
                        break;
                    }
                    if (parts.length < 2) {
                        System.out.println(YELLOW + "Uso: preset <nome>" + RESET);
                        printPresets();
                        continue;
                    }
                    String presetName = line.substring(line.indexOf(' ') + 1).trim();
                    applyPreset(presetName, rules);
                    break;

                case "presets":
                    printPresets();
                    break;

                case "start":
                    if (server.isGameStarted()) {
                        System.out.println(YELLOW + "Uma partida ja esta em andamento." + RESET);
                        continue;
                    }
                    if (server.hasMissingAiBackend()) {
                        System.out.println(RED + "[ERRO] Ha bots de IA no lobby sem backend conectado." + RESET);
                        System.out.println(YELLOW + "       Remova o bot ou conecte um backend ao adiciona-lo." + RESET);
                        continue;
                    }
                    if (server.getTotalPlayers() < 2) {
                        System.out.println(RED + "[ERRO] Sao necessarios pelo menos 2 jogadores para iniciar." + RESET);
                        continue;
                    }
                    System.out.println(GREEN + BOLD + "[JOGO] Iniciando partida..." + RESET);
                    if (!server.startGame(rules, "game-" + System.currentTimeMillis(), -1)) {
                        System.out.println(RED + "[ERRO] Nao foi possivel iniciar a partida. Verifique o lobby e o estado do servidor." + RESET);
                    }
                    break;

                case "stopmatch":
                    if (!server.isGameStarted()) {
                        System.out.println(YELLOW + "Nao ha partida ativa para parar." + RESET);
                        break;
                    }
                    if (server.stopCurrentMatch(NetworkProtocol.R_MATCH_STOPPED_BY_HOST)) {
                        System.out.println(CYAN + "Partida atual encerrada. O servidor permanece online." + RESET);
                    } else {
                        System.out.println(RED + "[ERRO] Nao foi possivel encerrar a partida atual." + RESET);
                    }
                    break;

                case "stop":
                    server.stop();
                    server = null;
                    hosting = false;
                    System.out.println(CYAN + "Servidor encerrado. Digite 'host' para hospedar novamente." + RESET);
                    printMainHelp();
                    break;

                case "clear":
                    for (int i = 0; i < 30; i++) System.out.println();
                    break;

                case "color":
                case "colors":
                    handleColorCommand(parts);
                    break;

                case "q":
                case "quit":
                case "exit":
                    server.stop();
                    System.out.println(CYAN + "Servidor encerrado. Ate mais!" + RESET);
                    return;

                case "menu":
                    server.stop();
                    server = null;
                    hosting = false;
                    System.out.println(CYAN + "Servidor encerrado. Voltando ao menu principal." + RESET);
                    printMainHelp();
                    break;

                default:
                    System.out.println(RED + "Comando desconhecido. Digite 'help'." + RESET);
                    break;
            }
        }
    }

    private static GameServer createAndStartServer(String hostName, int port, HostShellState state) {
        GameServer server = new GameServer(hostName);
        server.setOnLobbyChanged(slots -> {
            if (!state.suppressLobbyPrint) {
                printLobby(slots);
            }
        });

        server.setOnMatchEnded(() -> {
            System.out.println(GREEN + BOLD + "[OK] Partida encerrada. Voce pode iniciar outra." + RESET);
            printHostHelp();
        });

        server.setOnPlayerDisconnected(playerName -> {
            System.out.println(RED + BOLD + "[ERRO] Jogador '" + playerName + "' desconectou durante a partida. Voltando ao menu." + RESET);
            state.returnToMenu = true;
        });

        try {
            server.start(port);
        } catch (IOException e) {
            System.err.println(RED + "[ERRO] Nao foi possivel iniciar o servidor: " + e.getMessage() + RESET);
            return null;
        }

        return server;
    }

    private static void printMainHelp() {
        System.out.println(BOLD + CYAN + "\n=== Comandos Principais do Host ===" + RESET);
        System.out.println(GREEN + ConsoleStyle.padRight("  help | h", 25) + RESET + "Mostrar comandos");
        System.out.println(GREEN + ConsoleStyle.padRight("  host [porta]", 25) + RESET + "Iniciar hospedagem (pede porta se omitida)");
        System.out.println(GREEN + ConsoleStyle.padRight("  color [auto|on|off]", 25) + RESET + "Ajustar cores do console");
        System.out.println(GREEN + ConsoleStyle.padRight("  quit | q", 25) + RESET + "Sair do host");
        System.out.println();
    }

    private static void printHostHelp() {
        System.out.println(BOLD + CYAN + "\n=== Comandos do Servidor Host ===" + RESET);
        System.out.println(GREEN + ConsoleStyle.padRight("  help | h", 30) + RESET + "Mostrar comandos");
        System.out.println(GREEN + ConsoleStyle.padRight("  status", 30) + RESET + "Mostrar status do servidor/partida");
        System.out.println(GREEN + ConsoleStyle.padRight("  list | ls", 30) + RESET + "Listar slots do lobby");
        System.out.println(GREEN + ConsoleStyle.padRight("  addai [n] | ai [n]", 30) + RESET + "Adicionar bot(s) de IA");
        System.out.println(GREEN + ConsoleStyle.padRight("  addrandom [n] | rnd [n]", 30) + RESET + "Adicionar bot(s) aleatorios");
        System.out.println(GREEN + ConsoleStyle.padRight("  remove <name> | rm", 30) + RESET + "Remover jogador");
        System.out.println(GREEN + ConsoleStyle.padRight("  rules", 30) + RESET + "Mostrar todas as regras");
        System.out.println(GREEN + ConsoleStyle.padRight("  rule <1-8> <on|off>", 30) + RESET + "Alternar uma regra");
        System.out.println(GREEN + ConsoleStyle.padRight("  preset <nome>", 30) + RESET + "Aplicar preset de regras");
        System.out.println(GREEN + ConsoleStyle.padRight("  presets", 30) + RESET + "Listar presets disponiveis");
        System.out.println(GREEN + ConsoleStyle.padRight("  start", 30) + RESET + "Iniciar partida");
        System.out.println(GREEN + ConsoleStyle.padRight("  stopmatch", 30) + RESET + "Encerrar apenas a partida atual");
        System.out.println(GREEN + ConsoleStyle.padRight("  stop", 30) + RESET + "Parar hospedagem e voltar ao menu");
        System.out.println(GREEN + ConsoleStyle.padRight("  clear", 30) + RESET + "Limpar tela");
        System.out.println(GREEN + ConsoleStyle.padRight("  color [auto|on|off]", 30) + RESET + "Ajustar cores do console");
        System.out.println(GREEN + ConsoleStyle.padRight("  quit | q", 30) + RESET + "Encerrar servidor e sair");
        System.out.println();
    }

    private static void printHostStatus(GameServer server, int fallbackPort) {
        String running = server.isRunning() ? GREEN + "ONLINE" + RESET : RED + "OFFLINE" + RESET;
        String match = server.isGameStarted() ? YELLOW + "EM ANDAMENTO" + RESET : GREEN + "OCIOSO" + RESET;
        int activePort = server.isRunning() ? server.getPort() : fallbackPort;

        System.out.println(BOLD + CYAN + "\n=== Status do Servidor ===" + RESET);
        System.out.println("  " + ConsoleStyle.padRight("Servidor", 10) + ": " + running);
        System.out.println("  " + ConsoleStyle.padRight("Partida", 10) + ": " + match);
        System.out.println("  " + ConsoleStyle.padRight("Porta", 10) + ": " + GREEN + activePort + RESET);
        System.out.println("  " + ConsoleStyle.padRight("Lobby", 10) + ": " + GREEN + server.getHumanCount() + " humanos / " + server.getTotalPlayers() + " total" + RESET + "\n");
    }

    private static ConnectionAI promptAndConnectAi(Scanner scanner, HostShellState state) {
        int fallbackPort = state.lastAiPort > 0
                ? state.lastAiPort
                : NetworkProtocol.DEFAULT_GAME_PORT + 1000;
        int port = promptForPort(scanner, "Porta do backend de IA", fallbackPort);

        ConnectionAI connectionAI = new ConnectionAI();
        System.out.println(CYAN + "Aguardando backend de IA na porta " + port + "..." + RESET);
        try {
            connectionAI.connect(port, false);
            state.lastAiPort = port;
            System.out.println(GREEN + "[OK] Backend de IA conectado na porta " + port + RESET);
            return connectionAI;
        } catch (IOException e) {
            System.out.println(RED + "[ERRO] Nao foi possivel conectar o backend de IA: " + e.getMessage() + RESET);
            return null;
        }
    }

    private static void printServerOnline(GameServer server) {
        System.out.println(BOLD + GREEN + "\n==========================================================" + RESET);
        System.out.println(BOLD + GREEN + "                   SERVIDOR ONLINE                        " + RESET);
        System.out.println(BOLD + GREEN + "==========================================================" + RESET);

        for (String desc : server.getLocalAddressDescriptions()) {
            int split = desc.lastIndexOf(" (");
            String ip = split > 0 ? desc.substring(0, split) : desc;
            String adapter = split > 0 && desc.endsWith(")")
                    ? desc.substring(split + 2, desc.length() - 1)
                    : "Adaptador desconhecido";

            String line = ip + ":" + server.getPort() + " (" + adapter + ")";
            System.out.println(BOLD + GREEN + "| " + RESET + ConsoleStyle.padRight(line, 54) + BOLD + GREEN + " |" + RESET);
        }
        System.out.println(BOLD + GREEN + "==========================================================" + RESET);
    }

    private static void printLobby(List<GameServer.LobbySlot> slots) {
        System.out.println(BOLD + CYAN + "\n=== Lobby (" + slots.size() + " jogadores) ===" + RESET);
        if (slots.isEmpty()) {
            System.out.println(YELLOW + "  (vazio)" + RESET);
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            GameServer.LobbySlot slot = slots.get(i);
            String typeColor = "AI".equals(slot.type.toString()) ? BLUE :
                    "RANDOM".equals(slot.type.toString()) ? MAGENTA : GREEN;
            System.out.println("  " + CYAN + (i + 1) + "." + RESET + " " + GREEN + ConsoleStyle.padRight(slot.name, 20) + RESET + " [" + typeColor + slot.type + RESET + "]");
        }
        System.out.println();
    }

    private static void printRules(boolean[] rules) {
        System.out.println(BOLD + CYAN + "\n=== Configuracao de Regras ===" + RESET);
        for (int i = 0; i < RULE_COUNT; i++) {
            String status = rules[i] ? GREEN + "ON" + RESET : RED + "OFF" + RESET;
            System.out.println("  " + CYAN + (i + 1) + RESET + ") " + ConsoleStyle.padRight(RULE_NAMES[i], 35) + " = " + status);
        }
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

    private static void applyPreset(String presetName, boolean[] rules) {
        if (presetName == null) {
            System.out.println(RED + "[ERRO] Preset desconhecido." + RESET);
            return;
        }
        boolean[] preset = RULE_PRESETS.get(presetName.toLowerCase());
        if (preset == null) {
            System.out.println(RED + "[ERRO] Preset desconhecido: " + presetName + RESET);
            printPresets();
            return;
        }
        System.arraycopy(preset, 0, rules, 0, RULE_COUNT);
        System.out.println(GREEN + "[OK] Preset aplicado: " + presetName.toUpperCase() + RESET);
        printRules(rules);
    }

    private static void printPresets() {
        System.out.println(BOLD + CYAN + "Presets disponiveis:" + RESET);
        for (String name : RULE_PRESETS.keySet()) {
            System.out.println("  - " + GREEN + name + RESET);
        }
    }

    private static void printPortError(String raw) {
        System.out.println(RED + "[ERRO] Porta invalida: '" + raw + "'. Digite um numero no intervalo " + MIN_PORT + "-" + MAX_PORT + "." + RESET);
    }

    private static void handleColorCommand(String[] parts) {
        if (parts.length < 2) {
            printColorStatus();
            System.out.println("  Uso: color <auto|on|off>");
            return;
        }

        ConsoleStyle.ColorMode mode = ConsoleStyle.parseColorMode(parts[1]);
        if (mode == null) {
            System.out.println(RED + "Modo de cor invalido. Use auto, on ou off." + RESET);
            return;
        }

        ConsoleStyle.setColorMode(mode);
        refreshColors();
        printColorStatus();
    }

    private static void printColorStatus() {
        String modeLabel = ConsoleStyle.getColorMode().name().toLowerCase();
        String enabled = ConsoleStyle.isColorsEnabled() ? GREEN + "ON" + RESET : RED + "OFF" + RESET;
        String support = ConsoleStyle.isAutoSupported() ? GREEN + "sim" + RESET : RED + "nao" + RESET;
        System.out.println(BOLD + CYAN + "\n=== Cores do Console ===" + RESET);
        System.out.println("  Modo atual : " + modeLabel);
        System.out.println("  Ativo      : " + enabled);
        System.out.println("  Auto detect: " + support + "\n");
    }

    private static void refreshColors() {
        RESET = ConsoleStyle.RESET;
        BOLD = ConsoleStyle.BOLD;
        CYAN = ConsoleStyle.CYAN;
        GREEN = ConsoleStyle.GREEN;
        YELLOW = ConsoleStyle.YELLOW;
        RED = ConsoleStyle.RED;
        BLUE = ConsoleStyle.BLUE;
        MAGENTA = ConsoleStyle.MAGENTA;
    }

    private static final class HostShellState {
        private boolean suppressLobbyPrint;
        private boolean returnToMenu;
        private int lastAiPort = -1;

        private void resetForHosting() {
            suppressLobbyPrint = false;
            returnToMenu = false;
        }
    }
}