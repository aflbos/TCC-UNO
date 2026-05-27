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
        System.out.println("\n" + BOLD + CYAN + "╔══════════════════════════════════╗" + RESET);
        System.out.println(BOLD + CYAN +        "║   UNO - Jogo de Cartas em Rede   ║" + RESET);
        System.out.println(BOLD + CYAN +        "║      Cliente do Jogador 1.0      ║" + RESET);
        System.out.println(BOLD + CYAN +        "╚══════════════════════════════════╝" + RESET + "\n");
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
                    System.out.println(CYAN + "\nConfiguracoes atuais:" + RESET);
                    System.out.println("  nome = " + (name.isEmpty() ? RED + "(nao definido)" + RESET : GREEN + name + RESET));
                    System.out.println("  host = " + GREEN + host + RESET);
                    System.out.println("  porta = " + GREEN + port + RESET + "\n");
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
                        System.out.println(GREEN + "✓ Nome definido como: " + name + RESET);
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
                        System.out.println(GREEN + "✓ Conectado! Aguardando no lobby..." + RESET);
                    } catch (IOException e) {
                        System.err.println(RED + "✗ Falha na conexao: " + e.getMessage() + RESET);
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
        System.out.println(BOLD + CYAN + "\n═══ Menu de Conexao ═══" + RESET);
        System.out.println(GREEN + "  help | h" + RESET + "              Mostrar comandos");
        System.out.println(GREEN + "  show" + RESET + "                  Mostrar nome/host/porta atuais");
        System.out.println(GREEN + "  setname <name>" + RESET + "        Definir nome do jogador");
        System.out.println(GREEN + "  prompt" + RESET + "                Perguntas interativas para todos os campos");
        System.out.println(GREEN + "  connect | c" + RESET + "           Conectar usando as configuracoes atuais");
        System.out.println(GREEN + "  connect <ip>:<porta>" + RESET + "   Definir destino e conectar imediatamente");
        System.out.println(GREEN + "  clear" + RESET + "                 Limpar tela");
        System.out.println(GREEN + "  quit | q" + RESET + "              Sair da aplicacao");
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
                    listener.requestDisconnect("Desconectado e retornando ao menu.");
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
        System.out.println(GREEN + "  help" + RESET + "          Mostrar esta lista de comandos");
        System.out.println(GREEN + "  players" + RESET + "       Mostrar resumo mais recente dos jogadores");
        System.out.println(GREEN + "  lobby" + RESET + "         Alias para players");
        System.out.println(GREEN + "  status" + RESET + "        Alias para players");
        System.out.println(GREEN + "  disconnect" + RESET + "    Sair do servidor e voltar ao menu de conexao");
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

            System.out.println(CYAN + "[lobby]" + RESET);

            for (String entry : summary.split(",")) {
                String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    System.out.println("  - " + GREEN + parts[0] + RESET + " [" + parts[1] + "]");
                }
            }
        }

        @Override
        public void onPlayers(String summary) {
            if (summary != null && !summary.isEmpty()) {
                lastPlayersSummary = summary;
                System.out.println(CYAN + "[jogadores] " + summary + RESET);
            }
        }

        @Override
        public void onGameStart() {
            System.out.println(GREEN + BOLD + "🎮 Partida iniciada!" + RESET);
        }

        @Override
        public void onState(String handDesc, String topCard, int[] validInputs, String decisionId, String prompt, int[] playableCardIds) {
            awaitingDecision = true;
            System.out.println();
            System.out.println(BOLD + YELLOW + "┌─ Estado do Jogo ─┐" + RESET);
            System.out.println(MAGENTA + "Carta do topo: " + RESET + BOLD + topCard + RESET);
            printHand(handDesc);
            System.out.println(CYAN + prompt + RESET);
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
                            System.out.println(MAGENTA + "Carta do topo: " + RESET + BOLD + topCard + RESET);
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
            if (NetworkProtocol.R_MATCH_STOPPED_BY_HOST.equals(winnerName)) {
                System.out.println(BOLD + YELLOW + "⚠ Partida encerrada pelo host." + RESET);
            } else {
                System.out.println(BOLD + GREEN + "✓ Fim de jogo. Vencedor: " + winnerName + RESET);
            }
            System.out.println(CYAN + "Permanecendo conectado no lobby. Aguardando a proxima partida..." + RESET);
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
                System.out.println(CYAN + "Jogadores: " + lastPlayersSummary + RESET);
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
                System.out.println(YELLOW + "Mao: (vazia)" + RESET);
                return;
            }

            System.out.println(YELLOW + "Mao:" + RESET);

            String[] cards = handDesc.split(",");

            for (int i = 0; i < cards.length; i++) {
                String[] fields = cards[i].split(":", 2);
                String card = fields.length > 1 ? fields[1] : cards[i];
                System.out.println("  " + GREEN + i + RESET + ") " + card);
            }
        }

        private static void printPlayerHelp() {
            System.out.println(BOLD + CYAN + "\n═══ Comandos Disponiveis ═══" + RESET);
            System.out.println(GREEN + "  help" + RESET + "                Mostrar lista de comandos");
            System.out.println(GREEN + "  state" + RESET + "               Reimprimir carta do topo e prompt");
            System.out.println(GREEN + "  hand" + RESET + "                Reimprimir sua mao");
            System.out.println(GREEN + "  players" + RESET + "             Mostrar resumo dos jogadores");
            System.out.println(GREEN + "  actions" + RESET + "             Mostrar opcoes rotuladas");
            System.out.println(GREEN + "  choose <action-id>" + RESET + "  Enviar uma acao");
            System.out.println(GREEN + "  <number>" + RESET + "            Atalho para choose <number>");
            System.out.println(GREEN + "  disconnect" + RESET + "          Sair e voltar ao menu");
            System.out.println();
        }

        private static void printDecisionLabels(String handDesc, int[] validInputs, int[] playableCardIds, String decisionId, String promptText) {
            List<CardEntry> cards = parseHand(handDesc);
            System.out.println(BOLD + CYAN + "Opcoes:" + RESET);

            for (int action : validInputs) {
                String description = describeAction(action, cards, playableCardIds, decisionId, promptText);
                System.out.println("  " + BOLD + GREEN + action + RESET + " => " + description);
            }
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
            if (action < 0 || action >= playableCardIds.length) {
                return null;
            }

            int cardId = playableCardIds[action];
            String cardLabel = findCardLabelById(cards, cardId);

            if (cardLabel != null) {
                return "Jogar carta #" + GREEN + action + RESET + " (" + cardLabel + ")";
            }

            return "Jogar carta #" + GREEN + action + RESET;
        }

        private static String describePlayableCardFallback(int action, List<CardEntry> cards, int[] playableCardIds) {
            for (int i = 0; i < playableCardIds.length; i++) {
                if (playableCardIds[i] != action) continue;

                if (i >= 0 && i < cards.size()) {
                    return "Jogar carta #" + GREEN + i + RESET + " (" + cards.get(i).label + ")";
                }

                return "Jogar carta";
            }

            for (int i = 0; i < cards.size(); i++) {
                CardEntry card = cards.get(i);
                if (card.cardId == action) {
                    return "Jogar carta #" + GREEN + i + RESET + " (" + card.label + ")";
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
}