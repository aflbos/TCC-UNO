package uno.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

public class GameClient {

    private final String playerName;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile GameClientListener listener;

    private final LinkedBlockingQueue<Integer> actionQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;
    private volatile boolean disconnectAfterGameOver = false;

    public GameClient(String playerName) {
        this.playerName = playerName;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);

        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);

        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        out.println(NetworkProtocol.encode(NetworkProtocol.C_NAME, playerName));
    }

    public void setListener(GameClientListener listener) {
        this.listener = listener;
    }

    public void setDisconnectAfterGameOver(boolean disconnectAfterGameOver) {
        this.disconnectAfterGameOver = disconnectAfterGameOver;
    }

    public void submitAction(int action) {
        try {
            actionQueue.put(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startReadLoop() {
        running = true;
        Thread t = new Thread(this::readLoop, "game-client-reader");
        t.setDaemon(true);
        t.start();
    }

    private void readLoop() {
        try {
            while (running) {
                String line = in.readLine();
                if (line == null) break;
                dispatch(line);
            }
        } catch (IOException e) {
            if (running) {
                handleDisconnect("Lost connection to server: " + e.getMessage());
            }
        }

        if (running) {
            handleDisconnect("The server closed the connection.");
        }
    }

    private void dispatch(String line) throws IOException {
        String[] parts = NetworkProtocol.decode(line);
        String type = parts[0];
        GameClientListener l = listener;

        switch (type) {

            case NetworkProtocol.S_NOTIFY:
                if (l != null) l.onNotify(parts.length > 1 ? parts[1] : "");
                break;

            case NetworkProtocol.S_STATE:
                if (l != null && parts.length >= 5) {
                    String handDesc = parts[1];
                    String topCard = parts[2];
                    int[] validInputs = NetworkProtocol.decodeInts(parts[3]);

                    String decisionId;
                    String prompt;
                    int playableIdx;

                    if (parts.length >= 7) {
                        decisionId = parts[4];
                        prompt = parts[5];
                        playableIdx = 6;
                    } else {
                        decisionId = NetworkProtocol.D_UNKNOWN;
                        prompt = parts[4];
                        playableIdx = 5;
                    }

                    int[] playableIds = parts.length > playableIdx
                            ? NetworkProtocol.decodeInts(parts[playableIdx]) : new int[0];

                    l.onState(handDesc, topCard, validInputs, decisionId, prompt, playableIds);

                    if (!running) {
                        return;
                    }

                    int action;

                    try {
                        action = actionQueue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    out.println(NetworkProtocol.encode(NetworkProtocol.C_ACTION, String.valueOf(action)));
                }
                break;

            case NetworkProtocol.S_PLAYERS:
                
                if (l != null && parts.length >= 2) l.onPlayers(parts[1]);
                break;

            case NetworkProtocol.S_LOBBY_UPDATE:
                if (l != null && parts.length >= 2) l.onLobbyUpdate(parts[1]);
                break;

            case NetworkProtocol.S_GAME_START:
                if (l != null) l.onGameStart();
                break;

            case NetworkProtocol.S_GAME_OVER:
                String winner = parts.length > 1 ? parts[1] : "DRAW";
                if (l != null) l.onGameOver(winner);
                if (disconnectAfterGameOver) {
                    running = false;
                }
                break;

            default:
                
                break;
        }
    }

    private void handleDisconnect(String reason) {
        running = false;
        GameClientListener l = listener;
        if (l != null) l.onDisconnect(reason);
    }

    public void disconnect() {
        running = false;
        try {
            if (out != null) {
                out.println(NetworkProtocol.C_LEAVE_LOBBY);
                out.flush();
            }
        } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public String getPlayerName() { return playerName; }
}
