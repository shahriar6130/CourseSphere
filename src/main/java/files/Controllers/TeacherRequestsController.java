package files.Controllers;

import files.Classes.Course;
import files.Classes.Teacher;
import files.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class TeacherRequestsController {

    private static final String APPLY_FILE = "database/CourseApplications.txt";
    private static final String ENROLL_FILE = "database/StudentCourses.txt"; // ✅ approval output

    @FXML private VBox requestsBox;
    @FXML private Label statusLabel;

    // sidebar buttons
    @FXML private Button backButton;
    @FXML private Button logoutButton;

    // top label
    @FXML private Label teacherNameTop;

    private Teacher teacher;

    // ✅ cache teacher assigned course IDs (only show requests for these)
    private final Set<String> assignedCourseIds = new HashSet<>();

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        buildAssignedCache();
        refresh();
    }

    @FXML
    public void initialize() {
        setStatus("", true);
    }

    private void buildAssignedCache() {
        assignedCourseIds.clear();
        if (teacher == null) return;

        try {
            List<Course> assigned = teacher.getCoursesAssigned(); // your method
            if (assigned != null) {
                for (Course c : assigned) {
                    if (c != null && c.getCourseID() != null) {
                        assignedCourseIds.add(c.getCourseID().trim());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void refresh() {
        if (teacherNameTop != null) {
            teacherNameTop.setText(teacher == null ? "" : teacher.getName());
        }

        List<String[]> rows = readRequestsFiltered();

        requestsBox.getChildren().clear();

        if (teacher == null) {
            Label none = new Label("Teacher not set. Open this page from Teacher Dashboard.");
            none.getStyleClass().add("muted-label");
            requestsBox.getChildren().add(none);
            setStatus("Teacher not set.", false);
            return;
        }

        if (rows.isEmpty()) {
            Label none = new Label("No pending requests for your assigned courses.");
            none.getStyleClass().add("muted-label");
            requestsBox.getChildren().add(none);
            setStatus("0 request(s) found.", true);
            return;
        }

        for (String[] r : rows) {
            requestsBox.getChildren().add(buildRow(r[0], r[1]));
        }

        setStatus(rows.size() + " request(s) found.", true);
    }

    // Reads: studentId;courseId  (ONLY for this teacher's assigned courses)
    private List<String[]> readRequestsFiltered() {
        List<String[]> list = new ArrayList<>();
        File f = new File(APPLY_FILE);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", 2);
                if (p.length != 2) continue;

                String sid = p[0].trim();
                String cid = p[1].trim();

                // ✅ filter: only requests for teacher assigned courses
                if (!assignedCourseIds.isEmpty() && !assignedCourseIds.contains(cid)) {
                    continue;
                }

                list.add(new String[]{sid, cid});
            }
        } catch (Exception ignored) {}
        return list;
    }

    private VBox buildRow(String studentId, String courseId) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card"); // use TeacherDashboard.css card look

        Label title = new Label("Student: " + studentId + "  →  Course: " + courseId);
        title.getStyleClass().add("section-title");

        Button approve = new Button("Approve");
        approve.getStyleClass().addAll("action-button", "green-button");

        Button reject = new Button("Reject");
        reject.getStyleClass().addAll("action-button", "red-button");

        approve.setOnAction(e -> {
            boolean ok = appendLine(ENROLL_FILE, studentId + ";" + courseId);
            if (!ok) {
                setStatus("Failed to approve (could not write " + ENROLL_FILE + ")", false);
                return;
            }
            removeRequest(studentId, courseId);
            setStatus("Approved ✅ " + studentId + " for " + courseId, true);
            refresh();
        });

        reject.setOnAction(e -> {
            removeRequest(studentId, courseId);
            setStatus("Rejected ❌ " + studentId + " for " + courseId, true);
            refresh();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(10, spacer, approve, reject);
        card.getChildren().addAll(title, actions);

        return card;
    }

    private boolean appendLine(String path, String line) {
        try {
            File f = new File(path);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();

            // prevent duplicates
            if (existsLine(path, line)) return true;

            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(line + "\n");
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean existsLine(String path, String line) {
        File f = new File(path);
        if (!f.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String s;
            while ((s = br.readLine()) != null) {
                if (s.trim().equalsIgnoreCase(line.trim())) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void removeRequest(String studentId, String courseId) {
        File f = new File(APPLY_FILE);
        if (!f.exists()) return;

        List<String> keep = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", 2);
                if (p.length != 2) continue;

                String sid = p[0].trim();
                String cid = p[1].trim();

                if (!(sid.equals(studentId) && cid.equalsIgnoreCase(courseId))) {
                    keep.add(line);
                }
            }
        } catch (Exception ignored) {}

        try (FileWriter fw = new FileWriter(f, false)) {
            for (String s : keep) fw.write(s + "\n");
        } catch (Exception ignored) {}
    }

    private void setStatus(String msg, boolean ok) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(ok
                ? "-fx-text-fill:#2e7d32; -fx-font-weight:700;"
                : "-fx-text-fill:#c62828; -fx-font-weight:700;");
    }

    // ===================== NAV =====================

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/TeacherDashboard.fxml"));
            Scene scene = new Scene(loader.load());

            TeacherDashboardController controller = loader.getController();
            controller.setTeacher(teacher);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setTitle("Teacher Dashboard");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
