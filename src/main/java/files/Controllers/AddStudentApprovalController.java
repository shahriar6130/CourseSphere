package files.Controllers;

import files.Classes.Loader;
import files.Classes.Student;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class AddStudentApprovalController {

    private static final String PENDING_FILE = "database/StudentCredentials.txt";

    @FXML private TableView<Student> pendingStudentTable;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, Integer> idColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Student> pendingStudents =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        // IMPORTANT: uses getID()
        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("ID")
        );

        pendingStudentTable.setItems(pendingStudents);

        loadPendingStudentsAsync();
    }

    // ================= LOAD =================

    private void loadPendingStudentsAsync() {
        new Thread(() -> {
            List<Student> list = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(PENDING_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",");
                    if (p.length == 4) {
                        boolean approved = Boolean.parseBoolean(p[3].trim());
                        if (!approved) {
                            int id = Integer.parseInt(p[0].trim());
                            String name = p[1].trim();
                            String pass = p[2].trim();
                            list.add(new Student(name, id, pass));
                        }
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        statusLabel.setText("Failed to load pending students.")
                );
            }

            Platform.runLater(() -> pendingStudents.setAll(list));
        }).start();
    }

    // ================= APPROVE =================

    @FXML
    private void approveSelected() {
        Student s = pendingStudentTable.getSelectionModel().getSelectedItem();
        if (s == null) return;

        updateStudent(s, true);
    }

    @FXML
    private void approveAll() {
        for (Student s : new ArrayList<>(pendingStudents)) {
            updateStudent(s, true);
        }
        pendingStudents.clear();
    }

    // ================= DELETE =================

    @FXML
    private void deleteSelected() {
        Student s = pendingStudentTable.getSelectionModel().getSelectedItem();
        if (s == null) return;

        updateStudent(s, false);
    }

    @FXML
    private void deleteAll() {
        for (Student s : new ArrayList<>(pendingStudents)) {
            updateStudent(s, false);
        }
        pendingStudents.clear();
    }

    // ================= CORE FILE UPDATE =================

    private void updateStudent(Student student, boolean approve) {
        new Thread(() -> {
            Path path = Paths.get(PENDING_FILE);

            try {
                List<String> lines = Files.readAllLines(path);
                List<String> updated = new ArrayList<>();

                for (String line : lines) {
                    if (line.startsWith(student.getID() + ",")) {
                        if (approve) {
                            String[] parts = line.split(",");
                            parts[3] = "true";
                            updated.add(String.join(",", parts));
                            Loader.studentList.addStudent(student);
                        }
                        // else delete → skip line
                    } else {
                        updated.add(line);
                    }
                }

                Files.write(path, updated,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                );

                Platform.runLater(() -> {
                    pendingStudents.remove(student);
                    statusLabel.setText(
                            approve ? "Student approved ✅" : "Student deleted 🗑"
                    );
                });

            } catch (IOException e) {
                Platform.runLater(() ->
                        statusLabel.setText("File update failed.")
                );
            }
        }).start();
    }
}
