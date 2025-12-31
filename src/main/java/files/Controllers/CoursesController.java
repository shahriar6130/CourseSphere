package files.Controllers;

import files.Classes.Student;
import files.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CoursesController {

    private static final String COURSES_FILE = "database/Courses.txt";
    private static final String APPLY_FILE   = "database/CourseApplications.txt";

    // Optional: allow “drop” enrolled course (if you want)
    private static final String ENROLLMENTS_FILE = "database/enrollments.txt"; // format assumed: studentId,courseId

    @FXML private Button logout;
    @FXML private Button backButton;

    @FXML private ComboBox<String> filterBox;
    @FXML private TextField SearchBox;

    @FXML private VBox allCourseVbox;
    @FXML private Label statusLabel;
    @FXML private Label studentTopLabel;

    private Student student;
    private final Set<String> appliedCourseIds = new HashSet<>();

    private static class CourseRow {
        String id;
        String name;
        double credit;
        CourseRow(String id, String name, double credit) {
            this.id = id; this.name = name; this.credit = credit;
        }
    }

    private List<CourseRow> allRows = new ArrayList<>();

    public void passStudent(Student student) {
        this.student = student;
        if (studentTopLabel != null && student != null) studentTopLabel.setText(student.getName());
        refreshAll(null);
    }

    @FXML
    public void initialize() {
        if (filterBox != null) {
            filterBox.getItems().setAll("All", "CSE", "EEE", "MAT", "BBA", "ENG");
            filterBox.getSelectionModel().select("All");
            filterBox.setOnAction(e -> rebuildList());
        }

        if (SearchBox != null) {
            SearchBox.textProperty().addListener((obs, ov, nv) -> rebuildList());
        }

        setStatus("", true);
    }

    @FXML
    public void refreshAll(javafx.event.ActionEvent e) {
        allRows = loadCoursesFromFileWithDuplicateProtection();
        reloadAppliedCache();
        rebuildList();
    }

    @FXML
    public void onClearFilters(javafx.event.ActionEvent e) {
        if (filterBox != null) filterBox.getSelectionModel().select("All");
        if (SearchBox != null) SearchBox.clear();
        rebuildList();
    }

    private void rebuildList() {
        if (allCourseVbox == null) return;
        allCourseVbox.getChildren().clear();

        if (allRows == null || allRows.isEmpty()) {
            Label none = new Label("No courses found in " + COURSES_FILE);
            none.getStyleClass().add("muted-label");
            allCourseVbox.getChildren().add(none);
            return;
        }

        String query = (SearchBox == null || SearchBox.getText() == null)
                ? "" : SearchBox.getText().trim().toLowerCase();

        String filter = (filterBox == null || filterBox.getValue() == null)
                ? "All" : filterBox.getValue();

        List<CourseRow> filtered = allRows.stream()
                .filter(r -> matchesFilter(r, filter))
                .filter(r -> matchesQuery(r, query))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label none = new Label("No courses match your search/filter.");
            none.getStyleClass().add("muted-label");
            allCourseVbox.getChildren().add(none);
            setStatus("No matches.", true);
            return;
        }

        for (CourseRow r : filtered) {
            allCourseVbox.getChildren().add(buildCourseCard(r));
        }

        setStatus(filtered.size() + " course(s) found.", true);
    }

    private boolean matchesFilter(CourseRow r, String filter) {
        if ("All".equalsIgnoreCase(filter)) return true;
        return r.id.toUpperCase().startsWith(filter.toUpperCase());
    }

    private boolean matchesQuery(CourseRow r, String query) {
        if (query.isEmpty()) return true;
        return r.id.toLowerCase().contains(query) || r.name.toLowerCase().contains(query);
    }

    private VBox buildCourseCard(CourseRow row) {
        VBox card = new VBox(10);
        card.getStyleClass().add("course-item");

        Label title = new Label(row.id + "  " + row.name);
        title.getStyleClass().add("file-name");

        Label meta = new Label("Credits: " + row.credit);
        meta.getStyleClass().add("muted-label");

        String courseId = row.id.trim();

        boolean enrolled = isEnrolled(courseId);
        boolean applied  = appliedCourseIds.contains(courseId);

        Button primaryBtn = new Button();
        primaryBtn.getStyleClass().addAll("action-button", "green-button");

        Button removeBtn = new Button();
        removeBtn.getStyleClass().addAll("action-button", "red-button");
        removeBtn.setVisible(false);
        removeBtn.setManaged(false);

        if (enrolled) {
            // Already enrolled -> allow optional Drop (remove from enrollments)
            primaryBtn.setText("Enrolled");
            primaryBtn.setDisable(true);
            primaryBtn.setOpacity(0.85);

            removeBtn.setText("Drop");
            removeBtn.setVisible(true);
            removeBtn.setManaged(true);
            removeBtn.setOnAction(e -> handleDrop(courseId));

        } else if (applied) {
            // Requested -> allow Withdraw request
            primaryBtn.setText("Requested");
            primaryBtn.setDisable(true);
            primaryBtn.setOpacity(0.85);

            removeBtn.setText("Withdraw");
            removeBtn.setVisible(true);
            removeBtn.setManaged(true);
            removeBtn.setOnAction(e -> handleWithdraw(courseId));

        } else {
            // Not enrolled and not requested -> Add/Apply
            primaryBtn.setText("Add / Apply");
            primaryBtn.setOnAction(e -> handleAddOrApply(row));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(10);
        buttons.getChildren().add(primaryBtn);
        if (removeBtn.isManaged()) buttons.getChildren().add(removeBtn);

        HBox top = new HBox(12, title, spacer, buttons);
        card.getChildren().addAll(top, meta);

        return card;
    }

    private boolean isEnrolled(String courseId) {
        if (student == null) return false;
        try {
            return student.getCourses().stream()
                    .anyMatch(c -> c.getCourseID().trim().equalsIgnoreCase(courseId));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void handleAddOrApply(CourseRow row) {
        if (student == null) {
            setStatus("Student not set. Open this page from Dashboard again.", false);
            showInfo("Student not set. Please open from Dashboard again.");
            return;
        }

        String studentId = String.valueOf(student.getId()).trim();
        String courseId = row.id.trim();

        if (isEnrolled(courseId)) {
            setStatus("You are already enrolled in " + courseId + ".", true);
            showInfo("Already enrolled in " + courseId);
            return;
        }

        if (appliedCourseIds.contains(courseId)) {
            setStatus("Already requested for " + courseId + " ✅", true);
            showInfo("Already requested for " + courseId);
            rebuildList();
            return;
        }

        boolean ok = appendApplyRecord(studentId, courseId);

        if (ok) {
            appliedCourseIds.add(courseId);
            setStatus("✅ Applied: " + courseId + " (Waiting for approval)", true);
            showInfo("Applied successfully ✅\nWaiting for approval.");
            rebuildList();
        } else {
            setStatus("❌ Failed to apply. Could not write to " + APPLY_FILE, false);
            showInfo("Failed to apply ❌");
        }
    }

    // ===== NEW: Withdraw request (remove from CourseApplications.txt) =====
    private void handleWithdraw(String courseId) {
        if (student == null) return;

        boolean confirm = confirmDialog(
                "Withdraw Request",
                "Withdraw your request for " + courseId + "?",
                "This will remove your application for this course."
        );
        if (!confirm) return;

        String studentId = String.valueOf(student.getId()).trim();
        boolean ok = removeLines(APPLY_FILE, line -> {
            String[] p = line.split(";", 2);
            if (p.length != 2) return false;
            return p[0].trim().equals(studentId) && p[1].trim().equalsIgnoreCase(courseId);
        });

        if (ok) {
            appliedCourseIds.remove(courseId);
            setStatus("✅ Withdrawn: " + courseId, true);
            showInfo("Withdrawn successfully ✅");
            rebuildList();
        } else {
            setStatus("❌ Failed to withdraw.", false);
            showInfo("Failed to withdraw ❌");
        }
    }

    // ===== NEW: Drop enrolled course (remove from enrollments.txt) =====
    private void handleDrop(String courseId) {
        if (student == null) return;

        boolean confirm = confirmDialog(
                "Drop Course",
                "Drop " + courseId + "?",
                "This will remove the enrollment record."
        );
        if (!confirm) return;

        String studentId = String.valueOf(student.getId()).trim();

        boolean ok = removeLines(ENROLLMENTS_FILE, line -> {
            String[] p = line.split(",", 2);
            if (p.length != 2) return false;
            return p[0].trim().equals(studentId) && p[1].trim().equalsIgnoreCase(courseId);
        });

        if (ok) {
            // best-effort: update in-memory list so UI reflects fast
            try {
                student.getCourses().removeIf(c -> c.getCourseID().trim().equalsIgnoreCase(courseId));
            } catch (Exception ignored) {}

            setStatus("✅ Dropped: " + courseId, true);
            showInfo("Dropped successfully ✅");
            rebuildList();
        } else {
            setStatus("❌ Failed to drop.", false);
            showInfo("Failed to drop ❌");
        }
    }

    private boolean appendApplyRecord(String studentId, String courseId) {
        try {
            File f = new File(APPLY_FILE);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();

            if (isAlreadyInApplyFile(studentId, courseId)) return true;

            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(studentId + ";" + courseId + "\n");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAlreadyInApplyFile(String studentId, String courseId) {
        File f = new File(APPLY_FILE);
        if (!f.exists()) return false;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", 2);
                if (p.length == 2) {
                    String sid = p[0].trim();
                    String cid = p[1].trim();
                    if (sid.equals(studentId) && cid.equalsIgnoreCase(courseId)) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void reloadAppliedCache() {
        appliedCourseIds.clear();
        if (student == null) return;

        String studentId = String.valueOf(student.getId()).trim();
        File f = new File(APPLY_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", 2);
                if (p.length == 2) {
                    if (p[0].trim().equals(studentId)) {
                        appliedCourseIds.add(p[1].trim());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // ===== UPDATED: load + hide duplicates (same code OR same name) =====
    private List<CourseRow> loadCoursesFromFileWithDuplicateProtection() {
        List<CourseRow> list = new ArrayList<>();
        File f = new File(COURSES_FILE);
        if (!f.exists()) return list;

        Set<String> seenIds = new HashSet<>();
        Set<String> seenNamesLower = new HashSet<>();
        int dupCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;

                String id = parts[0].trim();
                String name = parts[1].trim();
                double credit;

                try {
                    credit = Double.parseDouble(parts[2].trim());
                } catch (Exception ex) {
                    credit = 0.0;
                }

                String idKey = id.toLowerCase();
                String nameKey = name.toLowerCase();

                // ✅ block duplicates: same course code OR same name
                if (seenIds.contains(idKey) || seenNamesLower.contains(nameKey)) {
                    dupCount++;
                    continue;
                }

                seenIds.add(idKey);
                seenNamesLower.add(nameKey);

                list.add(new CourseRow(id, name, credit));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (dupCount > 0) {
            setStatus("⚠️ " + dupCount + " duplicate course(s) hidden (same code or same name).", true);
        }

        return list;
    }

    // ===== file rewrite helper (used by Withdraw/Drop) =====
    private boolean removeLines(String path, Predicate<String> shouldRemove) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return true;

            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            List<String> kept = new ArrayList<>();

            for (String line : lines) {
                if (line == null) continue;
                String t = line.trim();
                if (t.isEmpty()) continue;
                if (!shouldRemove.test(t)) kept.add(line);
            }

            Files.write(p, kept, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setStatus(String msg, boolean ok) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(ok
                ? "-fx-text-fill:#2e7d32; -fx-font-weight:700;"
                : "-fx-text-fill:#c62828; -fx-font-weight:700;");
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private boolean confirmDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> res = alert.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    // ===================== NAV =====================

    @FXML
    public void onBack(javafx.event.ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            DashboardController controller = loader.getController();
            controller.setCurrentStudent(student);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setTitle("Dashboard");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onLogout(javafx.event.ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) logout.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
