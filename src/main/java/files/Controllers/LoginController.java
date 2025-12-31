package files.Controllers;

import files.Classes.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // ===== ROOT =====
    @FXML private StackPane loginStackPane;

    // ===== LOGIN UI (must match FXML fx:id) =====
    @FXML private VBox loginAnchorPane;
    @FXML private ComboBox<String> roleBox;
    @FXML private TextField userIDField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button passwordToggleButton;

    @FXML private Button submitButton;
    @FXML private Button cancelButton;
    @FXML private Hyperlink registerHyperlink;
    @FXML private Label errorLabel;

    // ===== SIGNUP UI (must match FXML fx:id) =====
    @FXML private VBox signUpAnchorPane;
    @FXML private ComboBox<String> roleBoxSetup;
    @FXML private TextField setNameField;
    @FXML private TextField setUserIDField;

    @FXML private PasswordField setPasswordField;
    @FXML private TextField setPasswordVisibleField;
    @FXML private Button setPasswordToggleButton;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Button confirmPasswordToggleButton;

    @FXML private Button signUpButton;
    @FXML private Hyperlink LoginHyperlink;
    @FXML private Label registerErrorLabel;

    // ===== DATA =====
    private StudentList students = Loader.studentList;
    private TeacherList teachers = Loader.teacherList;

    private static final String STUDENT_CRED_PATH = "database/StudentCredentials.txt";
    private static final String TEACHER_CRED_PATH = "database/TeacherCredentials.txt";

    private static final double APP_W = 1200;
    private static final double APP_H = 750;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Platform.runLater(this::configureStage);

        showLoginPane();

        roleBox.getItems().setAll("Student", "Teacher", "Admin");
        roleBoxSetup.getItems().setAll("Student", "Teacher");

        bindPasswordToggle(passwordField, passwordVisibleField, passwordToggleButton);
        bindPasswordToggle(setPasswordField, setPasswordVisibleField, setPasswordToggleButton);
        bindPasswordToggle(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);

        userIDField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> submitButton.fire());
    }

    private void configureStage() {
        if (loginStackPane == null || loginStackPane.getScene() == null) return;
        Stage stage = (Stage) loginStackPane.getScene().getWindow();
        if (stage == null) return;

        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);
        stage.setWidth(APP_W);
        stage.setHeight(APP_H);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    // ==========================
    // PASSWORD EYE TOGGLE
    // ==========================
    private void bindPasswordToggle(PasswordField pf, TextField tf, Button eyeBtn) {
        if (pf == null || tf == null || eyeBtn == null) return;

        tf.textProperty().bindBidirectional(pf.textProperty());

        tf.setVisible(false);
        tf.setManaged(false);
        pf.setVisible(true);
        pf.setManaged(true);

        eyeBtn.setText("👁");
        eyeBtn.setFocusTraversable(false);
    }

    @FXML private void toggleLoginPassword(ActionEvent e) {
        toggle(passwordField, passwordVisibleField, passwordToggleButton);
    }

    @FXML private void toggleRegisterPassword(ActionEvent e) {
        toggle(setPasswordField, setPasswordVisibleField, setPasswordToggleButton);
    }

    @FXML private void toggleRegisterConfirmPassword(ActionEvent e) {
        toggle(confirmPasswordField, confirmPasswordVisibleField, confirmPasswordToggleButton);
    }

    private void toggle(PasswordField pf, TextField tf, Button eyeBtn) {
        boolean showing = tf.isVisible();

        tf.setVisible(!showing);
        tf.setManaged(!showing);

        pf.setVisible(showing);
        pf.setManaged(showing);

        eyeBtn.setText(showing ? "👁" : "🙈");

        Platform.runLater(() -> {
            if (!showing) tf.positionCaret(tf.getText().length());
            else pf.positionCaret(pf.getText().length());
        });
    }

    // ==========================
    // LOGIN
    // ==========================
    @FXML
    public void onSubmit(ActionEvent actionEvent) {

        // ok to reload here
        Loader.reloadAll();
        students = Loader.studentList;
        teachers = Loader.teacherList;

        errorLabel.setText("");

        String role = roleBox.getValue();
        String idText = safe(userIDField.getText());
        String pass = (passwordField.getText() == null) ? "" : passwordField.getText();

        if (role == null || idText.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Fill in all fields");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            errorLabel.setText("User ID must be numeric");
            return;
        }

        try {
            switch (role) {
                case "Student" -> loginStudent(id, pass);
                case "Teacher" -> loginTeacher(id, pass);
                case "Admin" -> loginAdmin(id, pass);
                default -> errorLabel.setText("Select a valid role");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    private void loginStudent(int id, String pass) throws IOException {
        Student s = students.searchStudent(id);
        if (s == null) {
            errorLabel.setText("Student ID not found");
            return;
        }
        if (!s.getPassword().equals(pass)) {
            errorLabel.setText("Wrong Password");
            return;
        }

        // ✅ store in session so profile edits remain while app runs
        Session.setStudent(s);
        Session.setTeacher(null);

        goToDashboard(s);
    }

    private void loginTeacher(int id, String pass) throws IOException {
        Teacher t = teachers.searchTeacher(id);
        if (t == null) {
            errorLabel.setText("Teacher ID not found");
            return;
        }
        if (!t.getPassword().equals(pass)) {
            errorLabel.setText("Wrong Password");
            return;
        }

        Session.setTeacher(t);
        Session.setStudent(null);

        goToTeacherDashboard(t);
    }

    private void loginAdmin(int id, String pass) throws IOException {
        if (!Admin.getAdminInstance().verifyCredentials(id, pass)) {
            errorLabel.setText("Admin credentials incorrect");
            return;
        }
        Session.clear();
        goToAdminDashboard();
    }

    // ==========================
    // SIGN UP
    // ==========================
    @FXML
    private void onSignUp() {
        registerErrorLabel.setText("");

        String name = safe(setNameField.getText());
        String idText = safe(setUserIDField.getText());
        String pass = (setPasswordField.getText() == null) ? "" : setPasswordField.getText();
        String confirm = (confirmPasswordField.getText() == null) ? "" : confirmPasswordField.getText();
        String role = roleBoxSetup.getValue();

        if (name.isEmpty() || idText.isEmpty() || pass.isEmpty() || confirm.isEmpty() || role == null) {
            registerErrorLabel.setText("All fields are required");
            return;
        }

        if (!pass.equals(confirm)) {
            registerErrorLabel.setText("Passwords do not match");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            registerErrorLabel.setText("User ID must be numeric");
            return;
        }

        if (pass.length() < 4) {
            registerErrorLabel.setText("Password must be 4+ characters");
            return;
        }

        if (role.equals("Student")) {
            if (String.valueOf(id).length() != 10) {
                registerErrorLabel.setText("Student ID should be 10 digits");
                return;
            }

            if (students.searchStudent(id) != null || idExistsInCredentialFile(STUDENT_CRED_PATH, id)) {
                registerErrorLabel.setText("Student ID already exists");
                return;
            }

            boolean ok = Writer.writeToFile(id + "," + name + "," + pass + ",false", STUDENT_CRED_PATH);
            registerErrorLabel.setText(ok
                    ? "Student request sent! Awaiting admin approval."
                    : "Failed to send request.");

        } else if (role.equals("Teacher")) {

            if (teachers.searchTeacher(id) != null || idExistsInCredentialFile(TEACHER_CRED_PATH, id)) {
                registerErrorLabel.setText("Teacher ID already exists");
                return;
            }

            boolean ok = Writer.writeToFile(id + "," + name + "," + pass + ",false", TEACHER_CRED_PATH);
            registerErrorLabel.setText(ok
                    ? "Teacher request sent! Awaiting admin approval."
                    : "Failed to send request.");

        } else {
            registerErrorLabel.setText("Admin cannot register here");
            return;
        }

        setNameField.clear();
        setUserIDField.clear();
        setPasswordField.clear();
        confirmPasswordField.clear();
        roleBoxSetup.getSelectionModel().clearSelection();
    }

    private boolean idExistsInCredentialFile(String path, int id) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(id + ",")) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    // ==========================
    // SWITCH PANES
    // ==========================
    @FXML
    private void onRegisterClick() {
        showSignUpPane();
        setNameField.requestFocus();
    }

    @FXML
    private void onLoginClick() {
        showLoginPane();
        userIDField.requestFocus();
    }

    private void showLoginPane() {
        loginAnchorPane.setVisible(true);
        loginAnchorPane.setManaged(true);

        signUpAnchorPane.setVisible(false);
        signUpAnchorPane.setManaged(false);

        errorLabel.setText("");
    }

    private void showSignUpPane() {
        signUpAnchorPane.setVisible(true);
        signUpAnchorPane.setManaged(true);

        loginAnchorPane.setVisible(false);
        loginAnchorPane.setManaged(false);

        registerErrorLabel.setText("");
    }

    // ==========================
    // CANCEL
    // ==========================
    @FXML
    public void onCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quit");
        alert.setHeaderText("Quitting Application");
        alert.setContentText("Are you sure you want to exit?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    // ==========================
    // SCENE SWITCHING
    // ==========================
    private void goToDashboard(Student s) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Scene scene = new Scene(loader.load());

        DashboardController controller = loader.getController();
        controller.setCurrentStudent(s);

        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setResizable(true);
        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);
        stage.setScene(scene);
        stage.setTitle("Dashboard");
        stage.centerOnScreen();
        stage.show();
    }

    private void goToTeacherDashboard(Teacher t) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TeacherDashboard.fxml"));
        Scene scene = new Scene(loader.load());

        TeacherDashboardController controller = loader.getController();
        controller.setTeacher(t);

        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setResizable(true);
        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);
        stage.setScene(scene);
        stage.setTitle("Teacher Dashboard");
        stage.centerOnScreen();
        stage.show();
    }

    private void goToAdminDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Admin/AdminDashboard.fxml"));
        Scene scene = new Scene(loader.load());

        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setResizable(true);
        stage.setMinWidth(APP_W);
        stage.setMinHeight(APP_H);
        stage.setScene(scene);
        stage.setTitle("Admin Dashboard");
        stage.centerOnScreen();
        stage.show();
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
