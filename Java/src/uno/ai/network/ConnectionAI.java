package uno.ai.network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ConnectionAI {
    private ServerSocket server;
    private Socket client;
    private BufferedWriter out;
    private BufferedReader in;

    public void connect(int port) throws IOException {
        connect(port, true);
    }

    public void connect(int port, boolean verbose) throws IOException {
        server = new ServerSocket(port);
        if (verbose) {
            System.out.println("Waiting for connection on port " + port + "...");
        }

        try {
            client = server.accept();
        } catch (IOException e) {
            safeClose(server);
            throw e;
        }

        client.setTcpNoDelay(true);
        out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
        in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));

        if (verbose) {
            System.out.println("Connection established");
        }
    }

    public void close() {
        safeClose(in);
        safeClose(out);
        safeClose(client);
        safeClose(server);
        in = null;
        out = null;
        client = null;
        server = null;
    }

    public int askAction(double[] obs, double[] mask, double reward, boolean done) {
        if (out == null || in == null) {
            throw new RuntimeException("AI backend disconnected: no active connection.");
        }

        try {
            String payload = encodeState(obs, mask, reward, done);
            out.write(payload);
            out.newLine();
            out.flush();

            String line = in.readLine();
            if (line == null) {
                throw new IOException("AI backend closed the connection.");
            }

            return parseAction(line);
        } catch (IOException e) {
            close();
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            System.err.println("Communication error: " + msg);
            throw new RuntimeException("AI backend disconnected: " + msg, e);
        }
    }

    private static String encodeState(double[] obs, double[] mask, double reward, boolean done) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append('{');
        sb.append("\"observation\":");
        appendArray(sb, obs);
        sb.append(',');
        sb.append("\"action_mask\":");
        appendArray(sb, mask);
        sb.append(',');
        sb.append("\"reward\":").append(Double.toString(reward));
        sb.append(',');
        sb.append("\"done\":").append(done);
        sb.append('}');
        return sb.toString();
    }

    private static void appendArray(StringBuilder sb, double[] values) {
        sb.append('[');
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(Double.toString(values[i]));
            }
        }
        sb.append(']');
    }

    private static int parseAction(String json) throws IOException {
        int keyIndex = json.indexOf("\"action\"");
        if (keyIndex < 0) {
            throw new IOException("Invalid action payload: missing 'action'.");
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            throw new IOException("Invalid action payload: missing ':'.");
        }
        int i = colon + 1;
        int len = json.length();
        while (i < len && Character.isWhitespace(json.charAt(i))) i++;
        int start = i;
        if (i < len && (json.charAt(i) == '-' || json.charAt(i) == '+')) i++;
        while (i < len && Character.isDigit(json.charAt(i))) i++;
        if (start == i || (i == start + 1 && (json.charAt(start) == '-' || json.charAt(start) == '+'))) {
            throw new IOException("Invalid action payload: empty action value.");
        }
        try {
            return Integer.parseInt(json.substring(start, i));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid action payload: non-integer action.", e);
        }
    }

    private static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // Best-effort shutdown.
            }
        }
    }

    private static void safeClose(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // Best-effort shutdown.
            }
        }
    }

    private static void safeClose(ServerSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // Best-effort shutdown.
            }
        }
    }
}