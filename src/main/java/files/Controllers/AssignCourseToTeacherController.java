package files.Controllers;

import files.Classes.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

public class AssignCourseToTeacherController implements Initializable {

    private static final String ASSIGN_FILE = "database/AssignedCoursesTeacher.txt";

    @FXML private Label teacherNameLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> courseIdCol;
    @FXML private TableColumn<Course, String> courseNameCol;
    @FXML private TableColumn<Course, Double> courseCreditCol;

    @FXML private Button approveButton;
    @FXML private Button cancelButton;

    @FXML private Label statusLabel;

    private Teacher teacher;

    private final ObservableList<Course> courseSource = FXCollections.observableArrayList();
    private FilteredList<Course> filteredCourses;

    // ✅ extra flag to disable button while processing (WITHOUT setDisable)
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;

        if (teacherNameLabel != null && teacher != null) {
            teacherNameLabel.setText("Assign Courses to " + teacher.getName() + " (" + teacher.getID() + ")");
        }
        loadCourses();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Table setup
        courseIdCol.setCellValueFactory(new PropertyValueFactory<>("courseID"));
        courseNameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseCreditCol.setCellValueFactory(new PropertyValueFactory<>("credit"));

        courseTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // ✅ Disable approve when: no selection OR busy
        approveButton.disableProperty().bind(
                courseTable.getSelectionModel().selectedItemProperty().isNull()
                        .or(busy)
        );

        // Search filtering
        filteredCourses = new FilteredList<>(courseSource, p -> true);
        courseTable.setItems(filteredCourses);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal == null) ? "" : newVal.trim().toLowerCase();
            filteredCourses.setPredicate(course -> {
                if (filter.isEmpty()) return true;
                return course.getCourseID().toLowerCase().contains(filter)
                        || course.getCourseName().toLowerCase().contains(filter);
            });
        });

        approveButton.setOnAction(event -> assignSelectedCourse());
        cancelButton.setOnAction(event -> closeWindow());

        setStatus("", true);
    }

    private void loadCourses() {
        if (teacher == null) return;

        List<Course> applicable = new ArrayList<>();
        for (Course c : Loader.courseList.getCourses()) {
            if (!teacher.getCoursesAssigned().contains(c)) {
                applicable.add(c);
            }
        }

        courseSource.setAll(applicable);
        setStatus("Showing " + applicable.size() + " available course(s).", true);
    }

    private void assignSelectedCourse() {
        if (teacher == null) {
            setStatus("Teacher not set.", false);
            return;
        }

        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            setStatus("Please select a course.", false);
            return;
        }

        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Assignment");
        confirm.setHeaderText("Assign this course to teacher?");
        confirm.setContentText(
                "Teacher: " + teacher.getName() + " (" + teacher.getID() + ")\n" +
                        "Course: " + selectedCourse.getCourseID() + " - " + selectedCourse.getCourseName()
        );

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        setStatus("Assigning...", true);
        busy.set(true); // ✅ disable via binding (no setDisable)

        new Thread(() -> {
            try {
                // Prevent duplicate assignment in file
                boolean alreadyExists = isAssignmentAlreadySaved(teacher.getID(), selectedCourse.getCourseID());
                if (alreadyExists) {
                    Platform.runLater(() -> setStatus("This assignment already exists in file.", false));
                    return;
                }

                // Update memory
                Loader.courseList.addTeacherToCourse(selectedCourse, teacher);

                // Write to file
                String line = teacher.getID() + "," + selectedCourse.getCourseID();
                boolean ok = Writer.writeToFile(line, ASSIGN_FILE);

                Platform.runLater(() -> {
                    if (ok) {
                        setStatus("Course assigned successfully ✅", true);
                        closeWindow();
                    } else {
                        setStatus("Failed to save assignment. Try again.", false);
                    }
                });

            } finally {
                Platform.runLater(() -> busy.set(false)); // ✅ re-enable via binding
            }
        }).start();
    }

    private boolean isAssignmentAlreadySaved(int teacherId, String courseId) {
        try (BufferedReader br = new BufferedReader(new FileReader(ASSIGN_FILE))) {
            String line;
            String key = teacherId + "," + courseId;
            while ((line = br.readLine()) != null) {
                if (line.trim().equals(key)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill: #1b5e20; -fx-font-weight: bold;"
                : "-fx-text-fill: #b71c1c; -fx-font-weight: bold;"
        );
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
