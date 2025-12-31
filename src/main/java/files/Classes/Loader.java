package files.Classes;

import files.Request;
import files.Server.SocketWrapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Loader {

    public static CourseList courseList = new CourseList();
    public static StudentList studentList = new StudentList();
    public static TeacherList teacherList = new TeacherList();

    // ===== FILES (local fallback / local course mode) =====
    private static final String COURSES_FILE = "database/Courses.txt";
    private static final String STUDENT_CRED = "database/StudentCredentials.txt";
    private static final String TEACHER_CRED = "database/TeacherCredentials.txt";
    private static final String ENROLL_FILE  = "database/StudentCourses.txt"; // studentId;courseId
    private static final String ENROLL_FILE2 = "database/enrollments.txt";    // studentId,courseId
    private static final String ASSIGN_FILE  = "database/AssignedCoursesTeacher.txt"; // teacherId,courseId

    // ===== SERVER =====
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 55555;

    public static void loadAll() {
        reloadAll();
    }

    public static void reloadAll() {
        // ✅ Try server first; if fails, fallback to local.
        boolean ok = tryLoadFromServer(HOST, PORT);
        if (!ok) {
            System.out.println("⚠️ Server not reachable. Loading from local files...");
            loadAllFromLocalFiles();
        }
    }

    /**
     * ✅ If you add/remove courses locally (Admin), call this AFTER writing Courses.txt
     * so other pages show updated courses without server.
     */
    public static void reloadCoursesFromFile() {
        CourseList cl = loadCoursesLocal();
        courseList = cl;

        // optional: re-apply coordination if you need it locally
        coordinateStudentCoursesLocal();
        coordinateTeacherCoursesLocal();
    }

    // ==========================================
    // SERVER LOAD
    // ==========================================
    private static boolean tryLoadFromServer(String host, int port) {
        SocketWrapper server = null;
        try {
            server = new SocketWrapper(host, port);

            server.write(new Request(Request.RequestType.GET_ALL_COORDINATED_DATA));

            Object sObj = server.read();
            Object tObj = server.read();
            Object cObj = server.read();

            if (sObj instanceof StudentList sl) studentList = sl;
            if (tObj instanceof TeacherList tl) teacherList = tl;
            if (cObj instanceof CourseList cl)  courseList  = cl;

            // ✅ Apply local approvals AFTER server load (safe)
            applyEnrollmentsFromFile();

            return true;

        } catch (Exception e) {
            System.err.println("Load from server failed: " + e.getMessage());
            return false;

        } finally {
            if (server != null) {
                try { server.closeConnection(); } catch (IOException ignored) {}
            }
        }
    }

    // ==========================================
    // LOCAL LOAD (fallback / offline)
    // ==========================================
    private static void loadAllFromLocalFiles() {
        courseList  = loadCoursesLocal();
        studentList = loadStudentsLocal();
        teacherList = loadTeachersLocal();

        // coordinate from local mappings
        coordinateStudentCoursesLocal();
        coordinateTeacherCoursesLocal();
    }

    private static CourseList loadCoursesLocal() {
        CourseList cl = new CourseList();
        Path p = Paths.get(COURSES_FILE);
        if (!Files.exists(p)) return cl;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;

                String id = parts[0].trim();
                String name = parts[1].trim();
                double credit;

                try { credit = Double.parseDouble(parts[2].trim()); }
                catch (Exception ex) { continue; }

                // ✅ CourseList blocks duplicate id/name automatically now
                try { cl.addCourse(new Course(id, name, credit)); }
                catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Local course load failed: " + e.getMessage());
        }

        return cl;
    }

    private static StudentList loadStudentsLocal() {
        StudentList sl = new StudentList();
        Path p = Paths.get(STUDENT_CRED);
        if (!Files.exists(p)) return sl;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (Exception ex) { continue; }

                String name = parts[1].trim();
                String pass = parts[2].trim();
                boolean approved = Boolean.parseBoolean(parts[3].trim());
                if (!approved) continue;

                sl.addStudent(new Student(name, id, pass));
            }
        } catch (Exception e) {
            System.err.println("Local student load failed: " + e.getMessage());
        }
        return sl;
    }

    private static TeacherList loadTeachersLocal() {
        TeacherList tl = new TeacherList();
        Path p = Paths.get(TEACHER_CRED);
        if (!Files.exists(p)) return tl;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (Exception ex) { continue; }

                String name = parts[1].trim();
                String pass = parts[2].trim();
                boolean approved = Boolean.parseBoolean(parts[3].trim());
                if (!approved) continue;

                tl.addTeacher(new Teacher(name, id, pass));
            }
        } catch (Exception e) {
            System.err.println("Local teacher load failed: " + e.getMessage());
        }
        return tl;
    }

    // ==========================================
    // COORDINATION / ENROLLMENTS
    // ==========================================
    private static void applyEnrollmentsFromFile() {
        // server mode: apply locally-approved enrollments
        coordinateStudentCoursesLocal();
    }

    private static void coordinateStudentCoursesLocal() {
        // supports BOTH:
        // StudentCourses.txt => studentId;courseId
        // enrollments.txt    => studentId,courseId
        readStudentCourseMap(Paths.get(ENROLL_FILE), ";");
        readStudentCourseMap(Paths.get(ENROLL_FILE2), ",");
    }

    private static void readStudentCourseMap(Path p, String delimiter) {
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(delimiter, 2);
                if (parts.length != 2) continue;

                int sid;
                try { sid = Integer.parseInt(parts[0].trim()); }
                catch (Exception ex) { continue; }

                String courseId = parts[1].trim();
                if (courseId.isEmpty()) continue;

                Student s = studentList.searchStudent(sid);
                Course  c = courseList.searchCourse(courseId);

                if (s != null && c != null) {
                    // enroll() should prevent duplicates
                    try { s.enroll(c); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Student-course coordination failed: " + e.getMessage());
        }
    }

    private static void coordinateTeacherCoursesLocal() {
        Path p = Paths.get(ASSIGN_FILE);
        if (!Files.exists(p)) return;

        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length != 2) continue;

                int tid;
                try { tid = Integer.parseInt(parts[0].trim()); }
                catch (Exception ex) { continue; }

                String courseId = parts[1].trim();

                Teacher t = teacherList.searchTeacher(tid);
                Course  c = courseList.searchCourse(courseId);

                if (t != null && c != null) {
                    try { c.addTeacher(t); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Teacher-course coordination failed: " + e.getMessage());
        }
    }
}
