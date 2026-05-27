package uno.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class PlayerConnection {
    private static final int MAX_NAME_LENGTH = 32;
    private static final int ACTION_READ_TIMEOUT_MS = 750;

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    
    private String playerName = "Unknown";

    public PlayerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), "UTF-8")), true);
        this.in  = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
    }

    public void readName() throws IOException {
        String line = in.readLine();
        if (line == null) throw new IOException("Client disconnected before sending name.");
        String[] parts = NetworkProtocol.decode(line);
        if (parts.length < 2 || !NetworkProtocol.C_NAME.equals(parts[0])) {
            throw new IOException("Expected NAME command from client.");
        }

        String candidate = parts[1].trim();
        if (candidate.isEmpty()) {
            throw new IOException("Client name cannot be empty.");
        }
        if (candidate.length() > MAX_NAME_LENGTH) {
            throw new IOException("Client name too long (max " + MAX_NAME_LENGTH + ").");
        }

        playerName = candidate;
    }

    public void sendGameState(String handDesc, String topCard,
                              int[] validInputs, String decisionId, String prompt, int[] playableCardIds) {
        int[] pids = playableCardIds != null ? playableCardIds : new int[0];
        String safeDecisionId = decisionId == null || decisionId.isEmpty()
                ? NetworkProtocol.D_UNKNOWN : decisionId;
        out.println(NetworkProtocol.encode(
                NetworkProtocol.S_STATE,
                handDesc,
                topCard,
                NetworkProtocol.encodeInts(validInputs),
                safeDecisionId,
                prompt,
                NetworkProtocol.encodeInts(pids)));
    }

    public void sendNotification(String message) {
        out.println(NetworkProtocol.encode(NetworkProtocol.S_NOTIFY, message));
    }
    public void sendPlayerUpdate(String summary) {
        out.println(NetworkProtocol.encode(NetworkProtocol.S_PLAYERS, summary));
    }

    public void sendLobbyUpdate(String summary) {
        out.println(NetworkProtocol.encode(NetworkProtocol.S_LOBBY_UPDATE, summary));
    }
    
    public void sendGameStart() {
        out.println(NetworkProtocol.S_GAME_START);
    }

    public void sendGameOver(String winnerName) {
        out.println(NetworkProtocol.encode(NetworkProtocol.S_GAME_OVER, winnerName));
    }

    public int receiveAction() throws IOException {
        int prev = socket.getSoTimeout();
        socket.setSoTimeout(ACTION_READ_TIMEOUT_MS);

        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Action wait interrupted.");
                }

                String line;
                try {
                    line = in.readLine();
                } catch (SocketTimeoutException e) {
                    continue;
                }

                if (line == null) throw new IOException("Client '" + playerName + "' disconnected.");
                String[] parts = NetworkProtocol.decode(line);

                if (parts.length >= 2 && NetworkProtocol.C_ACTION.equals(parts[0])) {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        sendNotification("Acao invalida - envie um numero.");
                    }
                    continue;
                }

                if (parts.length >= 1 && NetworkProtocol.C_LEAVE_LOBBY.equals(parts[0])) {
                    throw new IOException("Client '" + playerName + "' left the game.");
                }

                sendNotification("Comando inesperado enquanto aguardava uma acao.");
            }
        } finally {
            if (!socket.isClosed()) {
                try {
                    socket.setSoTimeout(prev);
                } catch (Exception ignored) {}
            }
        }
    }

    public String getPlayerName() { return playerName; }

    



    public boolean pollLobbyDisconnectOrLeave() throws IOException {
        int prev = socket.getSoTimeout();
        socket.setSoTimeout(500);
        try {
            String line;
            try {
                line = in.readLine();
            } catch (SocketTimeoutException e) {
                return false;
            }
            if (line == null) {
                return true;
            }
            String[] parts = NetworkProtocol.decode(line);
            return parts.length >= 1 && NetworkProtocol.C_LEAVE_LOBBY.equals(parts[0]);
        } finally {
            socket.setSoTimeout(prev);
        }
    }

    
    public void clearReadTimeout() {
        try {
            socket.setSoTimeout(0);
        } catch (Exception ignored) {}
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}