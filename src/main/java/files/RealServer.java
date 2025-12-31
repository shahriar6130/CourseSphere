// ✅ files/RealServer.java
package files;

import files.Server.NotificationServer;
import files.Server.SocketWrapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class RealServer {

    public static final int PORT = 55555;

    private ServerSocket serverSocket;

    public RealServer() {
        try {
            // start notification server (port 44444) in background
            startNotificationServer();

            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ RealServer started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Client connected from: " + clientSocket.getInetAddress());

                serve(clientSocket);
            }

        } catch (Exception e) {
            System.err.println("❌ RealServer startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void serve(Socket clientSocket) {
        try {
            Queue<Object> messageQueue = new LinkedList<>();
            SocketWrapper wrappedClientSocket = new SocketWrapper(clientSocket);

            new ServerReadThread(wrappedClientSocket, messageQueue);
            new ServerWriteThread(wrappedClientSocket, messageQueue);

        } catch (IOException e) {
            System.err.println("❌ Error setting up client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startNotificationServer() {
        new Thread(() -> {
            try {
                new NotificationServer(); // runs forever
            } catch (Exception e) {
                System.err.println("❌ NotificationServer failed: " + e.getMessage());
            }
        }, "NotificationServerThread").start();

        System.out.println("✅ NotificationServer thread started (port " + NotificationServer.PORT + ")");
    }

    public static void main(String[] args) {
        new RealServer();
    }
}
