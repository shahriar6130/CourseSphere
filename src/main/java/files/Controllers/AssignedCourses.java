package files.Controllers;

import files.Classes.Course;
import files.Classes.Teacher;
import files.Main;
import files.Server.SocketWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AssignedCourses {

    @FXML private Button backButton;
    @FXML private Button logoutButton;

    @FXML private VBox courseVbox;
    @FXML private Label teacherNameLabel;
    @FXML private ScrollPane coursesScroll; // (exists in your FXML)

    private Teacher teacher;
    private List<Course> courses = new ArrayList<>();

    @FXML
    public void initialize() {
        // safe defaults
        if (teacherNameLabel != null) teacherNameLabel.setText("Teacher: -");
        if (courseVbox != null) courseVbox.getChildren().clear();
    }

    /** ✅ Called from other controllers */
    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;

        if (teacher != null) {
            List<Course> list = teacher.getCoursesAssigned();
            this.courses = (list == null) ? new ArrayList<>() : list;

            if (teacherNameLabel != null) {
                teacherNameLabel.setText("Teacher: " + teacher.getName());
            }
        } else {
            this.courses = new ArrayList<>();
            if (teacherNameLabel != null) {
                teacherNameLabel.setText("Teacher: -");
            }
        }

        // ✅ IMPORTANT: always rebuild UI after setting teacher
        displayCoursesd();
    }

    /** ✅ Keep this method name because you already call it elsewhere */
    public void displayCoursesd() {
        if (courseVbox == null) return;

        courseVbox.getChildren().clear();

        if (teacher == null) {
            Label msg = new Label("Teacher not set.");
            msg.getStyleClass().add("muted-label");
            courseVbox.getChildren().add(msg);
            return;
        }

        if (courses == null || courses.isEmpty()) {
            Label msg = new Label("No assigned courses found.");
            msg.getStyleClass().add("muted-label");
            courseVbox.getChildren().add(msg);
            return;
        }

        for (Course course : courses) {
            courseVbox.getChildren().add(buildCourseCard(course));
        }
    }

    private VBox buildCourseCard(Course course) {
        VBox card = new VBox(8);
        card.getStyleClass().add("course-item");
        card.setPadding(new Insets(14));

        HBox topRow = new HBox(10);

        VBox titleBox = new VBox(2);
        Label title = new Label(course.getCourseID() + " — " + course.getCourseName());
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("Click Open to manage announcements, deadlines, files, etc.");
        subtitle.getStyleClass().add("muted-label");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().addAll("action-button", "green-button");
        openBtn.setOnAction(e -> {
            try {
                openTeacherCoursePage(course);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        topRow.getChildren().addAll(titleBox, spacer, openBtn);

        HBox bottomRow = new HBox(12);
        Label info = new Label("Status: Assigned");
        info.getStyleClass().add("muted-label");
        bottomRow.getChildren().add(info);

        card.getChildren().addAll(topRow, bottomRow);

        // clickable card
        card.setOnMouseClicked(e -> {
            try {
                openTeacherCoursePage(course);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        return card;
    }

    private void openTeacherCoursePage(Course course) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TeacherCoursePage.fxml"));
        Scene scene = new Scene(loader.load());

        TeacherCoursePage controller = loader.getController();
        controller.setTeacher(teacher);
        controller.setCourse(course);

        try {
            SocketWrapper sw = new SocketWrapper(new Socket("127.0.0.1", 44444));
            controller.setSocketWrapper(sw);
        } catch (Exception e) {
            System.out.println("⚠️ Could not connect to notification server: " + e.getMessage());
        }

        controller.display();

        Stage stage = (Stage) courseVbox.getScene().getWindow();
        stage.setMinWidth(1200);
        stage.setMinHeight(750);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setTitle("Course Page");
        stage.show();
    }

    @FXML
    private void onBack(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TeacherDashboard.fxml"));
            Scene scene = new Scene(loader.load());

            TeacherDashboardController controller = loader.getController();
            controller.setTeacher(teacher);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setResizable(true);
            stage.setScene(scene);
            stage.setTitle("Teacher Dashboard");
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogout(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
