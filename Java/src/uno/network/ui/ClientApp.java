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

    private static String RESET = ConsoleStyle.RESET;
    private static String BOLD = ConsoleStyle.BOLD;
    private static String CYAN = ConsoleStyle.CYAN;
    private static String GREEN = ConsoleStyle.GREEN;
    private static String YELLOW = ConsoleStyle.YELLOW;
    private static String RED = ConsoleStyle.RED;
    private static String BLUE = ConsoleStyle.BLUE;
    private static String MAGENTA = ConsoleStyle.MAGENTA;
    private static final Object CONSOLE_LOCK = new Object();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        refreshColors();
        printWelcome();
        runClient(scanner);
    }

    private static void printWelcome() {
        System.out.println("\n" + BOLD + CYAN + "==========================================================" + RESET);
        System.out.println(BOLD + CYAN +        "|               UNO - Jogo de Cartas em Rede             |" + RESET);
        System.out.println(BOLD + CYAN +        "|                        Cliente 1.0                     |" + RESET);
        System.out.println(BOLD + CYAN +        "==========================================================" + RESET + "\n");
    }

    private static void runClient(Scanner scanner) {
        String name = "";
        String host = "127.0.0.1";
        int port = NetworkProtocol.DEFAULT_GAME_PORT;

        printConnectHelp();
        while (true) {
            String line = prompt(scanner, BOLD + "jogador> " + RESET);
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "h":
                case "help":
                    printConnectHelp();
                    continue;

                case "clear":
                    for (int i = 0; i < 30; i++) System.out.println();
                    continue;

                case "show":
                    System.out.println(BOLD + CYAN + "\n=== Configuracoes Atuais ===" + RESET);
                    System.out.println("  " + ConsoleStyle.padRight("Nome", 10) + ": " + (name.isEmpty() ? RED + "(nao definido)" + RESET : GREEN + name + RESET));
                    System.out.println("  " + ConsoleStyle.padRight("Host", 10) + ": " + GREEN + host + RESET);
                    System.out.println("  " + ConsoleStyle.padRight("Porta", 10) + ": " + GREEN + port + RESET + "\n");
                    continue;

                case "color":
                case "colors":
                    handleColorCommand(parts);
                    continue;

                case "setname":
                    if (parts.length < 2) {
                        System.out.println(YELLOW + "Uso: setname <nome-do-jogador>" + RESET);
                        continue;
                    }

                    name = line.substring(line.indexOf(' ') + 1).trim();

                    if (name.isEmpty()) {
                        System.out.println(RED + "O nome nao pode ser vazio." + RESET);
                    } else {
                        System.out.println(GREEN + "[OK] Nome definido como: " + name + RESET);
                    }
                    continue;

                case "prompt": {
                    String shownName = name.isEmpty() ? "nao definido" : name;
                    String promptedName = prompt(scanner, "Seu nome [" + shownName + "]: ");

                    if (!promptedName.isEmpty()) {
                        name = promptedName;
                    }

                    host = prompt(scanner, "IP do servidor [" + host + "]: ");
                    if (host.isEmpty()) host = "127.0.0.1";
                    port = promptForPort(scanner, "Porta", port);
                    continue;
                }

                case "c":
                case "connect":
                    String connectTarget = parts.length >= 2 ? parts[1] : "";
                    if (!connectTarget.isEmpty()) {
                        String[] hp = connectTarget.split(":", 2);

                        if (hp.length != 2 || hp[0].trim().isEmpty()) {
                            System.out.println(YELLOW + "Uso: connect <ip>:<porta>" + RESET);
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
                        String promptedName = prompt(scanner, "Defina seu nome primeiro: ");

                        if (promptedName.trim().isEmpty()) {
                            System.out.println(RED + "Nome obrigatorio. Use `setname <nome>`." + RESET);
                            continue;
                        }

                        name = promptedName.trim();
                    }

                    if (host.trim().isEmpty()) {
                        System.out.println(RED + "Host nao pode ser vazio. Use `sethost` primeiro." + RESET);
                        continue;
                    }

                    GameClient client = new GameClient(name);
                    client.setDisconnectAfterGameOver(false);
                    CliClientListener sessionListener = new CliClientListener(client, scanner);
                    client.setListener(sessionListener);

                    try {
                        System.out.println(CYAN + "Conectando em " + host + ":" + port + "..." + RESET);
                        client.connect(host, port);
                        client.startReadLoop();
                        System.out.println(GREEN + "[OK] Conectado! Aguardando no lobby..." + RESET);
                    } catch (IOException e) {
                        System.err.println(RED + "[ERRO] Falha na conexao: " + e.getMessage() + RESET);
                        continue;
                    }

                    runConnectedLobbyLoop(scanner, sessionListener);

                    System.out.println(CYAN + "De volta ao menu de conexao." + RESET);

                    continue;
                case "quit":
                case "q":
                case "exit":
                case "menu":
                    System.out.println(CYAN + "Ate mais!" + RESET + "\n");
                    return;
            }

            System.out.println(RED + "Comando desconhecido. Digite `help`." + RESET);
        }
    }

    private static void printConnectHelp() {
        System.out.println(BOLD + CYAN + "\n=== Menu de Conexao ===" + RESET);
        System.out.println(GREEN + ConsoleStyle.padRight("  help | h", 28) + RESET + "Mostrar comandos");
        System.out.println(GREEN + ConsoleStyle.padRight("  show", 28) + RESET + "Mostrar configuracoes atuais");
        System.out.println(GREEN + ConsoleStyle.padRight("  setname <nome>", 28) + RESET + "Definir nome do jogador");
        System.out.println(GREEN + ConsoleStyle.padRight("  prompt", 28) + RESET + "Configurar de forma interativa");
        System.out.println(GREEN + ConsoleStyle.padRight("  connect | c", 28) + RESET + "Conectar usando configuracoes atuais");
        System.out.println(GREEN + ConsoleStyle.padRight("  connect <ip>:<porta>", 28) + RESET + "Conectar imediatamente");
        System.out.println(GREEN + ConsoleStyle.padRight("  clear", 28) + RESET + "Limpar tela");
        System.out.println(GREEN + ConsoleStyle.padRight("  color [auto|on|off]", 28) + RESET + "Ajustar cores do console");
        System.out.println(GREEN + ConsoleStyle.padRight("  quit | q", 28) + RESET + "Sair da aplicacao");
        System.out.println();
    }

    private static void runConnectedLobbyLoop(Scanner scanner, CliClientListener listener) {
        System.out.println(CYAN + "Modo lobby conectado: digite 'help' para comandos do lobby." + RESET);

        while (listener.isSessionActive()) {
            if (listener.isAwaitingDecision()) {
                continue;
            }

            String line = tryReadConsoleLine(scanner, BOLD + "lobby> " + RESET, listener);
            if (line == null) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String cmd = parts[0].trim().toLowerCase();
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
                    listener.requestDisconnect("Desconectado e retornando ao menu.");
                    break;
                case "color":
                case "colors":
                    handleColorCommand(parts);
                    break;
                default:
                    System.out.println(YELLOW + "Comando de lobby desconhecido. Digite 'help'." + RESET);
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


    private static void printLobbyHelp() {
        System.out.println(BOLD + CYAN + "\n=== Comandos do Lobby ===" + RESET);
        System.out.println(GREEN + ConsoleStyle.padRight("  help", 20) + RESET + "Mostrar esta lista de comandos");
        System.out.println(GREEN + ConsoleStyle.padRight("  players | lobby", 20) + RESET + "Mostrar resumo mais recente dos jogadores");
        System.out.println(GREEN + ConsoleStyle.padRight("  disconnect", 20) + RESET + "Sair do servidor e voltar ao menu de conexao");
        System.out.println(GREEN + ConsoleStyle.padRight("  color [auto|on|off]", 20) + RESET + "Ajustar cores do console");
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
        System.out.println(RED + "Porta invalida: '" + raw + "'. Digite um numero no intervalo " + MIN_PORT + "-" + MAX_PORT + "." + RESET);
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
            if (!message.isEmpty()) System.out.println(BLUE + "[aviso] " + message + RESET);
        }

        @Override
        public void onLobbyUpdate(String summary) {
            if (summary == null || summary.isEmpty()) {
                System.out.println(CYAN + "[lobby] (vazio)" + RESET);
                return;
            }

            System.out.println(BOLD + CYAN + "\n=== Atualizacao do Lobby ===" + RESET);

            for (String entry : summary.split(",")) {
                String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    System.out.println("  - " + GREEN + ConsoleStyle.padRight(parts[0], 20) + RESET + " [" + parts[1] + "]");
                }
            }
            System.out.println();
        }

        @Override
        public void onPlayers(String summary) {
            if (summary != null && !summary.isEmpty()) {
                lastPlayersSummary = summary;
                System.out.println("\n" + BOLD + CYAN + "=== Resumo dos Jogadores ===" + RESET);
                for (String entry : summary.split(",")) {
                    System.out.println("  - " + entry.trim());
                }
                System.out.println();
            }
        }

        @Override
        public void onGameStart() {
            System.out.println(GREEN + BOLD + "[JOGO] Partida iniciada!" + RESET);
        }

        @Override
        public void onState(String handDesc, String topCard, int[] validInputs, String decisionId, String prompt, int[] playableCardIds) {
            awaitingDecision = true;
            System.out.println();
            System.out.println(BOLD + YELLOW + "==========================================================" + RESET);
            System.out.println(BOLD + YELLOW + "                      ESTADO DO JOGO                      " + RESET);
            System.out.println(BOLD + YELLOW + "==========================================================" + RESET);
            System.out.println(" " + MAGENTA + "> Carta do topo:" + RESET + " " + BOLD + topCard + RESET);
            System.out.println();
            printHand(handDesc);
            System.out.println();
            System.out.println(" " + CYAN + "> " + prompt + RESET);
            System.out.println();
            printDecisionLabels(handDesc, validInputs, playableCardIds, decisionId, prompt);

            try {
                while (true) {
                    String raw;
                    synchronized (CONSOLE_LOCK) {
                        raw = prompt(scanner, BOLD + "jogador> " + RESET);
                    }
                    if (raw.isEmpty()) continue;

                    String[] parts = raw.split("\\s+");
                    String cmd = parts[0].toLowerCase();

                    switch (cmd) {
                        case "help":
                            printPlayerHelp();
                            continue;

                        case "state":
                            System.out.println(" " + MAGENTA + "> Carta do topo:" + RESET + " " + BOLD + topCard + RESET);
                            System.out.println(" " + CYAN + "> " + prompt + RESET);
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

                        case "color":
                        case "colors":
                            handleColorCommand(parts);
                            continue;

                        case "disconnect":
                        case "menu":
                            requestDisconnect("Desconectado e retornando ao menu.");
                            return;
                    }

                    int action;

                    if ("choose".equals(cmd)) {
                        if (parts.length < 2) {
                            System.out.println(YELLOW + "Uso: choose <id-da-acao>" + RESET);
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

                    System.out.println(RED + "Acao invalida. Use `actions` para ver as opcoes validas." + RESET);
                }
            } finally {
                awaitingDecision = false;
            }
        }

        @Override
        public void onGameOver(String winnerName) {
            System.out.println();
            System.out.println(BOLD + YELLOW + "==========================================================" + RESET);
            System.out.println(BOLD + YELLOW + "                       FIM DE JOGO                        " + RESET);
            System.out.println(BOLD + YELLOW + "----------------------------------------------------------" + RESET);

            if (NetworkProtocol.R_MATCH_STOPPED_BY_HOST.equals(winnerName)) {
                System.out.println(BOLD + YELLOW + "| " + RED + ConsoleStyle.padRight("Partida encerrada pelo host.", 54) + YELLOW + " |" + RESET);
            } else {
                System.out.println(BOLD + YELLOW + "| " + GREEN + ConsoleStyle.padRight("Vencedor: " + winnerName, 54) + YELLOW + " |" + RESET);
            }

            System.out.println(BOLD + YELLOW + "==========================================================" + RESET);
            System.out.println(CYAN + " Retornando ao lobby. Aguardando proxima partida..." + RESET + "\n");
        }

        @Override
        public void onDisconnect(String reason) {
            System.out.println(RED + "Desconectado: " + reason + RESET);
            shutdownClient("Conexao encerrada.");
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
                System.out.println(YELLOW + "Jogadores: (sem atualizacao ainda)" + RESET);
            } else {
                System.out.println(BOLD + CYAN + "\n=== Resumo dos Jogadores ===" + RESET);
                for (String entry : lastPlayersSummary.split(",")) {
                    System.out.println("  - " + entry.trim());
                }
                System.out.println();
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
                System.out.println(" " + YELLOW + "> Sua mao:" + RESET + " (vazia)");
                return;
            }

            System.out.println(" " + YELLOW + "> Sua mao:" + RESET);

            String[] cards = handDesc.split(",");
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < cards.length; i++) {
                String[] fields = cards[i].split(":", 2);
                String card = fields.length > 1 ? fields[1] : cards[i];

                String entry = "   - " + card;
                sb.append(ConsoleStyle.padRight(entry, 25));

                if (i % 3 == 2 || i == cards.length - 1) {
                    System.out.println(sb.toString());
                    sb.setLength(0);
                }
            }
        }

        private static void printPlayerHelp() {
            System.out.println(BOLD + CYAN + "\n=== Comandos Durante a Partida ===" + RESET);
            System.out.println(GREEN + ConsoleStyle.padRight("  help", 25) + RESET + "Mostrar lista de comandos");
            System.out.println(GREEN + ConsoleStyle.padRight("  state", 25) + RESET + "Reimprimir estado do jogo (carta e opcoes)");
            System.out.println(GREEN + ConsoleStyle.padRight("  hand", 25) + RESET + "Reimprimir sua mao");
            System.out.println(GREEN + ConsoleStyle.padRight("  players", 25) + RESET + "Mostrar resumo dos jogadores");
            System.out.println(GREEN + ConsoleStyle.padRight("  actions", 25) + RESET + "Mostrar opcoes rotuladas");
            System.out.println(GREEN + ConsoleStyle.padRight("  choose <id>", 25) + RESET + "Enviar uma acao (ex: choose 1)");
            System.out.println(GREEN + ConsoleStyle.padRight("  <numero>", 25) + RESET + "Atalho direto para enviar a acao");
            System.out.println(GREEN + ConsoleStyle.padRight("  color [auto|on|off]", 25) + RESET + "Ajustar cores do console");
            System.out.println(GREEN + ConsoleStyle.padRight("  disconnect", 25) + RESET + "Sair da partida e voltar ao menu");
            System.out.println();
        }

        private static void printDecisionLabels(String handDesc, int[] validInputs, int[] playableCardIds, String decisionId, String promptText) {
            List<CardEntry> cards = parseHand(handDesc);
            System.out.println(" " + BOLD + CYAN + "> Opcoes de Acao:" + RESET);

            for (int action : validInputs) {
                String description = describeAction(action, cards, playableCardIds, decisionId, promptText);
                System.out.println("    " + BOLD + GREEN + "[" + action + "]" + RESET + " " + description);
            }
            System.out.println();
        }

        private static String describeAction(int action, List<CardEntry> cards, int[] playableCardIds, String decisionId, String promptText) {
            String id = decisionId == null || decisionId.isEmpty() ? NetworkProtocol.D_UNKNOWN : decisionId;

            if (NetworkProtocol.D_CHOOSE_COLOR.equals(id)) {
                if (action == 1 || action == NetworkProtocol.MASK_COLOR_RED) {
                    return RED + "Escolher a cor VERMELHO" + RESET;
                }

                if (action == 2 || action == NetworkProtocol.MASK_COLOR_GREEN) {
                    return GREEN + "Escolher a cor VERDE" + RESET;
                }

                if (action == 3 || action == NetworkProtocol.MASK_COLOR_BLUE) {
                    return BLUE + "Escolher a cor AZUL" + RESET;
                }

                if (action == 4 || action == NetworkProtocol.MASK_COLOR_YELLOW) {
                    return YELLOW + "Escolher a cor AMARELO" + RESET;
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
                return "Trocar mao com o jogador #" + action;
            }

            if (NetworkProtocol.D_UNKNOWN.equals(id) && action == 1) {
                return GREEN + "Sim / Opcao 1" + RESET;
            }

            if (NetworkProtocol.D_UNKNOWN.equals(id) && action == 2) {
                return RED + "Nao / Opcao 2" + RESET;
            }

            return "Acao";
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
            int idx = action - 1;
            if (idx < 0 || idx >= playableCardIds.length) {
                return null;
            }

            int cardId = playableCardIds[idx];
            String cardLabel = findCardLabelById(cards, cardId);

            if (cardLabel != null) {
                return "Jogar carta " + BOLD + MAGENTA + cardLabel + RESET;
            }

            return "Jogar carta";
        }

        private static String describePlayableCardFallback(int action, List<CardEntry> cards, int[] playableCardIds) {
            for (int i = 0; i < playableCardIds.length; i++) {
                if (playableCardIds[i] != action) continue;

                if (i >= 0 && i < cards.size()) {
                    return "Jogar carta " + BOLD + MAGENTA + cards.get(i).label + RESET;
                }

                return "Jogar carta";
            }

            for (int i = 0; i < cards.size(); i++) {
                CardEntry card = cards.get(i);
                if (card.cardId == action) {
                    return "Jogar carta " + BOLD + MAGENTA + card.label + RESET;
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
                return yes ? GREEN + "Jogar uma carta" + RESET : RED + "Comprar uma carta" + RESET;
            }

            if (NetworkProtocol.D_PLAY_WHEN_CANNOT_DRAW.equals(id)) {
                return yes ? GREEN + "Jogar uma carta" + RESET : RED + "Nao jogar" + RESET;
            }

            if (NetworkProtocol.D_PLAY_DRAWN_CARD.equals(id)) {
                return yes ? GREEN + "Jogar a carta comprada" + RESET : RED + "Ficar com ela" + RESET;
            }

            if (NetworkProtocol.D_STACK_OR_DRAW.equals(id)) {
                return yes ? GREEN + "Empilhar uma carta" + RESET : RED + "Comprar cartas de penalidade" + RESET;
            }

            if (NetworkProtocol.D_PLAY_IDENTICAL.equals(id)) {
                return yes ? GREEN + "Jogar outra carta identica" + RESET
                        : RED + "Nao jogar outra" + RESET;
            }

            if (NetworkProtocol.D_CHALLENGE_DRAW_FOUR.equals(id)) {
                return yes ? GREEN + "Desafiar" + RESET : RED + "Nao desafiar" + RESET;
            }

            String prompt = (promptText == null || promptText.isEmpty()) ? "(sem prompt)" : promptText;
            System.err.println("[DEBUG] ID de decisao binaria nao mapeado: " + id + " | prompt: \"" + prompt + "\"");
            return yes ? GREEN + "Sim" + RESET : RED + "Nao" + RESET;
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
}
