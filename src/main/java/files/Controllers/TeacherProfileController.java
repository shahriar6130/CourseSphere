package files.Controllers;

import files.Classes.Loader;
import files.Classes.Teacher;
import files.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TeacherProfileController {

    private static final String TEACHER_CRED_PATH = "database/TeacherCredentials.txt";

    @FXML private Label teacherNameTop;
    @FXML private Label nameValue;
    @FXML private Label idValue;
    @FXML private Label courseCountValue;

    // edit UI
    @FXML private VBox editBox;
    @FXML private TextField nameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label editStatusLabel;

    private Teacher teacher;

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        refreshUI(true);
    }

    @FXML
    public void initialize() {
        if (teacherNameTop != null) teacherNameTop.setText("");
        if (nameValue != null) nameValue.setText("-");
        if (idValue != null) idValue.setText("-");
        if (courseCountValue != null) courseCountValue.setText("0");

        setEditVisible(false);
        setEditStatus("", true);
    }

    @FXML
    private void refreshProfile(ActionEvent e) {
        refreshUI(true);
    }

    private void refreshUI(boolean reloadFromLoader) {
        if (teacher == null) return;

        // ✅ FIX: ensures assigned course count is correct everywhere
        if (reloadFromLoader) {
            Loader.reloadAll();
            Teacher updated = Loader.teacherList.searchTeacher(teacher.getId());
            if (updated != null) teacher = updated;
        }

        teacherNameTop.setText(teacher.getName());
        nameValue.setText(teacher.getName());
        idValue.setText(String.valueOf(teacher.getId()));

        int total = safeCountAssignedCourses(teacher);
        courseCountValue.setText(String.valueOf(total));
    }

    private int safeCountAssignedCourses(Teacher t) {
        if (t == null) return 0;

        String[] candidates = {
                "getCoursesAssigned",     // ✅ YOUR REAL METHOD
                "getAssignedCourses",
                "getCourses",
                "getCourseList",
                "getAssignedCourseList"
        };

        for (String mName : candidates) {
            try {
                Method m = t.getClass().getMethod(mName);
                Object result = m.invoke(t);
                if (result instanceof Collection<?> c) return c.size();
            } catch (Exception ignored) {}
        }
        return 0;
    }

    // ================= EDIT FLOW =================

    @FXML
    private void onEdit(ActionEvent e) {
        if (teacher == null) return;

        nameField.setText(teacher.getName());
        newPasswordField.clear();
        confirmPasswordField.clear();
        setEditStatus("", true);
        setEditVisible(true);
    }

    @FXML
    private void onCancelEdit(ActionEvent e) {
        setEditVisible(false);
        setEditStatus("", true);
    }

    @FXML
    private void onSaveProfile(ActionEvent e) {
        if (teacher == null) return;

        String newName = safe(nameField.getText());
        String newPass = safe(newPasswordField.getText());
        String confirm = safe(confirmPasswordField.getText());

        if (newName.isEmpty()) {
            setEditStatus("Name cannot be empty.", false);
            return;
        }

        // password optional: only validate if user typed something
        if (!newPass.isEmpty() || !confirm.isEmpty()) {
            if (newPass.length() < 4) {
                setEditStatus("Password must be 4+ characters.", false);
                return;
            }
            if (!newPass.equals(confirm)) {
                setEditStatus("Passwords do not match.", false);
                return;
            }
        }

        boolean ok = updateTeacherCredentialLine(
                teacher.getId(),
                newName,
                newPass.isEmpty() ? null : newPass
        );

        if (!ok) {
            setEditStatus("❌ Save failed. Could not update " + TEACHER_CRED_PATH, false);
            return;
        }

        // Update in-memory object (so UI updates immediately)
        teacher.setName(newName);
        if (!newPass.isEmpty()) teacher.setPassword(newPass);

        // refresh from loader to keep everything consistent across pages
        refreshUI(true);

        setEditVisible(false);
        setEditStatus("✅ Saved successfully.", true);
    }

    /**
     * Updates teacher line in: id,name,password,approved
     * Keeps approved flag unchanged.
     */
    private boolean updateTeacherCredentialLine(int teacherId, String newName, String newPasswordOrNull) {
        File f = new File(TEACHER_CRED_PATH);
        if (!f.exists()) return false;

        List<String> lines;
        try {
            lines = Files.readAllLines(f.toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        boolean updated = false;
        List<String> out = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split(",", 4);
            if (parts.length < 4) {
                out.add(line);
                continue;
            }

            int id;
            try {
                id = Integer.parseInt(parts[0].trim());
            } catch (Exception ex) {
                out.add(line);
                continue;
            }

            if (id != teacherId) {
                out.add(line);
                continue;
            }

            // matched teacher row
            String approved = parts[3].trim();
            String oldPass = parts[2].trim();

            String passToWrite = (newPasswordOrNull == null) ? oldPass : newPasswordOrNull;

            out.add(teacherId + "," + newName + "," + passToWrite + "," + approved);
            updated = true;
        }

        if (!updated) return false;

        // overwrite file (atomic-ish)
        try {
            Path tmp = Path.of(TEACHER_CRED_PATH + ".tmp");
            Files.write(tmp, out);
            Files.move(tmp, f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void setEditVisible(boolean v) {
        if (editBox == null) return;
        editBox.setVisible(v);
        editBox.setManaged(v);
    }

    private void setEditStatus(String msg, boolean ok) {
        if (editStatusLabel == null) return;
        editStatusLabel.setText(msg);
        editStatusLabel.setStyle(ok
                ? "-fx-text-fill:#2e7d32; -fx-font-weight:700;"
                : "-fx-text-fill:#c62828; -fx-font-weight:700;");
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ================= NAV =================

    @FXML
    private void onHome(ActionEvent e) {
        switchTo(e, "/fxml/TeacherDashboard.fxml", "Teacher Dashboard", 1200, 750, true);
    }

    @FXML
    private void onCourses(ActionEvent e) {
        switchTo(e, "/fxml/AssignedCourses.fxml", "Assigned Courses", 1200, 750, true);
    }

    @FXML
    private void onLogout(ActionEvent e) {
        switchTo(e, "/fxml/login.fxml", "Login", 1200, 750, false);
    }

    private void switchTo(ActionEvent e, String fxml, String title, int w, int h, boolean resizable) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            passTeacherToController(controller, teacher);

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.setMinWidth(w);
            stage.setMinHeight(h);
            stage.setResizable(resizable);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void passTeacherToController(Object controller, Teacher teacher) {
        if (controller == null || teacher == null) return;

        String[] setters = {"setTeacher", "passTeacher", "setCurrentTeacher"};
        for (String s : setters) {
            try {
                Method m = controller.getClass().getMethod(s, Teacher.class);
                m.invoke(controller, teacher);
                return;
            } catch (Exception ignored) {}
        }
    }
}
