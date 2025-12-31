package files;

import files.Server.SocketWrapper;

import java.io.EOFException;
import java.io.IOException;
import java.util.Queue;

public class ServerReadThread implements Runnable {

    private final SocketWrapper wrappedClientSocket;
    private final Queue<Object> messageQueue;

    public ServerReadThread(SocketWrapper socketWrapper, Queue<Object> messageQueue) {
        this.wrappedClientSocket = socketWrapper;
        this.messageQueue = messageQueue;

        Thread t = new Thread(this, "ServerReadThread");
        t.start(); // ✅ don't set daemon for a server thread
        System.out.println("✅ ServerReadThread started");
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object o = wrappedClientSocket.read();

                if (o instanceof Request request) {
                    enqueue(request);
                } else {
                    System.out.println("⚠️ Unknown object from client: " +
                            (o == null ? "null" : o.getClass().getName()));
                }
            }
        } catch (EOFException e) {
            System.out.println("🔌 Client disconnected (EOF).");
        } catch (IOException e) {
            System.out.println("🔌 Client disconnected (read IO): " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Class not found in ServerReadThread: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ ServerReadThread error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enqueue(Object msg) {
        synchronized (messageQueue) {
            messageQueue.offer(msg);
            messageQueue.notifyAll();
        }
    }
}
