package files.Controllers;

import files.Classes.Course;
import files.Classes.Loader;
import files.Classes.Student;
import files.Classes.Writer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class PendingCourseApprovalController {

    private static final Path PENDING_FILE = Paths.get("database/PendingEnrollment.txt");
    private static final String ENROLL_FILE = "database/enrollments.txt";

    @FXML private TableView<Student> pendingStudentTable;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, Integer> idColumn;

    private final ObservableList<Student> pendingStudents = FXCollections.observableArrayList();
    private Course currentCourse;

    public void setCourse(Course course) {
        this.currentCourse = course;
        loadPendingStudents();
    }

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
        pendingStudentTable.setItems(pendingStudents);
        pendingStudentTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void loadPendingStudents() {
        pendingStudents.clear();
        if (currentCourse == null) return;
        if (!Files.exists(PENDING_FILE)) return;

        try (BufferedReader br = Files.newBufferedReader(PENDING_FILE)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                int studentId;
                try {
                    studentId = Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }

                String courseId = parts[1].trim();
                if (!courseId.equalsIgnoreCase(currentCourse.getCourseID())) continue;

                Student s = Loader.studentList.searchStudent(studentId);
                if (s != null && !pendingStudents.contains(s)) pendingStudents.add(s);
            }
        } catch (IOException e) {
            System.out.println("Error reading pending enrollments: " + e.getMessage());
        }
    }

    // ===================== BUTTON ACTIONS =====================

    @FXML
    private void approveSelected() {
        Student selected = pendingStudentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (approveEnrollment(selected)) {
            pendingStudents.remove(selected);
        }
    }

    @FXML
    private void approveAll() {
        for (Student s : new ArrayList<>(pendingStudents)) {
            approveEnrollment(s);
        }
        pendingStudents.clear();
    }

    @FXML
    private void deleteSelected() {
        Student selected = pendingStudentTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        removeFromPendingFile(selected.getID(), currentCourse.getCourseID());
        pendingStudents.remove(selected);
    }

    @FXML
    private void deleteAll() {
        for (Student s : new ArrayList<>(pendingStudents)) {
            removeFromPendingFile(s.getID(), currentCourse.getCourseID());
        }
        pendingStudents.clear();
    }

    // ===================== CORE LOGIC =====================

    private boolean approveEnrollment(Student student) {
        if (student == null || currentCourse == null) return false;

        Course realCourse = Loader.courseList.searchCourse(currentCourse.getCourseID());
        if (realCourse == null) {
            showAlert("Course not found in CourseList.");
            return false;
        }

        // ✅ Add to memory (Course should do bidirectional addStudent -> student.addCourses)
        boolean added = realCourse.addStudent(student);

        // ✅ Save enrollment to file
        if (added) {
            String enrollLine = student.getID() + "," + realCourse.getCourseID();
            Writer.writeToFile(enrollLine, ENROLL_FILE);
        }

        // ✅ Remove from pending file ALWAYS after approval attempt
        removeFromPendingFile(student.getID(), realCourse.getCourseID());
        return true;
    }

    private void removeFromPendingFile(int studentId, String courseId) {
        if (!Files.exists(PENDING_FILE)) return;

        try {
            List<String> lines = Files.readAllLines(PENDING_FILE);
            List<String> updated = new ArrayList<>();

            for (String raw : lines) {
                String[] parts = raw.split(",");
                if (parts.length != 2) {
                    updated.add(raw);
                    continue;
                }

                String sid = parts[0].trim();
                String cid = parts[1].trim();

                boolean match = sid.equals(String.valueOf(studentId)) && cid.equalsIgnoreCase(courseId);

                if (!match) updated.add(raw);
            }

            Files.write(PENDING_FILE, updated, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.out.println("Error updating pending enrollments: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
