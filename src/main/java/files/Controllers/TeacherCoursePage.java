package files.Controllers;

import files.Classes.Course;
import files.Classes.Student;
import files.Classes.Teacher;
import files.Main;
import files.Server.Deadline;
import files.Server.FilePacket;
import files.Server.Notification;
import files.Server.SocketWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TeacherCoursePage {

    private static final String ANNOUNCEMENT_FILE = "database/CourseAnnouncements.txt";

    // ✅ your format remains: courseId;fileName
    private static final String UPLOAD_RECORD_FILE = "database/UploadedFiles.txt";

    // ✅ permanent file storage
    private static final String UPLOAD_BASE_DIR = "database/uploads";

    // ===== FXML =====
    @FXML private Label Name;
    @FXML private Button homeButton;
    @FXML private Button logoutButton;

    @FXML private Label welcomeLabel;

    @FXML private TextArea t;
    @FXML private Button post;
    @FXML private Button filePost;
    @FXML private Label attachedFileName;

    @FXML private TextField taskField;
    @FXML private ComboBox<String> typeBox;
    @FXML private DatePicker dueDatePicker;

    @FXML private TableView<Student> participantsTable;
    @FXML private TableColumn<Student, Integer> studentIdColumn;
    @FXML private TableColumn<Student, String> studentNameColumn;

    @FXML private VBox announcementBox;

    // ✅ Files UI
    @FXML private VBox uploadedFilesBox;
    @FXML private Label filesStatusLabel;

    // ===== DATA =====
    private Teacher teacher;
    private Course course;
    private List<Student> students = new ArrayList<>();

    private File selectedFile;
    private SocketWrapper socketWrapper;

    // ================== INIT ==================
    @FXML
    public void initialize() {
        // table setup
        if (studentIdColumn != null) studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
        if (studentNameColumn != null) studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (participantsTable != null) participantsTable.setItems(FXCollections.observableArrayList());

        // combobox items
        if (typeBox != null) typeBox.getItems().setAll("Assignment", "Quiz", "Lab Report", "CT", "Project");

        if (attachedFileName != null) attachedFileName.setText("No file selected");
        if (welcomeLabel != null) welcomeLabel.setText("");
        if (Name != null) Name.setText("");
        if (filesStatusLabel != null) filesStatusLabel.setText("");
    }

    // ================== SETTERS ==================
    public void setSocketWrapper(SocketWrapper socketWrapper) {
        this.socketWrapper = socketWrapper;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        if (teacher != null && Name != null) Name.setText(teacher.getName());
    }

    public void setCourse(Course course) {
        this.course = course;
        if (course == null) {
            students = new ArrayList<>();
        } else {
            students = new ArrayList<>(course.getCourseStudents());
        }
    }

    /** ✅ Call after setTeacher + setCourse (+ setSocketWrapper) */
    public void display() {
        if (course == null) {
            if (welcomeLabel != null) welcomeLabel.setText("Course not set");
            if (participantsTable != null) participantsTable.getItems().clear();
            if (announcementBox != null) {
                announcementBox.getChildren().clear();
                announcementBox.getChildren().add(new Label("Course not set."));
            }
            if (uploadedFilesBox != null) uploadedFilesBox.getChildren().clear();
            return;
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText(course.getCourseID() + " " + course.getCourseName());
        }

        if (participantsTable != null) {
            ObservableList<Student> stdList = FXCollections.observableArrayList(students);
            participantsTable.setItems(stdList);
        }

        refreshAll(null);
    }

    // ===================== REFRESH =====================
    @FXML
    public void refreshAll(ActionEvent e) {
        loadAnnouncements();
        loadUploadedFiles();
        if (filesStatusLabel != null) filesStatusLabel.setText("Refreshed ✅");
    }

    // ===================== ANNOUNCEMENTS =====================
    private void loadAnnouncements() {
        if (announcementBox == null) return;

        announcementBox.getChildren().clear();

        if (course == null) {
            announcementBox.getChildren().add(new Label("Course not set."));
            return;
        }

        File file = new File(ANNOUNCEMENT_FILE);
        if (!file.exists()) {
            Label none = new Label("No announcements yet.");
            none.getStyleClass().add("muted-label");
            announcementBox.getChildren().add(none);
            return;
        }

        boolean found = false;
        String courseId = course.getCourseID().trim();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // split limit=4
                String[] parts = line.split(";", 4);

                if (parts.length == 4 && parts[0].trim().equals(courseId)) {
                    found = true;

                    String teacherName = parts[1].trim();
                    String msg = parts[2].trim();
                    String time = parts[3].trim();

                    Label lbl = new Label("• " + teacherName + ": " + msg + "  (" + time + ")");
                    lbl.setWrapText(true);
                    lbl.getStyleClass().add("announce-item"); // optional CSS
                    announcementBox.getChildren().add(lbl);
                }
            }
        } catch (IOException e) {
            Label err = new Label("Failed to load announcements.");
            err.setStyle("-fx-text-fill: red;");
            announcementBox.getChildren().add(err);
            return;
        }

        if (!found) {
            Label none = new Label("No announcements for this course yet.");
            none.getStyleClass().add("muted-label");
            announcementBox.getChildren().add(none);
        }
    }

    // ===================== POST ANNOUNCEMENT =====================
    @FXML
    public void onPost(ActionEvent actionEvent) {
        if (course == null || teacher == null) {
            showAlert("❗ Course/Teacher not set.");
            return;
        }

        String message = (t == null) ? "" : t.getText().trim();
        if (message.isEmpty()) {
            showAlert("❗ Write something first.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String courseId = course.getCourseID().trim();
        String teacherName = teacher.getName().trim();

        String line = courseId + ";" + teacherName + ";" + message + ";" + now;

        // 1) save announcement
        try {
            File file = new File(ANNOUNCEMENT_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("❌ Failed to save announcement.");
            return;
        }

        // 2) if file selected -> save permanently + record it (2 fields)
        if (selectedFile != null) {
            try {
                saveFileToCourseFolder(courseId, selectedFile);
                addUploadRecordIfMissing(courseId, selectedFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("⚠️ File save failed!");
            }
        }

        // 3) send notification to server (optional)
        if (socketWrapper != null) {
            try {
                socketWrapper.write(new Notification(line));
            } catch (IOException e) {
                System.out.println("⚠️ Could not send notification: " + e.getMessage());
            }
        }

        // 4) upload to server (optional)
        if (selectedFile != null && socketWrapper != null) {
            try (FileInputStream fis = new FileInputStream(selectedFile)) {
                byte[] fileData = fis.readAllBytes();
                FilePacket packet = new FilePacket(courseId, selectedFile.getName(), fileData);
                socketWrapper.write(packet);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("⚠️ File upload to server failed (saved locally ✅).");
            }
        }

        // reset UI
        if (t != null) t.clear();
        selectedFile = null;
        if (attachedFileName != null) attachedFileName.setText("No file selected");

        loadAnnouncements();
        loadUploadedFiles();
        showAlert("✅ Announcement Posted!");
    }

    // ===================== FILE PICKER =====================
    @FXML
    public void onFilepost(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        Stage stage = (Stage) ((filePost != null) ? filePost.getScene().getWindow() : null);
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null && attachedFileName != null) {
            attachedFileName.setText(selectedFile.getName());
        }
    }

    // ===================== UPLOADED FILES: LOAD + UI =====================
    private void loadUploadedFiles() {
        if (uploadedFilesBox == null) return;

        uploadedFilesBox.getChildren().clear();

        if (course == null) return;

        File record = new File(UPLOAD_RECORD_FILE);
        if (!record.exists()) {
            Label none = new Label("No uploaded files yet.");
            none.getStyleClass().add("muted-label");
            uploadedFilesBox.getChildren().add(none);
            return;
        }

        String courseId = course.getCourseID().trim();
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(record))) {
            String line;
            while ((line = br.readLine()) != null) {
                // format: courseId;fileName
                String[] parts = line.split(";", 2);
                if (parts.length < 2) continue;

                String cid = parts[0].trim();
                String fname = parts[1].trim();

                if (!cid.equals(courseId)) continue;

                found = true;
                uploadedFilesBox.getChildren().add(buildFileRow(courseId, fname));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Label err = new Label("Failed to load uploaded files.");
            err.setStyle("-fx-text-fill: red;");
            uploadedFilesBox.getChildren().add(err);
        }

        if (!found) {
            Label none = new Label("No uploaded files for this course yet.");
            none.getStyleClass().add("muted-label");
            uploadedFilesBox.getChildren().add(none);
        }
    }

    // Replace Open with Save (Replace) + Remove
    private HBox buildFileRow(String courseId, String fileName) {
        HBox row = new HBox(12);
        row.getStyleClass().add("file-row");
        row.setPadding(new Insets(10));

        Label name = new Label(fileName);
        name.getStyleClass().add("file-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = new Button("Save (Replace)");
        saveBtn.getStyleClass().addAll("action-button", "green-button");
        saveBtn.setOnAction(e -> replaceSavedFile(courseId, fileName));

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().addAll("action-button", "red-button");
        removeBtn.setOnAction(e -> removeSavedFile(courseId, fileName));

        row.getChildren().addAll(name, spacer, saveBtn, removeBtn);
        return row;
    }

    private File getStoredFile(String courseId, String fileName) {
        return new File(UPLOAD_BASE_DIR + "/" + courseId + "/" + fileName);
    }

    private void saveFileToCourseFolder(String courseId, File sourceFile) throws IOException {
        File dir = new File(UPLOAD_BASE_DIR + "/" + courseId);
        dir.mkdirs();

        File target = new File(dir, sourceFile.getName());
        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(target)) {
            in.transferTo(out);
        }
    }

    private void addUploadRecordIfMissing(String courseId, String fileName) throws IOException {
        File record = new File(UPLOAD_RECORD_FILE);
        record.getParentFile().mkdirs();
        record.createNewFile();

        List<String> lines = java.nio.file.Files.readAllLines(record.toPath());
        String key = courseId + ";" + fileName;

        for (String line : lines) {
            if (line.trim().equals(key)) return;
        }

        try (FileWriter writer = new FileWriter(record, true)) {
            writer.write(key + "\n");
        }
    }

    private void replaceSavedFile(String courseId, String oldFileName) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose file to save (replace)");
        File newFile = fc.showOpenDialog(uploadedFilesBox.getScene().getWindow());
        if (newFile == null) return;

        try {
            // delete old stored file
            File oldStored = getStoredFile(courseId, oldFileName);
            if (oldStored.exists()) oldStored.delete();

            // save new file to course folder
            saveFileToCourseFolder(courseId, newFile);

            // update record if filename changed
            if (!newFile.getName().equals(oldFileName)) {
                updateUploadRecordName(courseId, oldFileName, newFile.getName());
            }

            loadUploadedFiles();
            showAlert("✅ File saved (replaced).");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("❌ Replace failed.");
        }
    }

    private void removeSavedFile(String courseId, String fileName) {
        try {
            // delete stored file
            File stored = getStoredFile(courseId, fileName);
            if (stored.exists()) stored.delete();

            // remove record line
            removeUploadRecord(courseId, fileName);

            loadUploadedFiles();
            showAlert("✅ File removed.");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("❌ Remove failed.");
        }
    }

    private void updateUploadRecordName(String courseId, String oldName, String newName) throws IOException {
        File record = new File(UPLOAD_RECORD_FILE);
        if (!record.exists()) return;

        List<String> lines = java.nio.file.Files.readAllLines(record.toPath());
        String oldLine = courseId + ";" + oldName;
        String newLine = courseId + ";" + newName;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(oldLine)) {
                lines.set(i, newLine);
                break;
            }
        }
        java.nio.file.Files.write(record.toPath(), lines);
    }

    private void removeUploadRecord(String courseId, String fileName) throws IOException {
        File record = new File(UPLOAD_RECORD_FILE);
        if (!record.exists()) return;

        List<String> lines = java.nio.file.Files.readAllLines(record.toPath());
        String target = courseId + ";" + fileName;

        lines.removeIf(l -> l.trim().equals(target));
        java.nio.file.Files.write(record.toPath(), lines);
    }

    // ===================== DEADLINE UPLOAD =====================
    @FXML
    public void uploadDeadline(ActionEvent actionEvent) {
        if (course == null) {
            showAlert("❗ Course not set.");
            return;
        }
        if (socketWrapper == null) {
            showAlert("❗ Server not connected. Start NotificationServer first.");
            return;
        }

        String task = taskField.getText().trim();
        String type = typeBox.getValue();
        LocalDate dueDate = dueDatePicker.getValue();

        if (task.isEmpty() || type == null || dueDate == null) {
            showAlert("❗ Please fill in all fields before uploading.");
            return;
        }

        Deadline deadline = new Deadline(course.getCourseID().trim(), task, type, dueDate);

        new Thread(() -> {
            try {
                socketWrapper.write(deadline);
                Object response = socketWrapper.read();

                Platform.runLater(() -> {
                    if ("DEADLINE_SAVED".equals(response)) {
                        showAlert("✅ Deadline uploaded successfully!");
                        taskField.clear();
                        typeBox.getSelectionModel().clearSelection();
                        dueDatePicker.setValue(null);
                    } else {
                        showAlert("⚠️ Server failed to save deadline.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("❌ Error sending deadline to server."));
            }
        }, "UploadDeadlineThread").start();
    }

    // ===================== NAV =====================
    @FXML
    public void onHomeClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/TeacherDashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            TeacherDashboardController controller = fxmlLoader.getController();
            controller.setTeacher(teacher);

            Stage stage = (Stage) homeButton.getScene().getWindow();
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.setTitle("Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onLogout(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===================== ALERT =====================
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
