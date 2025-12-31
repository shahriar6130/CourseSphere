// ✅ files/ServerWriteThread.java
package files;

import files.Classes.*;
import files.Server.SocketWrapper;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;

public class ServerWriteThread implements Runnable {

    private final SocketWrapper wrappedClientSocket;
    private final Queue<Object> messageQueue;

    // ✅ NOT final (we recreate to avoid clearing unmodifiable lists)
    private CourseList courseList = new CourseList();
    private StudentList studentList = new StudentList();
    private TeacherList teacherList = new TeacherList();

    // ✅ profile override files (saved by EditProfileController)
    private static final String STUDENT_PROFILE_FILE = "database/StudentProfiles.txt";
    private static final String TEACHER_PROFILE_FILE = "database/TeacherProfiles.txt";

    public ServerWriteThread(SocketWrapper socketWrapper, Queue<Object> messageQueue) {
        this.wrappedClientSocket = socketWrapper;
        this.messageQueue = messageQueue;

        Thread t = new Thread(this, "ServerWriteThread");
        t.setDaemon(true);
        t.start();

        System.out.println("✅ ServerWriteThread started");
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object msg;

                synchronized (messageQueue) {
                    while ((msg = messageQueue.poll()) == null) {
                        messageQueue.wait();
                    }
                }

                if (!(msg instanceof Request request)) continue;

                switch (request.getRequestType()) {
                    case GET_ALL_COORDINATED_DATA -> handleGetAll();
                    case WRITE_TO_FILE -> handleWriteToFile(request);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("ServerWriteThread interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("❌ ServerWriteThread error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { wrappedClientSocket.closeConnection(); } catch (Exception ignored) {}
        }
    }

    // =========================
    // GET ALL DATA
    // =========================
    private void handleGetAll() {
        // ✅ recreate lists
        courseList = new CourseList();
        studentList = new StudentList();
        teacherList = new TeacherList();

        loadStudents();
        loadTeachers();
        loadCourses();

        // ✅ apply saved profile overrides BEFORE coordinating and sending
        applyStudentProfiles();
        applyTeacherProfiles();

        coordinateStudentCourse();
        coordinateTeacherCourse();

        try {
            wrappedClientSocket.write(studentList);
            wrappedClientSocket.write(teacherList);
            wrappedClientSocket.write(courseList);
            System.out.println("✅ Sent: StudentList, TeacherList, CourseList");
        } catch (Exception e) {
            System.out.println("❌ Failed to send data: " + e.getMessage());
        }
    }

    // =========================
    // WRITE TO FILE (FIXED)
    // =========================
    private void handleWriteToFile(Request request) {
        if (request.getPath() == null || request.getPath().isBlank()) return;
        if (request.getLine() == null) return;

        try {
            Path p = Paths.get(request.getPath());
            Path parent = p.getParent();
            if (parent != null) Files.createDirectories(parent);

            String fileName = p.getFileName().toString();

            // ✅ UPSERT for profile + credential files (replace by id, don't append forever)
            if (isUpsertFile(fileName)) {
                upsertByFirstField(p, request.getLine(), ",");
                return;
            }

            // default: append (for enrollments, course applications, etc.)
            try (BufferedWriter writer = Files.newBufferedWriter(p,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(request.getLine());
                writer.newLine();
            }

        } catch (Exception e) {
            System.out.println("❌ WRITE_TO_FILE failed: " + e.getMessage());
        }
    }

    private boolean isUpsertFile(String fileName) {
        if (fileName == null) return false;
        return fileName.equalsIgnoreCase("StudentProfiles.txt")
                || fileName.equalsIgnoreCase("TeacherProfiles.txt")
                || fileName.equalsIgnoreCase("StudentCredentials.txt")
                || fileName.equalsIgnoreCase("TeacherCredentials.txt");
    }

    /**
     * ✅ Replace a line where first field (ID) matches, otherwise add.
     * Works for comma-separated files that start with id.
     */
    private void upsertByFirstField(Path path, String newLine, String delimiter) throws IOException {
        Files.createDirectories(path.getParent() == null ? Paths.get(".") : path.getParent());
        if (!Files.exists(path)) Files.createFile(path);

        String newKey = firstField(newLine, delimiter);
        if (newKey.isBlank()) {
            // if we can't parse key, fallback to append
            try (BufferedWriter writer = Files.newBufferedWriter(path,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(newLine);
                writer.newLine();
            }
            return;
        }

        List<String> lines = Files.readAllLines(path);
        boolean replaced = false;

        List<String> out = new ArrayList<>(lines.size() + 1);
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;

            String key = firstField(line, delimiter);
            if (!replaced && key.equals(newKey)) {
                out.add(newLine);
                replaced = true;
            } else {
                out.add(line);
            }
        }
        if (!replaced) out.add(newLine);

        Files.write(path, out, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private String firstField(String line, String delimiter) {
        if (line == null) return "";
        String[] parts = line.split(delimiter, 2);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    // =========================
    // LOAD DATA
    // =========================
    private void loadCourses() {
        Path p = Paths.get("database/Courses.txt");
        if (!Files.exists(p)) return;

        try (BufferedReader reader = Files.newBufferedReader(p)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] w = line.split(",");
                if (w.length == 3) {
                    Course c = new Course(w[0].trim(), w[1].trim(), Double.parseDouble(w[2].trim()));
                    courseList.addCourse(c);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading courses: " + e.getMessage());
        }
    }

    private void loadTeachers() {
        Path p = Paths.get("database/TeacherCredentials.txt");
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String data;
            while ((data = br.readLine()) != null) {
                String[] creds = data.split(",");
                if (creds.length == 4) {
                    int id = Integer.parseInt(creds[0].trim());
                    String name = creds[1].trim();
                    String pass = creds[2].trim();
                    boolean approved = Boolean.parseBoolean(creds[3].trim());
                    if (approved) teacherList.addTeacher(new Teacher(name, id, pass));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading teachers: " + e.getMessage());
        }
    }

    private void loadStudents() {
        Path p = Paths.get("database/StudentCredentials.txt");
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String data;
            while ((data = br.readLine()) != null) {
                String[] creds = data.split(",");
                if (creds.length == 4) {
                    int id = Integer.parseInt(creds[0].trim());
                    String name = creds[1].trim();
                    String pass = creds[2].trim();
                    boolean approved = Boolean.parseBoolean(creds[3].trim());
                    if (approved) studentList.addStudent(new Student(name, id, pass));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading students: " + e.getMessage());
        }
    }

    // =========================
    // APPLY PROFILE OVERRIDES
    // =========================
    private void applyStudentProfiles() {
        Path p = Paths.get(STUDENT_PROFILE_FILE);
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 4);
                if (parts.length < 2) continue;

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (Exception ignore) { continue; }

                Student s = studentList.searchStudent(id);
                if (s == null) continue;

                String name = parts.length >= 2 ? parts[1].trim() : "";
                String pass = parts.length >= 3 ? parts[2].trim() : "";
                String img  = parts.length >= 4 ? parts[3].trim() : "";

                if (!name.isBlank()) tryInvoke(s, "setName", String.class, name);
                if (!pass.isBlank()) tryInvoke(s, "setPassword", String.class, pass);
                // imagePath is optional
                tryInvoke(s, "setImagePath", String.class, img.isBlank() ? null : img);
            }
        } catch (Exception e) {
            System.out.println("❌ applyStudentProfiles error: " + e.getMessage());
        }
    }

    private void applyTeacherProfiles() {
        Path p = Paths.get(TEACHER_PROFILE_FILE);
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 4);
                if (parts.length < 2) continue;

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (Exception ignore) { continue; }

                Teacher t = teacherList.searchTeacher(id);
                if (t == null) continue;

                String name = parts.length >= 2 ? parts[1].trim() : "";
                String pass = parts.length >= 3 ? parts[2].trim() : "";
                String img  = parts.length >= 4 ? parts[3].trim() : "";

                if (!name.isBlank()) tryInvoke(t, "setName", String.class, name);
                if (!pass.isBlank()) tryInvoke(t, "setPassword", String.class, pass);
                tryInvoke(t, "setImagePath", String.class, img.isBlank() ? null : img);
            }
        } catch (Exception e) {
            System.out.println("❌ applyTeacherProfiles error: " + e.getMessage());
        }
    }

    private void tryInvoke(Object target, String method, Class<?> paramType, Object arg) {
        try {
            Method m = target.getClass().getMethod(method, paramType);
            m.invoke(target, arg);
        } catch (Exception ignored) {}
    }

    // =========================
    // COORDINATION (FIXED)
    // =========================
    private void coordinateStudentCourse() {
        // ✅ support BOTH formats:
        // 1) database/enrollments.txt : studentId,courseId
        // 2) database/StudentCourses.txt : studentId;courseId
        List<Path> candidates = List.of(
                Paths.get("database/enrollments.txt"),
                Paths.get("database/StudentCourses.txt")
        );

        for (Path p : candidates) {
            if (!Files.exists(p)) continue;

            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;

                    String[] parts = line.contains(";")
                            ? line.split(";", 2)
                            : line.split(",", 2);

                    if (parts.length != 2) continue;

                    int studentId = Integer.parseInt(parts[0].trim());
                    String courseId = parts[1].trim();

                    Student s = studentList.searchStudent(studentId);
                    if (s == null) continue;

                    courseList.addStudentToCourse(courseId, s);
                }
            } catch (Exception e) {
                System.out.println("❌ coordinateStudentCourse error (" + p + "): " + e.getMessage());
            }
        }
    }

    private void coordinateTeacherCourse() {
        Path p = Paths.get("database/AssignedCoursesTeacher.txt");
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length != 2) continue;

                int teacherId = Integer.parseInt(parts[0].trim());
                String courseId = parts[1].trim();

                Teacher t = teacherList.searchTeacher(teacherId);
                if (t == null) continue;

                courseList.addTeacherToCourse(courseId, t);
            }
        } catch (Exception e) {
            System.out.println("❌ coordinateTeacherCourse error: " + e.getMessage());
        }
    }
}
