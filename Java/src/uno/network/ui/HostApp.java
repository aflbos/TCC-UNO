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

    
    private static final String RESET = ConsoleStyle.RESET;
    private static final String BOLD = ConsoleStyle.BOLD;
    private static final String CYAN = ConsoleStyle.CYAN;
    private static final String GREEN = ConsoleStyle.GREEN;
    private static final String YELLOW = ConsoleStyle.YELLOW;
    private static final String RED = ConsoleStyle.RED;
    private static final String BLUE = ConsoleStyle.BLUE;
    private static final String MAGENTA = ConsoleStyle.MAGENTA;

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
        return java.util.Collections.unmodifiableMap(presets);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        printWelcome();
        runShell(scanner);
    }

    private static void printWelcome() {
        System.out.println("\n" + BOLD + MAGENTA + "╔══════════════════════════════════╗" + RESET);
        System.out.println(BOLD + MAGENTA +        "║   UNO - Jogo de Cartas em Rede   ║" + RESET);
        System.out.println(BOLD + MAGENTA +        "║         Servidor Host 1.0        ║" + RESET);
        System.out.println(BOLD + MAGENTA +        "╚══════════════════════════════════╝" + RESET + "\n");
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
                                System.out.println(RED + "✗ Bot de IA nao adicionado (falha na conexao)." + RESET);
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
                        System.out.println(RED + "✗ Nenhum slot encontrado com o nome: " + name + RESET);
                    } else {
                        System.out.println(GREEN + "✓ Jogador removido: " + name + RESET);
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
                        System.out.println(RED + "✗ O indice da regra deve ser 1-8." + RESET);
                        continue;
                    }
                    rules[idx] = "on".equalsIgnoreCase(parts[2]) || "true".equalsIgnoreCase(parts[2]);
                    System.out.println(GREEN + "✓ Regra " + (idx + 1) + " definida como: " + (rules[idx] ? "ON" : "OFF") + RESET);
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
                    applyPreset(parts[1], rules);
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
                        System.out.println(RED + "✗ Ha bots de IA no lobby sem backend conectado." + RESET);
                        System.out.println(YELLOW + "  Remova o bot ou conecte um backend ao adiciona-lo." + RESET);
                        continue;
                    }
                    if (server.getTotalPlayers() < 2) {
                        System.out.println(RED + "✗ Sao necessarios pelo menos 2 jogadores para iniciar." + RESET);
                        continue;
                    }
                    System.out.println(GREEN + BOLD + "🎮 Iniciando partida..." + RESET);
                    if (!server.startGame(rules, "game-" + System.currentTimeMillis(), -1)) {
                        System.out.println(RED + "✗ Nao foi possivel iniciar a partida. Verifique o lobby e o estado do servidor." + RESET);
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
                        System.out.println(RED + "✗ Nao foi possivel encerrar a partida atual." + RESET);
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
            System.out.println(GREEN + BOLD + "✓ Partida encerrada. Voce pode iniciar outra." + RESET);
            printHostHelp();
        });

        server.setOnPlayerDisconnected(playerName -> {
            System.out.println(RED + BOLD + "✗ Jogador '" + playerName + "' desconectou durante a partida. Voltando ao menu." + RESET);
            state.returnToMenu = true;
        });

        try {
            server.start(port);
        } catch (IOException e) {
            System.err.println(RED + "✗ Nao foi possivel iniciar o servidor: " + e.getMessage() + RESET);
            return null;
        }

        return server;
    }

    private static void printMainHelp() {
        System.out.println(BOLD + CYAN + "\n═══ Comandos Principais do Host ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "                         Mostrar comandos");
        System.out.println(GREEN + "  host [porta]" + RESET + "                      Iniciar hospedagem (pede porta se omitida)");
        System.out.println(GREEN + "  quit | q" + RESET + "                         Sair do host");
        System.out.println();
    }

    private static void printHostHelp() {
        System.out.println(BOLD + CYAN + "\n═══ Comandos do Servidor Host ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "                            Mostrar comandos");
        System.out.println(GREEN + "  status" + RESET + "                              Mostrar status do servidor/partida");
        System.out.println(GREEN + "  list | ls" + RESET + "                           Listar slots do lobby");
        System.out.println(GREEN + "  addai [n] | ai [n]" + RESET + "                  Adicionar bot(s) de IA (pede porta)");
        System.out.println(GREEN + "  addrandom [n] | rnd [n]" + RESET + "             Adicionar bot(s) aleatorios");
        System.out.println(GREEN + "  remove <name> | rm <name>" + RESET + "           Remover/expulsar jogador");
        System.out.println(GREEN + "  rules" + RESET + "                               Mostrar todas as regras");
        System.out.println(GREEN + "  rule <1-8> <on|off>" + RESET + "                 Alternar uma regra");
        System.out.println(GREEN + "  preset <nome>" + RESET + "                      Aplicar preset de regras");
        System.out.println(GREEN + "  presets" + RESET + "                            Listar presets disponiveis");
        System.out.println(GREEN + "  start" + RESET + "                               Iniciar partida");
        System.out.println(GREEN + "  stopmatch" + RESET + "                           Encerrar apenas a partida atual");
        System.out.println(GREEN + "  stop" + RESET + "                                Parar hospedagem e voltar ao menu principal");
        System.out.println(GREEN + "  clear" + RESET + "                               Limpar tela");
        System.out.println(GREEN + "  quit | q" + RESET + "                            Encerrar servidor e sair");
        System.out.println();
    }

    private static void printHostStatus(GameServer server, int fallbackPort) {
        String running = server.isRunning() ? GREEN + "ONLINE" + RESET : RED + "OFFLINE" + RESET;
        String match = server.isGameStarted() ? YELLOW + "EM ANDAMENTO" + RESET : GREEN + "OCIOSO" + RESET;
        int activePort = server.isRunning() ? server.getPort() : fallbackPort;
        System.out.println(CYAN + "Servidor: " + running + ", partida: " + match + ", porta: " + activePort + RESET);
        System.out.println(CYAN + "Lobby humanos/total: " + server.getHumanCount() + "/" + server.getTotalPlayers() + RESET);
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
            System.out.println(GREEN + "✓ Backend de IA conectado na porta " + port + RESET);
            return connectionAI;
        } catch (IOException e) {
            System.out.println(RED + "✗ Nao foi possivel conectar o backend de IA: " + e.getMessage() + RESET);
            return null;
        }
    }

    private static void printServerOnline(GameServer server) {
        System.out.println(GREEN + BOLD + "✓ Servidor online em:" + RESET);
        for (String desc : server.getLocalAddressDescriptions()) {
            int split = desc.lastIndexOf(" (");
            String ip = split > 0 ? desc.substring(0, split) : desc;
            String adapter = split > 0 && desc.endsWith(")")
                    ? desc.substring(split + 2, desc.length() - 1)
                    : "Adaptador desconhecido";

            System.out.println(CYAN + "  " + ip + ":" + server.getPort() + RESET);
            System.out.println(CYAN + "    adaptador: " + adapter + RESET);
        }
    }

    private static void printLobby(List<GameServer.LobbySlot> slots) {
        System.out.println(BOLD + CYAN + "Lobby (" + slots.size() + " jogadores):" + RESET);
        if (slots.isEmpty()) {
            System.out.println(YELLOW + "  (vazio)" + RESET);
            return;
        }
        for (GameServer.LobbySlot slot : slots) {
            String typeColor = "AI".equals(slot.type.toString()) ? BLUE :
                             "RANDOM".equals(slot.type.toString()) ? MAGENTA : GREEN;
            System.out.println("  - " + GREEN + slot.name + RESET + " [" + typeColor + slot.type + RESET + "]");
        }
    }

    private static void printRules(boolean[] rules) {
        System.out.println(BOLD + CYAN + "Configuracao de Regras:" + RESET);

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

    private static void applyPreset(String presetName, boolean[] rules) {
        if (presetName == null) {
            System.out.println(RED + "✗ Preset desconhecido." + RESET);
            return;
        }
        boolean[] preset = RULE_PRESETS.get(presetName.toLowerCase());
        if (preset == null) {
            System.out.println(RED + "✗ Preset desconhecido: " + presetName + RESET);
            printPresets();
            return;
        }
        System.arraycopy(preset, 0, rules, 0, RULE_COUNT);
        System.out.println(GREEN + "✓ Preset aplicado: " + presetName.toUpperCase() + RESET);
        printRules(rules);
    }

    private static void printPresets() {
        System.out.println(BOLD + CYAN + "Presets disponiveis:" + RESET);
        for (String name : RULE_PRESETS.keySet()) {
            System.out.println("  - " + GREEN + name + RESET);
        }
    }

    private static void printPortError(String raw) {
        System.out.println(RED + "✗ Porta invalida: '" + raw + "'. Digite um numero no intervalo " + MIN_PORT + "-" + MAX_PORT + "." + RESET);
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