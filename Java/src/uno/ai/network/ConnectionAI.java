package uno.ai.network;

import java.io.*;
import java.net.*;

public class ConnectionAI {
    private ServerSocket server;
    private Socket client;
    private DataOutputStream out;
    private DataInputStream in;

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
        out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(client.getInputStream()));

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
            for (double d : obs) out.writeDouble(d);
            for (double d : mask) out.writeDouble(d);

            out.writeDouble(reward);
            out.writeBoolean(done);
            out.flush();

            return in.readInt();
        } catch (IOException e) {
            close();
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            System.err.println("Communication error: " + msg);
            throw new RuntimeException("AI backend disconnected: " + msg, e);
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