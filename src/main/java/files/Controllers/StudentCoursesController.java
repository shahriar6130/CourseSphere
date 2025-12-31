package files.Controllers;

import files.Classes.Course;
import files.Classes.Loader;
import files.Classes.Student;
import files.Main;
import files.Server.SocketWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class StudentCoursesController {

    @FXML private Button backButton;
    @FXML private Button logoutButton;
    @FXML private Button refresh;

    @FXML private VBox courseVbox;
    @FXML private Label studentTopLabel;
    @FXML private Label statusLabel;

    private Student student;
    private List<Course> courses = new ArrayList<>();

    // ✅ called from DashboardController
    public void passStudent(Student student) {
        this.student = student;
        if (studentTopLabel != null && student != null) studentTopLabel.setText(student.getName());
        reloadCoursesFromLoader();
        displayCourses();
    }

    // ✅ Always get fresh student object from Loader
    private void reloadCoursesFromLoader() {
        if (student == null) return;

        // Optional: if your Loader reads from file, this ensures latest profile/course state
        try { Loader.reloadAll(); } catch (Exception ignored) {}

        Student updated = Loader.studentList.searchStudent(student.getID());
        if (updated != null) {
            student = updated;
            courses = new ArrayList<>(student.getCourses());
        } else {
            courses = new ArrayList<>();
        }
    }

    private void displayCourses() {
        if (courseVbox == null) return;

        courseVbox.getChildren().clear();

        if (student == null) {
            courseVbox.getChildren().add(new Label("Student not set."));
            setStatus("Student not set.", false);
            return;
        }

        if (courses == null || courses.isEmpty()) {
            Label label = new Label("You haven't enrolled in any course yet.");
            label.getStyleClass().add("muted-label");
            courseVbox.getChildren().add(label);
            setStatus("0 courses found.", true);
            return;
        }

        for (Course course : courses) {
            Hyperlink link = new Hyperlink(course.getCourseID() + "  " + course.getCourseName());
            link.getStyleClass().add("file-name");
            link.setOnAction(e -> {
                try {
                    openCoursePage(course);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    setStatus("Failed to open course page.", false);
                }
            });

            VBox card = new VBox(8, link);
            card.getStyleClass().add("course-item");
            courseVbox.getChildren().add(card);
        }

        setStatus(courses.size() + " course(s) loaded.", true);
    }

    private void openCoursePage(Course course) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Student/CoursePage.fxml"));
        Scene scene = new Scene(loader.load());

        CoursePageController controller = loader.getController();

        // Server socket (wrap in try so it doesn’t crash UI if server off)
        try {
            controller.setSocketWrapper(new SocketWrapper(new Socket("127.0.0.1", 44444)));
        } catch (Exception e) {
            // CoursePage can still open if it has file-based fallback
            System.out.println("⚠️ Could not connect to server: " + e.getMessage());
        }

        controller.setCourse(course);
        controller.setStudent(student);
        controller.display();

        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setTitle("My Courses");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void toDash(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
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
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Failed to return to dashboard.", false);
        }
    }

    @FXML
    public void onLogout(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Logout failed.", false);
        }
    }

    @FXML
    public void onRefresh(ActionEvent actionEvent) {
        reloadCoursesFromLoader();
        displayCourses();
    }

    private void setStatus(String msg, boolean ok) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(ok
                ? "-fx-text-fill:#2e7d32; -fx-font-weight:700;"
                : "-fx-text-fill:#c62828; -fx-font-weight:700;");
    }
}
