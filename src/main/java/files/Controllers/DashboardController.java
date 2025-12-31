package files.Controllers;

import files.Classes.Student;
import files.Classes.Session;
import files.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class DashboardController implements ProfileUpdateListener {

    @FXML private Button homeButton;
    @FXML private Button myCoursesButton;
    @FXML private Button addCourseButton;
    @FXML private Button editProfileButton;
    @FXML private Button logoutButton;

    @FXML private Label welcomeText;
    @FXML private Label nameLabel;
    @FXML private Label idLabel;

    @FXML private ImageView profileImageView;

    private Student currentStudent;

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
        Session.setStudent(student); // ✅ keep it globally
        refreshUI();
    }

    @FXML
    public void initialize() {
        if (welcomeText != null) welcomeText.setText("Welcome");
        if (nameLabel != null) nameLabel.setText("-");
        if (idLabel != null) idLabel.setText("-");

        // ✅ if this controller was loaded without setCurrentStudent
        if (currentStudent == null) {
            currentStudent = Session.getStudent();
            refreshUI();
        }
    }

    private void refreshUI() {
        if (currentStudent == null) return;

        welcomeText.setText("Welcome, " + currentStudent.getName());
        nameLabel.setText(currentStudent.getName());
        idLabel.setText(String.valueOf(currentStudent.getId()));
        setAvatar(currentStudent.getImagePath());
    }

    private void setAvatar(String path) {
        try {
            if (profileImageView == null) return;

            Image img;
            if (path != null && !path.isBlank()) {
                File file = new File(path);
                if (file.exists()) {
                    img = new Image(file.toURI().toString(), true);
                    profileImageView.setImage(img);
                    return;
                }
            }

            img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/avatar.png")));
            profileImageView.setImage(img);

        } catch (Exception ignored) {}
    }

    @Override
    public void onProfileUpdated(Student updatedStudent) {
        this.currentStudent = updatedStudent;
        Session.setStudent(updatedStudent); // ✅ persist in memory
        refreshUI();
    }

    // ===================== NAVIGATION =====================

    @FXML
    private void onHome(ActionEvent e) {
        // ✅ don't reload the whole FXML (prevents losing edited data)
        refreshUI();
    }

    @FXML
    private void onMyCourses(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Student/StudentCourses.fxml"));
            Parent root = loader.load();

            StudentCoursesController controller = loader.getController();
            controller.passStudent(currentStudent);

            switchScene(e, root, "My Courses");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onAddCourse(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Courses.fxml"));
            Parent root = loader.load();

            CoursesController controller = loader.getController();
            controller.passStudent(currentStudent);

            switchScene(e, root, "All Courses");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onEditProfile(ActionEvent e) {
        openEditProfilePopup();
    }

    private void openEditProfilePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditProfile.fxml"));
            Parent root = loader.load();

            EditProfileController controller = loader.getController();
            controller.setStudent(currentStudent, this);

            Stage popup = new Stage();
            popup.setTitle("Edit Profile");
            popup.setScene(new Scene(root));
            popup.setResizable(false);

            try {
                popup.getIcons().add(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/CSEDU_logo.png")
                )));
            } catch (Exception ignored) {}

            popup.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogout(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            switchScene(e, root, "Login");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void switchScene(ActionEvent e, Parent root, String title) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);

        // ✅ consistent sizing
        stage.setMinWidth(1200);
        stage.setMinHeight(750);
        stage.setResizable(true);

        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/CSEDU_logo.png")
            )));
        } catch (Exception ignored) {}

        stage.show();
    }
}
