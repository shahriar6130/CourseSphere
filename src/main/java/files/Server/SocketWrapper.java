package files.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketWrapper implements AutoCloseable {

    private final Socket socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;

    // client-side
    public SocketWrapper(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    // server-side
    public SocketWrapper(Socket socket) throws IOException {
        this.socket = socket;
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    public Object read() throws IOException, ClassNotFoundException {
        return ois.readObject();
    }

    public void write(Object o) throws IOException {
        oos.writeObject(o);
        oos.flush();
    }

    public void closeConnection() throws IOException {
        // close streams first
        try { ois.close(); } catch (IOException ignored) {}
        try { oos.close(); } catch (IOException ignored) {}
        // then socket
        socket.close();
    }

    // ✅ Now try-with-resources works
    @Override
    public void close() throws IOException {
        closeConnection();
    }
}
