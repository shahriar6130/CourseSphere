package files.Controllers;

import files.Classes.Course;
import files.Classes.Loader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AddCourseController {

    private static final String COURSES_FILE = "database/Courses.txt";

    private static final String ENROLL_FILE_COMMA = "database/enrollments.txt";        // studentId,courseId
    private static final String ENROLL_FILE_SEMI  = "database/StudentCourses.txt";     // studentId;courseId
    private static final String APPLY_FILE        = "database/CourseApplications.txt"; // studentId;courseId
    private static final String ASSIGN_FILE       = "database/AssignedCoursesTeacher.txt"; // teacherId,courseId

    @FXML private TextField courseIdField;
    @FXML private TextField courseNameField;
    @FXML private ComboBox<Double> courseCreditCombo;
    @FXML private TextArea courseDescriptionArea;

    @FXML private Label statusLabel;

    @FXML private Button addCourseButton;
    @FXML private Button removeCourseButton;
    @FXML private Button cancelButton;

    @FXML
    public void initialize() {

        courseCreditCombo.getItems().setAll(0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0);

        addCourseButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        addCourseButton.disableProperty().bind(
                courseIdField.textProperty().isEmpty()
                        .or(courseNameField.textProperty().isEmpty())
                        .or(courseCreditCombo.valueProperty().isNull())
        );

        if (removeCourseButton != null) {
            removeCourseButton.disableProperty().bind(courseIdField.textProperty().isEmpty());
            removeCourseButton.setOnAction(e -> handleRemoveCourse());
        }

        addCourseButton.setOnAction(e -> handleAddCourse());
        cancelButton.setOnAction(e -> closeWindow());

        setStatus("", true);
    }

    // =========================================================
    // ADD COURSE
    // =========================================================
    private void handleAddCourse() {
        String id = safe(courseIdField.getText());
        String name = safe(courseNameField.getText());
        Double credit = courseCreditCombo.getValue();

        if (id.isEmpty() || name.isEmpty() || credit == null) {
            setStatus("Please fill in all required fields.", false);
            return;
        }

        // block duplicates in memory (CourseList already blocks both now)
        if (Loader.courseList.searchCourse(id) != null) {
            setStatus("Course ID already exists.", false);
            return;
        }
        if (Loader.courseList.searchCourseByName(name) != null) {
            setStatus("Course name already exists (name must be unique).", false);
            return;
        }

        // file duplicate check too
        DuplicateCheck dup = checkDuplicatesInCoursesFile(id, name);
        if (dup == DuplicateCheck.ID_EXISTS) {
            setStatus("Course ID already exists in file.", false);
            return;
        }
        if (dup == DuplicateCheck.NAME_EXISTS) {
            setStatus("Course name already exists in file.", false);
            return;
        }

        String safeName = name.replace(",", " ");
        String line = String.format("%s,%s,%.2f", id, safeName, credit);

        setStatus("Saving...", true);

        new Thread(() -> {
            boolean ok = appendLine(COURSES_FILE, line);

            Platform.runLater(() -> {
                if (ok) {
                    setStatus("Course added successfully ✅", true);

                    // refresh from file so all pages stay consistent
                    Loader.reloadCoursesFromFile();

                    clearFields();
                } else {
                    setStatus("Failed to save course. Please try again.", false);
                }
            });
        }, "AddCourseThread").start();
    }

    // =========================================================
    // REMOVE COURSE
    // =========================================================
    private void handleRemoveCourse() {
        String id = safe(courseIdField.getText());
        if (id.isEmpty()) {
            setStatus("Enter a Course ID to remove.", false);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Course");
        confirmation.setHeaderText("Remove this course permanently?");
        confirmation.setContentText(
                "Course ID: " + id +
                        "\n\nThis will also remove it from:\n" +
                        "• enrollments.txt\n" +
                        "• StudentCourses.txt\n" +
                        "• CourseApplications.txt\n" +
                        "• AssignedCoursesTeacher.txt"
        );

        confirmation.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;

            setStatus("Removing...", true);

            new Thread(() -> {
                boolean removedMain = removeLines(COURSES_FILE, line -> {
                    String[] p = line.split(",", 2);
                    return p.length >= 1 && p[0].trim().equalsIgnoreCase(id);
                });

                if (!removedMain) {
                    Platform.runLater(() -> setStatus("Course ID not found in Courses.txt", false));
                    return;
                }

                // remove from enrollments.txt (comma)
                removeLines(ENROLL_FILE_COMMA, line -> {
                    String[] p = line.split(",", 2);
                    return p.length == 2 && p[1].trim().equalsIgnoreCase(id);
                });

                // remove from StudentCourses.txt (semicolon)
                removeLines(ENROLL_FILE_SEMI, line -> {
                    String[] p = line.split(";", 2);
                    return p.length == 2 && p[1].trim().equalsIgnoreCase(id);
                });

                // remove from applications (semicolon)
                removeLines(APPLY_FILE, line -> {
                    String[] p = line.split(";", 2);
                    return p.length == 2 && p[1].trim().equalsIgnoreCase(id);
                });

                // remove teacher assignment (comma)
                removeLines(ASSIGN_FILE, line -> {
                    String[] p = line.split(",", 2);
                    return p.length == 2 && p[1].trim().equalsIgnoreCase(id);
                });

                Platform.runLater(() -> {
                    Loader.reloadCoursesFromFile(); // refresh memory from file
                    setStatus("Course removed successfully ✅", true);
                    clearFields();
                });

            }, "RemoveCourseThread").start();
        });
    }

    // =========================================================
    // FILE HELPERS
    // =========================================================
    private enum DuplicateCheck { OK, ID_EXISTS, NAME_EXISTS }

    private DuplicateCheck checkDuplicatesInCoursesFile(String id, String name) {
        File f = new File(COURSES_FILE);
        if (!f.exists()) return DuplicateCheck.OK;

        String nameKey = name.trim().toLowerCase();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                String[] parts = t.split(",", 3);
                if (parts.length < 2) continue;

                String fileId = parts[0].trim();
                String fileName = parts[1].trim();

                if (fileId.equalsIgnoreCase(id)) return DuplicateCheck.ID_EXISTS;
                if (fileName.trim().toLowerCase().equals(nameKey)) return DuplicateCheck.NAME_EXISTS;
            }
        } catch (Exception ignored) {}

        return DuplicateCheck.OK;
    }

    private boolean appendLine(String path, String line) {
        try {
            File f = new File(path);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean removeLines(String path, Predicate<String> removeIf) {
        File f = new File(path);
        if (!f.exists()) return false; // not removed

        try {
            List<String> out = new ArrayList<>();
            boolean removedSomething = false;

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;

                    if (removeIf.test(t)) {
                        removedSomething = true;
                        continue;
                    }
                    out.add(line);
                }
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                for (String l : out) pw.println(l);
            }

            return removedSomething;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================
    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;

        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill: #1b5e20; -fx-font-weight: bold;"
                : "-fx-text-fill: #b71c1c; -fx-font-weight: bold;"
        );
    }

    private void clearFields() {
        courseIdField.clear();
        courseNameField.clear();
        courseCreditCombo.setValue(null);
        if (courseDescriptionArea != null) courseDescriptionArea.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
