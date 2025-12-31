package files.Controllers;

import files.Classes.Session;
import files.Classes.Student;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EditProfileController {

    private static final String STUDENT_PROFILE_FILE = "database/StudentProfiles.txt";

    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField imagePathField;
    @FXML private Label statusLabel;

    private Student student;
    private ProfileUpdateListener updateListener;

    public void setStudent(Student student, ProfileUpdateListener updateListener) {
        this.student = student;
        this.updateListener = updateListener;

        if (student == null) {
            setStatus("Student not loaded.", false);
            return;
        }

        idField.setText(String.valueOf(student.getId()));
        nameField.setText(student.getName());
        imagePathField.setText(student.getImagePath() == null ? "" : student.getImagePath());
        statusLabel.setText("");
    }

    @FXML
    private void onBrowseImage() {
        if (nameField == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(nameField.getScene().getWindow());
        if (file != null) {
            imagePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onSave() {
        if (student == null) {
            setStatus("Student not loaded.", false);
            return;
        }

        String name = (nameField.getText() == null) ? "" : nameField.getText().trim();
        String pass = (passwordField.getText() == null) ? "" : passwordField.getText();
        String confirm = (confirmPasswordField.getText() == null) ? "" : confirmPasswordField.getText();
        String imgPath = (imagePathField.getText() == null) ? "" : imagePathField.getText().trim();

        if (name.isEmpty()) {
            setStatus("Name cannot be empty.", false);
            return;
        }

        if (!pass.isEmpty() && !pass.equals(confirm)) {
            setStatus("Passwords do not match.", false);
            return;
        }

        // ✅ Update in-memory object
        student.setName(name);
        if (!pass.isEmpty()) student.setPassword(pass);
        student.setImagePath(imgPath.isBlank() ? null : imgPath);

        // ✅ Keep updated student globally (this fixes "go to other page and back")
        Session.setStudent(student);

        // ✅ Update file (your upsert approach is correct)
        boolean ok = upsertStudentProfile(student, STUDENT_PROFILE_FILE);
        if (!ok) {
            setStatus("Failed to save profile.", false);
            return;
        }

        // ✅ Notify dashboard / caller so UI refreshes immediately
        if (updateListener != null) {
            updateListener.onProfileUpdated(student);
        }

        setStatus("Profile updated successfully ✅", true);

        // close popup
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if (nameField != null && nameField.getScene() != null) {
            ((Stage) nameField.getScene().getWindow()).close();
        }
    }

    /**
     * File format: id,name,password,imagePath
     * Replace line if id exists, otherwise append.
     */
    private boolean upsertStudentProfile(Student s, String filePath) {
        try {
            File f = new File(filePath);
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            if (!f.exists()) f.createNewFile();

            List<String> lines = new ArrayList<>();
            boolean replaced = false;

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", 4);
                    if (parts.length >= 1 && parts[0].trim().equals(String.valueOf(s.getId()))) {
                        lines.add(buildProfileLine(s));
                        replaced = true;
                    } else {
                        lines.add(line);
                    }
                }
            }

            if (!replaced) lines.add(buildProfileLine(s));

            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                for (String l : lines) pw.println(l);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String buildProfileLine(Student s) {
        String safeName = (s.getName() == null) ? "" : s.getName().replace(",", " ");
        String safePass = (s.getPassword() == null) ? "" : s.getPassword().replace(",", " ");
        String safeImage = (s.getImagePath() == null) ? "" : s.getImagePath().replace(",", " ");
        return s.getId() + "," + safeName + "," + safePass + "," + safeImage;
    }

    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
    }
}
