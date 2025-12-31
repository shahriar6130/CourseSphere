// ✅ files/Server/NotificationServer.java
package files.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotificationServer {

    public static final int PORT = 44444;

    private static final String ANNOUNCEMENT_FILE = "database/CourseAnnouncements.txt";
    private static final String DEADLINE_FILE = "database/deadlines.txt";
    private static final String UPLOAD_BASE_DIR = "uploaded_files";

    private ServerSocket serverSocket;
    private final List<SocketWrapper> clientList = new ArrayList<>();

    public NotificationServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("✅ NotificationServer started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                SocketWrapper socketWrapper = new SocketWrapper(clientSocket);

                synchronized (clientList) {
                    clientList.add(socketWrapper);
                }

                new Thread(() -> listenToClient(socketWrapper), "NotifyClientThread").start();
            }

        } catch (IOException e) {
            System.out.println("❌ NotificationServer failed: " + e.getMessage());
        }
    }

    private void listenToClient(SocketWrapper socketWrapper) {
        try {
            while (true) {
                Object obj = socketWrapper.read();

                if (obj instanceof Notification n) {
                    // Save + broadcast announcement
                    saveAnnouncementToFile(n.getNotification());
                    broadcast(n);

                } else if (obj instanceof Deadline deadline) {
                    // Save deadline and reply ACK
                    boolean ok = saveDeadlineToFile(deadline);
                    socketWrapper.write(ok ? "DEADLINE_SAVED" : "DEADLINE_SAVE_FAILED");

                } else if (obj instanceof FilePacket packet) {
                    saveUploadedFile(packet);

                } else if (obj instanceof GetDeadlinesRequest request) {
                    List<Deadline> deadlines = loadDeadlinesForCourse(request.getCourseId());
                    socketWrapper.write(deadlines);

                } else {
                    System.out.println("⚠️ Unknown object received: " + obj.getClass().getName());
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                socketWrapper.closeConnection();
            } catch (IOException ignored) {}

            synchronized (clientList) {
                clientList.remove(socketWrapper);
            }
        }
    }

    // ===================== ANNOUNCEMENTS =====================

    private void saveAnnouncementToFile(String line) {
        if (line == null || line.isBlank()) return;

        try {
            File file = new File(ANNOUNCEMENT_FILE);
            file.getParentFile().mkdirs();

            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(line.trim() + "\n");
            }
        } catch (IOException e) {
            System.out.println("❌ Error saving announcement: " + e.getMessage());
        }
    }

    private void broadcast(Notification notification) {
        synchronized (clientList) {
            for (SocketWrapper client : new ArrayList<>(clientList)) {
                try {
                    client.write(notification);
                } catch (IOException e) {
                    System.out.println("⚠️ Broadcast failed: " + e.getMessage());
                }
            }
        }
    }

    // ===================== DEADLINES =====================

    private boolean saveDeadlineToFile(Deadline d) {
        try {
            File file = new File(DEADLINE_FILE);
            file.getParentFile().mkdirs();

            synchronized (this) {
                try (FileWriter fw = new FileWriter(file, true)) {
                    fw.write(d.toString() + "\n");
                }
            }
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving deadline: " + e.getMessage());
            return false;
        }
    }

    private List<Deadline> loadDeadlinesForCourse(String courseIdRaw) {
        String courseId = (courseIdRaw == null) ? "" : courseIdRaw.trim();
        List<Deadline> deadlines = new ArrayList<>();

        File file = new File(DEADLINE_FILE);
        if (!file.exists()) return deadlines;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";", 4);
                if (parts.length == 4 && parts[0].trim().equalsIgnoreCase(courseId)) {
                    deadlines.add(new Deadline(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            LocalDate.parse(parts[3].trim())
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error reading deadlines: " + e.getMessage());
        }
        return deadlines;
    }

    // ===================== FILE UPLOADS =====================

    private void saveUploadedFile(FilePacket packet) {
        try {
            String courseId = packet.getCourseId().trim();
            String fileName = packet.getFileName().trim();

            Path courseDir = Paths.get(UPLOAD_BASE_DIR, courseId);
            Files.createDirectories(courseDir);

            Path outFile = courseDir.resolve(fileName);
            Files.write(outFile, packet.getFileData(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("✅ File saved: " + outFile.toString());
        } catch (IOException e) {
            System.out.println("❌ File save failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new NotificationServer();
    }
}
