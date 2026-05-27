package uno.network;

import uno.ai.network.ConnectionAI;
import uno.game.engine.Simulation;
import uno.game.players.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GameServer {
    public enum SlotType { HUMAN, AI, RANDOM }
    public static final class LobbySlot {
        public final String name;
        public final SlotType type;
        public final ClientHandler handler;
        public final ConnectionAI aiConnection;
        public final int aiPort;

        LobbySlot(String name, SlotType type, ClientHandler handler) {
            this(name, type, handler, null, -1);
        }

        LobbySlot(String name, SlotType type, ClientHandler handler, ConnectionAI aiConnection, int aiPort) {
            this.name = name;
            this.type = type;
            this.handler = handler;
            this.aiConnection = aiConnection;
            this.aiPort = aiPort;
        }

        @Override public String toString() {
            return name + " [" + type + "]";
        }
    }

    private ServerSocket serverSocket;
    private int port;
    private final String hostName;
    private int aiCount = 0;
    private int randomCount = 0;
    private int maxPlayers = 8;
    private final Random botNameRng = new Random();

    private volatile boolean gameStarted = false;
    private volatile Thread gameThread;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final List<LobbySlot> slots = new CopyOnWriteArrayList<>();
    private ExecutorService acceptPool;

    private Consumer<List<LobbySlot>> onLobbyChanged = ignored -> {};
    private Runnable onMatchEnded = () -> {};
    private Consumer<String> onPlayerDisconnected = ignored -> {};

    public GameServer(String hostName) {
        this.hostName = hostName;
    }

    public void start(int port) throws IOException {
        if (isRunning()) {
            throw new IOException("Servidor ja esta em execucao na porta " + this.port + ".");
        }

        serverSocket = new ServerSocket(port);
        this.port = serverSocket.getLocalPort();


        acceptPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "server-accept");
            t.setDaemon(true);
            return t;
        });
        acceptPool.submit(this::acceptLoop);
    }

    public void stop() {
        stopCurrentMatch("Partida encerrada pelo host.");
        gameStarted = false;
        if (acceptPool  != null) acceptPool.shutdownNow();
        for (ClientHandler ch : clients) ch.closeOnDisconnect();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}

        for (LobbySlot slot : slots) {
            if (slot.type == SlotType.AI && slot.aiConnection != null) {
                try {
                    slot.aiConnection.close();
                } catch (Exception ignored) {
                    // Best-effort shutdown.
                }
            }
        }

        synchronized (this) {
            aiCount = 0;
            randomCount = 0;
            slots.clear();
            clients.clear();
            gameThread = null;
        }
        fireOnLobbyChanged();
    }

    public boolean isRunning() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean stopCurrentMatch(String reason) {
        if (!gameStarted) {
            return false;
        }

        Thread runningGameThread = gameThread;
        gameStarted = false;

        if (runningGameThread != null) {
            runningGameThread.interrupt();
        }

        String safeReason = (reason == null || reason.trim().isEmpty())
                ? "Partida encerrada pelo host." : reason.trim();

        broadcastHostNotice(NetworkProtocol.N_MATCH_STOPPED);

        for (ClientHandler ch : clients) {
            try {
                ch.connection.sendGameOver(safeReason);
            } catch (Exception ignored) {}
        }

        synchronized (this) {
            notifyAll();
        }

        return true;
    }

    public void setAiCount(int n) {
        aiCount = Math.max(0, n);
        rebuildSlots();
    }

    public void setRandomCount(int n) {
        randomCount = Math.max(0, n);
        rebuildSlots();
    }

    public void addAiBot() {
        addAiBot(null, -1);
    }

    public void addAiBot(ConnectionAI aiConnection, int aiPort) {
        aiCount++;
        String name = generateBotName("AI ");
        slots.add(new LobbySlot(name, SlotType.AI, null, aiConnection, aiPort));
        broadcastLobbyUpdate();
        fireOnLobbyChanged();
    }

    public void addRandomBot() {
        randomCount++;
        rebuildSlots();
    }

    public void setMaxPlayers(int max) {
        maxPlayers = Math.max(1, max);
    }

    public void setOnLobbyChanged(Consumer<List<LobbySlot>> callback) {
        this.onLobbyChanged = callback;
    }

    
    public void setOnMatchEnded(Runnable callback) {
        this.onMatchEnded = callback != null ? callback : () -> {};
    }

    
    public void setOnPlayerDisconnected(Consumer<String> callback) {
        this.onPlayerDisconnected = callback != null ? callback : ignored -> {};
    }

    public List<LobbySlot> getSlots()  { return Collections.unmodifiableList(slots); }
    public int  getPort()              { return port; }
    public int  getHumanCount()        { return clients.size(); }
    public int  getAiCount()           { return aiCount; }
    public int  getRandomCount()       { return randomCount; }
    public int  getTotalPlayers()      { return clients.size() + aiCount + randomCount; }

    public boolean kickHuman(String name) {
        if (name == null) return false;
        for (LobbySlot s : slots) {
            if (s.type == SlotType.HUMAN && s.name.equals(name) && s.handler != null) {
                s.handler.closeOnDisconnect();
                return true;
            }
        }
        return false;
    }

    public boolean removeSlotByName(String name) {
        if (name == null) return false;
        for (LobbySlot s : slots) {
            if (!s.name.equals(name)) continue;

            if (s.type == SlotType.HUMAN) {
                if (s.handler != null) s.handler.closeOnDisconnect();
                return true;
            }

            if (s.type == SlotType.AI) {
                if (aiCount > 0) aiCount--;
                slots.remove(s);
                if (s.aiConnection != null) {
                    try {
                        s.aiConnection.close();
                    } catch (Exception ignored) {
                        // Best-effort shutdown.
                    }
                }
                broadcastLobbyUpdate();
                fireOnLobbyChanged();
                return true;
            }

            if (s.type == SlotType.RANDOM) {
                if (randomCount > 0) randomCount--;
                slots.remove(s);
                broadcastLobbyUpdate();
                fireOnLobbyChanged();
                return true;
            }
        }
        return false;
    }

    public boolean hasMissingAiBackend() {
        for (LobbySlot slot : slots) {
            if (slot.type == SlotType.AI && slot.aiConnection == null) {
                return true;
            }
        }
        return false;
    }

    public List<String> getLocalAddresses() {
        List<String> addrs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> ia = iface.getInetAddresses();
                while (ia.hasMoreElements()) {
                    InetAddress a = ia.nextElement();
                    if (a instanceof Inet4Address) addrs.add(a.getHostAddress());
                }
            }
        } catch (SocketException ignored) {}
        if (addrs.isEmpty()) addrs.add("127.0.0.1");
        return addrs;
    }

    public List<String> getLocalAddressDescriptions() {
        List<String> addrs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                String adapterName = iface.getDisplayName();
                Enumeration<InetAddress> ia = iface.getInetAddresses();
                while (ia.hasMoreElements()) {
                    InetAddress a = ia.nextElement();
                    if (a instanceof Inet4Address) {
                        addrs.add(a.getHostAddress() + " (" + adapterName + ")");
                    }
                }
            }
        } catch (SocketException ignored) {}
        if (addrs.isEmpty()) addrs.add("127.0.0.1 (Loopback)");
        return addrs;
    }

    public boolean startGame(boolean[] rules,
                             String gameId, int seed) {
        if (gameStarted) return false;

        List<Player> playerList = new ArrayList<>();

        for (LobbySlot slot : slots) {
            switch (slot.type) {
                case HUMAN:
                    if (slot.handler != null) {
                        PlayerNetwork pn = new PlayerNetwork(slot.name, null, slot.handler.connection);
                        playerList.add(pn);
                    }
                    break;
                case AI:
                    if (slot.aiConnection == null) {
                        return false;
                    }
                    playerList.add(new PlayerAI(slot.name, null, slot.aiConnection));
                    break;
                case RANDOM:
                    playerList.add(new PlayerRandom(slot.name, null));
                    break;
            }
        }

        Player[] players = playerList.toArray(new Player[0]);
        if (players.length < 2) {
            return false;
        }

        Simulation sim = new Simulation(players, null, rules, gameId, seed);
        for (Player p : players) p.setSimulation(sim);

        for (ClientHandler ch : clients) {
            ch.connection.sendGameStart();
        }
        broadcastHostNotice(NetworkProtocol.N_MATCH_STARTED);

        gameStarted = true;
        synchronized (this) { notifyAll(); }

        gameThread = new Thread(() -> {
            try {
                sim.startGame();
                while (gameStarted && !sim.isGameOver()) {
                    sim.playTurn();
                }
            } catch (RuntimeException e) {
                if (Thread.currentThread().isInterrupted()) {
                    // Expected when host stops the current match.
                } else if (e.getMessage() != null && e.getMessage().contains("disconnected")) {
                    onPlayerDisconnected.accept(e.getMessage());
                } else {
                    System.err.println("[GameServer] erro no loop do jogo: " + e.getMessage());
                }
            } finally {
                synchronized (GameServer.this) {
                    gameStarted = false;
                    gameThread = null;
                    GameServer.this.notifyAll();
                }

                if (sim.isGameOver()) {
                    Player winner = sim.getWinner();
                    if (winner != null) {
                        broadcastHostNotice(String.format(NetworkProtocol.N_MATCH_FINISHED_WINNER, winner.getName()));
                    } else {
                        broadcastHostNotice(NetworkProtocol.N_MATCH_FINISHED_DRAW);
                    }
                } else if (!Thread.currentThread().isInterrupted()) {
                    broadcastHostNotice(NetworkProtocol.N_MATCH_ENDED_EARLY);
                }

                try {
                    broadcastLobbyUpdate();
                } catch (Exception ignored) {}
                onMatchEnded.run();
            }
        }, "game-loop");
        gameThread.setDaemon(true);
        gameThread.start();

        return true;
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                Thread t = new Thread(handler, "client-" + socket.getRemoteSocketAddress());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[GameServer] erro ao aceitar conexao: " + e.getMessage());
                }
                break;
            }
        }
    }

    private void rebuildSlots() {
        List<LobbySlot> newSlots = new ArrayList<>();
        for (LobbySlot s : slots) {
            if (s.type == SlotType.HUMAN) newSlots.add(s);
        }

        Set<String> reserved = new HashSet<>();
        for (LobbySlot s : newSlots) reserved.add(s.name);
        for (LobbySlot s : slots) reserved.add(s.name);

        int keepAi = 0, keepRandom = 0;
        for (LobbySlot s : slots) {
            if (s.type == SlotType.AI && keepAi < aiCount) { newSlots.add(s); keepAi++; }
            if (s.type == SlotType.RANDOM && keepRandom < randomCount) { newSlots.add(s); keepRandom++; }
        }

        for (int i = keepAi; i < aiCount; i++) {
            String name = BotNameLibrary.generate(botNameRng, reserved, "AI ");
            reserved.add(name);
            newSlots.add(new LobbySlot(name, SlotType.AI, null));
        }
        for (int i = keepRandom; i < randomCount; i++) {
            String name = BotNameLibrary.generate(botNameRng, reserved, "R ");
            reserved.add(name);
            newSlots.add(new LobbySlot(name, SlotType.RANDOM, null));
        }

        slots.clear();
        slots.addAll(newSlots);
        broadcastLobbyUpdate();
        fireOnLobbyChanged();
    }

    private String generateBotName(String prefix) {
        Set<String> reserved = new HashSet<>();
        for (LobbySlot s : slots) {
            reserved.add(s.name);
        }
        return BotNameLibrary.generate(botNameRng, reserved, prefix);
    }

    private void broadcastLobbyUpdate() {
        String summary = buildLobbyUpdateString();
        for (ClientHandler ch : clients) {
            ch.connection.sendLobbyUpdate(summary);
        }
    }

    private String buildLobbyUpdateString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) sb.append(',');
            LobbySlot s = slots.get(i);
            sb.append(s.name).append(':').append(s.type);
        }
        return sb.toString();
    }

    private void fireOnLobbyChanged() {
        onLobbyChanged.accept(getSlots());
    }

    private void broadcastHostNotice(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        String prefix = hostName == null || hostName.trim().isEmpty()
                ? "[host] " : "[host " + hostName.trim() + "] ";

        for (ClientHandler ch : clients) {
            try {
                ch.connection.sendNotification(prefix + message);
            } catch (Exception ignored) {}
        }
    }

    final class ClientHandler implements Runnable {

        final Socket socket;
        final PlayerConnection connection;
        LobbySlot slot;

        private volatile boolean forceClose = false;

        ClientHandler(Socket socket) throws IOException {
            this.socket     = socket;
            this.connection = new PlayerConnection(socket);
        }

        @Override
        public void run() {
            try {
                
                connection.readName();
                String name = connection.getPlayerName();

                synchronized (GameServer.this) {
                    if (slots.size() >= maxPlayers) {
                        connection.sendNotification("Lobby lotado.");
                        closeOnDisconnect();
                        clients.remove(this);
                        return;
                    }

                    for (LobbySlot existing : slots) {
                        if (existing.name.equalsIgnoreCase(name)) {
                            connection.sendNotification("Nome ja esta em uso. Escolha outro nome.");
                            closeOnDisconnect();
                            clients.remove(this);
                            return;
                        }
                    }

                    slot = new LobbySlot(name, SlotType.HUMAN, this);
                    slots.add(slot);
                }

                broadcastLobbyUpdate();
                fireOnLobbyChanged();

                while (!gameStarted && !forceClose) {
                    try {
                        if (connection.pollLobbyDisconnectOrLeave()) {
                            forceClose = true;
                            break;
                        }
                    } catch (IOException e) {
                        forceClose = true;
                        break;
                    }
                    synchronized (GameServer.this) {
                        if (gameStarted || forceClose) {
                            break;
                        }
                        try {
                            GameServer.this.wait(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            forceClose = true;
                            break;
                        }
                    }
                }

                if (forceClose) {
                    removeSlot();
                }

            } catch (IOException e) {
                
                System.err.println("[GameServer] erro do cliente ("
                        + (slot != null ? slot.name : "unknown") + "): " + e.getMessage());
                removeSlot();
            } finally {
                connection.clearReadTimeout();
            }
        }

        void closeOnDisconnect() {
            forceClose = true;
            connection.close();
            synchronized (GameServer.this) { GameServer.this.notifyAll(); }
        }

        private void removeSlot() {
            clients.remove(this);
            if (slot != null) {
                slots.remove(slot);
                slot = null;
            }
            broadcastLobbyUpdate();
            fireOnLobbyChanged();
        }
    }
}