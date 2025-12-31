package files.Controllers;

import files.Classes.Course;
import files.Classes.Student;
import files.Classes.Teacher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ViewTeacherCoursesController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCourseID;
    @FXML private TableColumn<Course, String> colCourseName;
    @FXML private TableColumn<Course, Double> colCredit;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> colStudentID;
    @FXML private TableColumn<Student, String> colStudentName;

    @FXML private Label teacherNameLabel;

    @FXML
    public void initialize() {

        // ✅ lock columns (course table)
        courseTable.getColumns().forEach(col -> {
            col.setReorderable(false);
            col.setResizable(false);
        });

        // ✅ lock columns (student table)
        studentTable.getColumns().forEach(col -> {
            col.setReorderable(false);
            col.setResizable(false);
        });

        // ✅ set factories ONCE
        colCourseID.setCellValueFactory(new PropertyValueFactory<>("courseID"));
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));

        colStudentID.setCellValueFactory(new PropertyValueFactory<>("ID"));
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // ✅ defaults
        teacherNameLabel.setText("");
        courseTable.setItems(FXCollections.observableArrayList());
        studentTable.setItems(FXCollections.observableArrayList());

        // ✅ double click event (safe)
        courseTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
                loadStudentsUnderCourse(selectedCourse);
            }
        });
    }

    public void setTeacher(Teacher teacher) {
        if (teacher == null) {
            teacherNameLabel.setText("Teacher not found.");
            courseTable.getItems().clear();
            studentTable.getItems().clear();
            return;
        }

        teacherNameLabel.setText("Courses Taught by: " + teacher.getName());

        ObservableList<Course> courseList =
                FXCollections.observableArrayList(teacher.getCoursesAssigned());

        courseTable.setItems(courseList);

        // ✅ clear students until course is selected
        studentTable.getItems().clear();
    }

    private void loadStudentsUnderCourse(Course course) {

        if (course == null) {
            studentTable.getItems().clear();
            return;
        }

        ObservableList<Student> studentsList =
                FXCollections.observableArrayList(course.getCourseStudents());

        studentTable.setItems(studentsList);

        // ✅ DEBUG (remove if you want)
        System.out.println("Course selected: " + course.getCourseID() +
                " students=" + studentsList.size());
    }
}
