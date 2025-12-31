package files.Controllers;

import files.Classes.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class AdminDashboardController implements Initializable {

    // SEARCH FIELDS
    @FXML private TextField ADcourseSearchField;
    @FXML private TextField ADstudentSearchField;
    @FXML private TextField ADteacherSearchField;

    // STUDENT TABLE
    @FXML private TableView<Student> ADstudentTable;
    @FXML private TableColumn<Student, String> ADstudentNameColumn;
    @FXML private TableColumn<Student, Integer> ADstudentIdColumn;

    // TEACHER TABLE
    @FXML private TableView<Teacher> ADteacherTable;
    @FXML private TableColumn<Teacher, String> ADteacherNameColumn;
    @FXML private TableColumn<Teacher, Integer> ADteacherIdColumn;

    // COURSE TABLE
    @FXML private TableView<Course> ADcourseTable;
    @FXML private TableColumn<Course, String> ADcourseIDColumn;
    @FXML private TableColumn<Course, String> ADcourseNameColumn;
    @FXML private TableColumn<Course, Double> ADcourseCreditColumn;

    // BUTTONS
    @FXML private Button ADaddStudentButton;
    @FXML private Button ADaddTeacherButton;
    @FXML private Button ADaddCourseButton;
    @FXML private Button ADsignOutButton;
    @FXML private Button ADrefreshButton;
    @FXML private Button ADassignTeacherButton;
    @FXML private Button ADremoveStudentButton;
    @FXML private Button ADremoveCourseButton;

    // COUNTS
    @FXML private Label ADstudentCountLabel;
    @FXML private Label ADteacherCountLabel;
    @FXML private Label ADcourseCountLabel;

    // Data sources
    private final ObservableList<Student> studentSource = FXCollections.observableArrayList();
    private final ObservableList<Teacher> teacherSource = FXCollections.observableArrayList();
    private final ObservableList<Course> courseSource   = FXCollections.observableArrayList();

    private FilteredList<Student> filteredStudentList;
    private FilteredList<Teacher> filteredTeacherList;
    private FilteredList<Course>  filteredCourseList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        setupStudentTable();
        setupTeacherTable();
        setupCourseTable();

        configureTablesUI();

        reloadSourcesFromLoader();

        filteredStudentList = new FilteredList<>(studentSource, p -> true);
        filteredTeacherList = new FilteredList<>(teacherSource, p -> true);
        filteredCourseList  = new FilteredList<>(courseSource, p -> true);

        SortedList<Student> sortedStudents = new SortedList<>(filteredStudentList);
        sortedStudents.comparatorProperty().bind(ADstudentTable.comparatorProperty());

        SortedList<Teacher> sortedTeachers = new SortedList<>(filteredTeacherList);
        sortedTeachers.comparatorProperty().bind(ADteacherTable.comparatorProperty());

        SortedList<Course> sortedCourses = new SortedList<>(filteredCourseList);
        sortedCourses.comparatorProperty().bind(ADcourseTable.comparatorProperty());

        ADstudentTable.setItems(sortedStudents);
        ADteacherTable.setItems(sortedTeachers);
        ADcourseTable.setItems(sortedCourses);

        setupSearchFiltering();
        setupButtons();
        setupDoubleClickActions();

        updateCounts();
    }

    // =================== TABLE SETUP ===================

    private void setupStudentTable() {
        ADstudentNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ADstudentIdColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));

        // Style ID column: readable + different
        styleIdColumn(ADstudentIdColumn);

        // weights (works with CONSTRAINED)
        ADstudentNameColumn.setMinWidth(260);
        ADstudentIdColumn.setMinWidth(160);
    }

    private void setupTeacherTable() {
        ADteacherNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ADteacherIdColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));

        styleIdColumn(ADteacherIdColumn);

        ADteacherNameColumn.setMinWidth(260);
        ADteacherIdColumn.setMinWidth(160);
    }

    private void setupCourseTable() {
        ADcourseIDColumn.setCellValueFactory(new PropertyValueFactory<>("courseID"));
        ADcourseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        ADcourseCreditColumn.setCellValueFactory(new PropertyValueFactory<>("credit"));

        styleIdColumn(ADcourseIDColumn);

        // Tooltip for long course names
        ADcourseNameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                Tooltip tip = new Tooltip(item);
                tip.setShowDelay(javafx.util.Duration.millis(150));
                setTooltip(tip);
            }
        });

        ADcourseIDColumn.setMinWidth(120);
        ADcourseNameColumn.setMinWidth(280);
        ADcourseCreditColumn.setMinWidth(90);
    }

    private void configureTablesUI() {
        // more visible rows
        ADstudentTable.setFixedCellSize(34);
        ADteacherTable.setFixedCellSize(34);
        ADcourseTable.setFixedCellSize(34);

        // consistent resize
        ADstudentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ADteacherTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ADcourseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        lockColumns(ADstudentTable);
        lockColumns(ADteacherTable);
        lockColumns(ADcourseTable);
    }

    private void lockColumns(TableView<?> table) {
        table.getColumns().forEach(col -> {
            col.setReorderable(false);
            col.setResizable(true);
            col.setSortable(true);
        });
    }

    private <S, T> void styleIdColumn(TableColumn<S, T> col) {
        col.setCellFactory(tc -> new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(String.valueOf(item));
                setStyle("-fx-text-fill: #51606e; -fx-font-family: 'Consolas'; -fx-font-weight: 700;");
            }
        });
    }


    // =================== SEARCH FILTERING ===================

    private void setupSearchFiltering() {

        ADstudentSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = safe(newVal);
            filteredStudentList.setPredicate(student -> {
                if (filter.isEmpty()) return true;
                return safe(student.getName()).contains(filter)
                        || String.valueOf(student.getID()).contains(filter);
            });
        });

        ADteacherSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = safe(newVal);
            filteredTeacherList.setPredicate(teacher -> {
                if (filter.isEmpty()) return true;
                return safe(teacher.getName()).contains(filter)
                        || String.valueOf(teacher.getID()).contains(filter);
            });
        });

        ADcourseSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = safe(newVal);
            filteredCourseList.setPredicate(course -> {
                if (filter.isEmpty()) return true;
                return safe(course.getCourseID()).contains(filter)
                        || safe(course.getCourseName()).contains(filter);
            });
        });
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim().toLowerCase();
    }

    // =================== BUTTONS ===================

    private void setupButtons() {

        ADaddStudentButton.setOnAction(e -> openWindow("/fxml/Admin/AddStudentApproval.fxml", "Approve Students", true));
        ADaddTeacherButton.setOnAction(e -> openWindow("/fxml/Admin/AddTeacherApproval.fxml", "Approve Teachers", true));
        ADaddCourseButton.setOnAction(e -> openWindow("/fxml/Admin/AddCourse.fxml", "Add Course", true));

        ADrefreshButton.setOnAction(e -> refreshAllTablesAsync());
        ADsignOutButton.setOnAction(e -> signOut());

        ADassignTeacherButton.setOnAction(e -> {
            Teacher selectedTeacher = ADteacherTable.getSelectionModel().getSelectedItem();
            if (selectedTeacher == null) {
                showAlert(Alert.AlertType.WARNING, "Select Teacher", "Please select a teacher first!");
                return;
            }
            openAssignCourseWindow(selectedTeacher);
        });

        ADremoveStudentButton.setOnAction(e -> {
            Student selected = ADstudentTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select Student", "Please select a student first!");
                return;
            }
            removeStudentWithConfirm(selected);
        });

        if (ADremoveCourseButton != null) {
            ADremoveCourseButton.setOnAction(e -> {
                Course selected = ADcourseTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showAlert(Alert.AlertType.WARNING, "Select Course", "Please select a course first!");
                    return;
                }
                removeCourseWithConfirm(selected);
            });
        }
    }

    // =================== DOUBLE CLICK ===================

    private void setupDoubleClickActions() {

        ADstudentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Student selectedStudent = ADstudentTable.getSelectionModel().getSelectedItem();
                if (selectedStudent != null) openStudentCoursesWindow(selectedStudent.getID());
            }
        });

        ADteacherTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Teacher selectedTeacher = ADteacherTable.getSelectionModel().getSelectedItem();
                if (selectedTeacher != null) openTeacherCoursesWindow(selectedTeacher.getID());
            }
        });
    }

    // =================== REFRESH ===================

    private void reloadSourcesFromLoader() {
        studentSource.setAll(Loader.studentList.getStudents());
        teacherSource.setAll(Loader.teacherList.getTeachers());
        courseSource.setAll(Loader.courseList.getCourses());
    }

    public void refreshAllTablesAsync() {
        new Thread(() -> {
            Loader.reloadAll();
            Platform.runLater(() -> {
                reloadSourcesFromLoader();
                updateCounts();
            });
        }).start();
    }

    private void updateCounts() {
        ADstudentCountLabel.setText("Students: " + studentSource.size());
        ADteacherCountLabel.setText("Teachers: " + teacherSource.size());
        ADcourseCountLabel.setText("Courses: " + courseSource.size());
    }

    // =================== COURSE REMOVE ===================

    private void removeCourseWithConfirm(Course course) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Course");
        confirmation.setHeaderText("Remove this course permanently?");
        confirmation.setContentText(
                "Course: " + course.getCourseID() + " - " + course.getCourseName() +
                        "\n\nThis will also remove it from:\n" +
                        "• enrollments.txt\n" +
                        "• CourseApplications.txt\n" +
                        "• AssignedCoursesTeacher.txt"
        );

        confirmation.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;

            new Thread(() -> {
                boolean ok = removeCourseEverywhere(course.getCourseID());
                Platform.runLater(() -> {
                    if (ok) {
                        Loader.reloadAll();
                        reloadSourcesFromLoader();
                        updateCounts();
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Course removed: " + course.getCourseID());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Failed",
                                "Could not remove course. Check file formats/permissions.");
                    }
                });
            }).start();
        });
    }

    private boolean removeCourseEverywhere(String courseId) {

        boolean okMain = removeLines("database/Courses.txt", line -> {
            String[] p = line.split(",", 2);
            return p.length >= 1 && p[0].trim().equalsIgnoreCase(courseId);
        });
        if (!okMain) return false;

        removeLines("database/enrollments.txt", line -> {
            String[] p = line.split(",", 2);
            return p.length == 2 && p[1].trim().equalsIgnoreCase(courseId);
        });

        removeLines("database/CourseApplications.txt", line -> {
            String[] p = line.split(";", 2);
            return p.length == 2 && p[1].trim().equalsIgnoreCase(courseId);
        });

        removeLines("database/AssignedCoursesTeacher.txt", line -> {
            String[] p = line.split(",", 2);
            return p.length == 2 && p[1].trim().equalsIgnoreCase(courseId);
        });

        return true;
    }

    private boolean removeLines(String path, Predicate<String> removeIf) {
        File f = new File(path);
        if (!f.exists()) return true;

        try {
            List<String> out = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    if (removeIf.test(t)) continue;
                    out.add(line);
                }
            }
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                for (String l : out) pw.println(l);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =================== STUDENT FEATURES ===================

    private void openStudentCoursesWindow(int studentID) {
        try {
            Loader.reloadAll();
            Student student = Loader.studentList.searchStudent(studentID);
            if (student == null) {
                showAlert(Alert.AlertType.ERROR, "Student Not Found",
                        "Student not found in list: " + studentID);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Admin/ViewStudentCourses.fxml"));
            Scene scene = new Scene(loader.load());

            ViewStudentCoursesController controller = loader.getController();
            controller.setStudent(student);

            Stage stage = new Stage();
            stage.setTitle("Courses for " + student.getName());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to open student courses: " + e.getMessage());
        }
    }

    private void removeStudentWithConfirm(Student selectedStudent) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Student");
        confirmation.setHeaderText("Are you sure you want to remove this student?");
        confirmation.setContentText("Student: " + selectedStudent.getName()
                + " (ID: " + selectedStudent.getID() + ")\n\nThis action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            new Thread(() -> {
                boolean removedFromList = Loader.studentList.removeStudent(selectedStudent);
                boolean removedFromFile = removedFromList && removeStudentFromFile(selectedStudent.getID());

                if (removedFromFile) removeStudentFromEnrollments(selectedStudent.getID());
                else if (removedFromList) Loader.studentList.addStudent(selectedStudent);

                Platform.runLater(() -> {
                    if (removedFromFile) {
                        reloadSourcesFromLoader();
                        updateCounts();
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Student removed successfully: " + selectedStudent.getName());
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Failed",
                                "Could not remove student from file. Please try again.");
                    }
                });
            }).start();
        });
    }

    private boolean removeStudentFromFile(int studentID) {
        Path input = Paths.get("database/StudentCredentials.txt");
        Path temp  = Paths.get("database/StudentCredentials_temp.txt");

        try {
            if (!Files.exists(input)) return false;

            List<String> lines = Files.readAllLines(input);
            List<String> updated = new ArrayList<>();
            boolean found = false;

            for (String raw : lines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    updated.add(raw);
                    continue;
                }

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (NumberFormatException ex) { updated.add(raw); continue; }

                if (id == studentID) { found = true; continue; }
                updated.add(raw);
            }

            if (!found) return false;

            Files.write(temp, updated, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temp, input, StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void removeStudentFromEnrollments(int studentID) {
        Path input = Paths.get("database/enrollments.txt");
        Path temp  = Paths.get("database/enrollments_temp.txt");

        try {
            if (!Files.exists(input)) return;

            List<String> lines = Files.readAllLines(input);
            List<String> updated = new ArrayList<>();

            for (String raw : lines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 1 || parts[0].trim().isEmpty()) {
                    updated.add(raw);
                    continue;
                }

                int id;
                try { id = Integer.parseInt(parts[0].trim()); }
                catch (NumberFormatException ex) { updated.add(raw); continue; }

                if (id == studentID) continue;
                updated.add(raw);
            }

            Files.write(temp, updated, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temp, input, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================== TEACHER FEATURES ===================

    private void openTeacherCoursesWindow(int teacherID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Admin/ViewTeacherCourses.fxml"));
            Scene scene = new Scene(loader.load());

            Teacher teacher = Loader.teacherList.searchTeacher(teacherID);
            if (teacher == null) {
                showAlert(Alert.AlertType.ERROR, "Teacher Not Found", "Teacher not found: " + teacherID);
                return;
            }

            ViewTeacherCoursesController controller = loader.getController();
            controller.setTeacher(teacher);

            Stage stage = new Stage();
            stage.setTitle("Courses for " + teacher.getName());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open teacher courses: " + e.getMessage());
        }
    }

    private void openAssignCourseWindow(Teacher selectedTeacher) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Admin/AssignCourseToTeacher.fxml"));
            Scene scene = new Scene(loader.load());

            AssignCourseToTeacherController controller = loader.getController();
            controller.setTeacher(selectedTeacher);

            Stage stage = new Stage();
            stage.setTitle("Assign Courses to " + selectedTeacher.getName());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open assign window: " + e.getMessage());
        }
    }

    // =================== WINDOW HELPERS ===================

    private void openWindow(String fxmlPath, String title, boolean refreshOnClose) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(true);

            if (refreshOnClose) stage.setOnHidden(e -> refreshAllTablesAsync());
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open window: " + e.getMessage());
        }
    }

    // =================== SIGN OUT ===================

    public void signOut() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene loginScene = new Scene(loader.load());

            Stage currentStage = (Stage) ADsignOutButton.getScene().getWindow();
            currentStage.setScene(loginScene);
            currentStage.setTitle("Login");

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Sign out failed: " + e.getMessage());
        }
    }

    // =================== ALERT HELPER ===================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
