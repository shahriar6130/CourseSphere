package files.Controllers;

import files.Classes.Student;
import files.Main;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ProfileController implements ProfileUpdateListener {

    @FXML public ImageView profileImage;
    @FXML public Label nameLabel;
    @FXML public Label courseCountLabel;
    @FXML public Label idLabel;
    @FXML public Label clockLabel;

    @FXML public Button home;
    @FXML public Button logout;
    @FXML public Button editProfileButton;

    private Student currentStudent;

    public void passStuddent(Student student) {
        this.currentStudent = student;
        refreshUI();
        startClock();
    }

    private void refreshUI() {
        if (currentStudent == null) return;

        nameLabel.setText(currentStudent.getName());
        idLabel.setText(String.valueOf(currentStudent.getId()));
        courseCountLabel.setText(String.valueOf(currentStudent.getCourses().size()));

        loadProfileImage(currentStudent.getImagePath());
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void loadProfileImage(String path) {
        try {
            Image img;

            if (path != null && !path.isBlank() && new File(path).exists()) {
                img = new Image(new File(path).toURI().toString());
            } else {
                img = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/default pp.jpg")
                ));
            }

            profileImage.setImage(img);
        } catch (Exception e) {
            profileImage.setImage(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/default pp.jpg")
            )));
        }
    }

    // ✅ Live update callback
    @Override
    public void onProfileUpdated(Student updatedStudent) {
        this.currentStudent = updatedStudent;
        refreshUI();
    }

    // ================= NAVIGATION =================

    @FXML
    public void onHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());

            DashboardController controller = loader.getController();
            controller.setCurrentStudent(currentStudent);

            Stage stage = (Stage) home.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) logout.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= EDIT PROFILE =================

    @FXML
    public void onEditProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditProfile.fxml"));
            Scene scene = new Scene(loader.load());

            EditProfileController controller = loader.getController();
            controller.setStudent(currentStudent, this); // ✅ callback for live update

            Stage popup = new Stage();
            popup.setTitle("Edit Profile");
            popup.setScene(scene);
            popup.setResizable(false);
            popup.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
