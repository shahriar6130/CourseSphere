package files.Controllers;

import files.Classes.Course;
import files.Classes.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ViewStudentCoursesController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCourseID;
    @FXML private TableColumn<Course, String> colCourseName;
    @FXML private TableColumn<Course, Double> colCredit;
    @FXML private Label studentNameLabel;

    @FXML
    public void initialize() {

        // lock columns
        courseTable.getColumns().forEach(col -> {
            col.setReorderable(false);
            col.setResizable(false);
        });

        // ✅ set factories ONCE (correct way)
        colCourseID.setCellValueFactory(new PropertyValueFactory<>("courseID"));
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colCredit.setCellValueFactory(new PropertyValueFactory<>("credit"));

        studentNameLabel.setText("");
        courseTable.setItems(FXCollections.observableArrayList());
    }

    public void setStudent(Student student) {
        if (student == null) {
            studentNameLabel.setText("Student not found.");
            courseTable.getItems().clear();
            return;
        }

        studentNameLabel.setText("Courses Taken by: " + student.getName());

        ObservableList<Course> list = FXCollections.observableArrayList(student.getCourses());
        courseTable.setItems(list);

        // ✅ DEBUG (remove later if you want)
        System.out.println("OPENED ViewStudentCourses for: " + student.getID());
        System.out.println("Courses size = " + student.getCourses().size());
    }
}
