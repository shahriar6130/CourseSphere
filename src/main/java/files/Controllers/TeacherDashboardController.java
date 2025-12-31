package files.Controllers;

import files.Classes.Course;
import files.Classes.Teacher;
import files.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardController {

    @FXML public Button logoutButton;
    @FXML public Button homeButton;
    @FXML public Button assignedCoursesButton;
    @FXML public Button profileButton;
    @FXML public Button requestsButton;

    @FXML public VBox mainContentBox;
    @FXML public Label welcomeLabel;

    @FXML public Label nameLabel;
    @FXML public Label assignedCountLabel;

    private Teacher teacher;
    private List<Course> courses = new ArrayList<>();

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        refreshHeader();
    }

    private void refreshHeader() {
        if (teacher != null) {
            this.courses = teacher.getCoursesAssigned();
            int count = (courses == null) ? 0 : courses.size();

            nameLabel.setText("Teacher: " + teacher.getName());
            assignedCountLabel.setText("Assigned Courses: " + count);
            welcomeLabel.setText("Welcome, " + teacher.getName());
        } else {
            nameLabel.setText("Teacher: -");
            assignedCountLabel.setText("Assigned Courses: 0");
            welcomeLabel.setText("Welcome, Teacher");
        }
    }

    public void onHomeClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/TeacherDashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            TeacherDashboardController controller = fxmlLoader.getController();
            controller.setTeacher(teacher);

            Stage stage = (Stage) homeButton.getScene().getWindow();
            stage.setTitle("Teacher Dashboard");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAssignedCoursesClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AssignedCourses.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            AssignedCourses controller = fxmlLoader.getController();
            controller.setTeacher(this.teacher);
            controller.displayCoursesd();

            Stage stage = (Stage) assignedCoursesButton.getScene().getWindow();
            stage.setTitle("Assigned Courses");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRequestsClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/TeacherRequests.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            TeacherRequestsController controller = fxmlLoader.getController();
            controller.setTeacher(this.teacher);

            Stage stage = (Stage) requestsButton.getScene().getWindow();
            stage.setTitle("Course Requests");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onProfileClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/TeacherProfile.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            TeacherProfileController controller = fxmlLoader.getController();
            controller.setTeacher(this.teacher);

            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setTitle("Teacher Profile");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onLogout(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Course Management System");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
