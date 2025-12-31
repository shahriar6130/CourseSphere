package files.Classes;

import files.Request;
import files.Server.SocketWrapper;

import java.io.IOException;

public final class Writer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 55555;

    private Writer() {}

    public static boolean writeToFile(String line, String path) {
        SocketWrapper server = null;

        try {
            server = new SocketWrapper(HOST, PORT);

            // ✅ Use the existing constructor: Request(String path, String line)
            server.write(new Request(path, line));

            return true;

        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
            return false;

        } finally {
            if (server != null) {
                try {
                    server.closeConnection();
                } catch (IOException ignored) {}
            }
        }
    }
}
