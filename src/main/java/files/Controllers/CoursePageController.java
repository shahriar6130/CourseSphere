package files.Controllers;

import files.Classes.Course;
import files.Classes.Student;
import files.Main;
import files.Server.Deadline;
import files.Server.Notification;
import files.Server.SocketWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CoursePageController implements Initializable {

    private static final String ANNOUNCEMENT_FILE = "database/CourseAnnouncements.txt";
    private static final String UPLOAD_RECORD_FILE = "database/UploadedFiles.txt";
    private static final String DEADLINE_FILE = "database/deadlines.txt";

    // ===== FXML =====
    @FXML private Label courseName;
    @FXML private Label creditLOabel;

    @FXML private Button logout;
    @FXML private Button courses;
    @FXML private Button home;

    @FXML private TableView<Student> participantsTable;
    @FXML private TableColumn<Student, Integer> studentIdColumn;
    @FXML private TableColumn<Student, String> studentNameColumn;

    @FXML private Label announcementToggle;
    @FXML private VBox announcementBox;

    @FXML private VBox fileListBox;
    @FXML private VBox deadlineContainer;

    // ===== DATA =====
    private Course course;
    private Student student;
    private SocketWrapper socketWrapper;

    private final Set<String> shownAnnouncements = new HashSet<>();
    private volatile boolean listening = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (studentIdColumn != null) studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));
        if (studentNameColumn != null) studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // announcements hidden by default
        if (announcementBox != null) {
            announcementBox.setVisible(false);
            announcementBox.setManaged(false);
        }
        if (announcementToggle != null) {
            announcementToggle.setText("📢 Show Announcements");
        }
    }

    // ===================== SETTERS =====================

    public void setCourse(Course course) {
        this.course = course;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public void setSocketWrapper(SocketWrapper socketWrapper) {
        this.socketWrapper = socketWrapper;
        startListening();
    }

    /** Call after setCourse + setStudent (+ setSocketWrapper) */
    public void display() {
        if (course == null) return;

        if (courseName != null) courseName.setText(course.getCourseID() + "  " + course.getCourseName());
        if (creditLOabel != null) creditLOabel.setText("Total Credits: " + course.getCredit());

        if (participantsTable != null) {
            ObservableList<Student> students = FXCollections.observableArrayList(course.getCourseStudents());
            participantsTable.setItems(students);
        }

        refreshAllInternal();
    }

    // ===================== REFRESH =====================

    @FXML
    public void refreshAll(ActionEvent e) {
        refreshAllInternal();
    }

    private void refreshAllInternal() {
        // IMPORTANT: full reload should re-show existing announcements
        shownAnnouncements.clear();

        loadAnnouncementsFromFileAsync();
        loadUploadedFilesAsync();
        loadUpcomingDeadlinesAsync();
    }

    // ===================== ANNOUNCEMENTS =====================

    private void loadAnnouncementsFromFileAsync() {
        new Thread(() -> {
            if (course == null) return;

            List<String> lines = new ArrayList<>();
            File f = new File(ANNOUNCEMENT_FILE);
            if (!f.exists()) {
                Platform.runLater(() -> {
                    if (announcementBox != null) announcementBox.getChildren().clear();
                });
                return;
            }

            String courseId = course.getCourseID().trim();

            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {

                    // ✅ safe split
                    String[] parts = line.split(";", 4);
                    if (parts.length == 4 && parts[0].trim().equals(courseId)) {
                        String teacher = parts[1].trim();
                        String msg = parts[2].trim();
                        String time = parts[3].trim();

                        lines.add("• " + teacher + ": " + msg + "  (" + time + ")");
                    }
                }
            } catch (IOException ignored) {}

            Platform.runLater(() -> {
                if (announcementBox == null) return;

                announcementBox.getChildren().clear();

                if (lines.isEmpty()) {
                    Label none = new Label("No announcements yet.");
                    none.getStyleClass().add("muted-label");
                    announcementBox.getChildren().add(none);
                    return;
                }

                for (String text : lines) {
                    Label lbl = new Label(text);
                    lbl.setWrapText(true);
                    lbl.getStyleClass().add("announce-item");
                    announcementBox.getChildren().add(lbl);

                    // keep set in sync so live socket messages don't duplicate
                    shownAnnouncements.add(text);
                }
            });

        }, "LoadAnnouncementsThread").start();
    }

    private void startListening() {
        if (socketWrapper == null || listening) return;
        listening = true;

        Thread thread = new Thread(() -> {
            try {
                while (listening) {
                    Object o = socketWrapper.read();
                    if (!(o instanceof Notification notification)) continue;
                    if (course == null) continue;

                    // ✅ safe split
                    String[] parts = notification.getNotification().split(";", 4);
                    if (parts.length != 4) continue;

                    String courseId = parts[0].trim();
                    if (!courseId.equals(course.getCourseID().trim())) continue;

                    String teacher = parts[1].trim();
                    String msg = parts[2].trim();
                    String time = parts[3].trim();

                    String text = "• " + teacher + ": " + msg + "  (" + time + ")";

                    Platform.runLater(() -> {
                        if (announcementBox == null) return;

                        if (shownAnnouncements.add(text)) {
                            Label lbl = new Label(text);
                            lbl.setWrapText(true);
                            lbl.getStyleClass().add("announce-item");
                            announcementBox.getChildren().add(0, lbl); // newest on top
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println("Client read error: " + e.getMessage());
            }
        }, "AnnouncementListenThread");

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void toggleAnnouncements(MouseEvent mouseEvent) {
        if (announcementBox == null || announcementToggle == null) return;

        boolean nowVisible = !announcementBox.isVisible();
        announcementBox.setVisible(nowVisible);
        announcementBox.setManaged(nowVisible);
        announcementToggle.setText(nowVisible ? "📢 Hide Announcements" : "📢 Show Announcements");
    }

    // ===================== FILES =====================

    private void loadUploadedFilesAsync() {
        new Thread(() -> {
            if (course == null) return;

            String courseId = course.getCourseID().trim();
            List<HBox> rows = new ArrayList<>();

            File record = new File(UPLOAD_RECORD_FILE);
            if (!record.exists()) {
                Platform.runLater(() -> {
                    if (fileListBox != null) fileListBox.getChildren().clear();
                });
                return;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(record))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(";", 2);
                    if (parts.length != 2) continue;

                    if (!parts[0].trim().equals(courseId)) continue;
                    String filename = parts[1].trim();

                    Label name = new Label(filename);
                    name.getStyleClass().add("file-name");

                    Button openBtn = new Button("Open");
                    openBtn.getStyleClass().addAll("action-button");
                    openBtn.setOnAction(e ->
                            openFile(new File("uploaded_files/" + courseId + "/" + filename))
                    );

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    HBox row = new HBox(12, name, spacer, openBtn);
                    row.getStyleClass().add("file-row");
                    rows.add(row);
                }
            } catch (IOException ignored) {}

            Platform.runLater(() -> {
                if (fileListBox == null) return;

                fileListBox.getChildren().clear();

                if (rows.isEmpty()) {
                    Label none = new Label("No files uploaded yet.");
                    none.getStyleClass().add("muted-label");
                    fileListBox.getChildren().add(none);
                } else {
                    fileListBox.getChildren().addAll(rows);
                }
            });

        }, "LoadFilesThread").start();
    }

    private void openFile(File file) {
        try {
            if (!file.exists()) return;
            if (!Desktop.isDesktopSupported()) return;
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===================== DEADLINES =====================

    private void loadUpcomingDeadlinesAsync() {
        new Thread(() -> {
            if (course == null) return;

            List<Deadline> deadlines = new ArrayList<>();
            File f = new File(DEADLINE_FILE);

            if (!f.exists()) {
                Platform.runLater(() -> showDeadlines(deadlines));
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(f.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length != 4) continue;

                    String courseId = parts[0].trim();
                    if (!course.getCourseID().trim().equals(courseId)) continue;

                    String taskName = parts[1].trim();
                    String type = parts[2].trim();
                    LocalDate dueDate = LocalDate.parse(parts[3].trim());

                    deadlines.add(new Deadline(courseId, taskName, type, dueDate));
                }
            } catch (Exception ignored) {}

            Platform.runLater(() -> showDeadlines(deadlines));
        }, "LoadDeadlinesThread").start();
    }

    private void showDeadlines(List<Deadline> deadlines) {
        if (deadlineContainer == null) return;
        deadlineContainer.getChildren().clear();

        if (deadlines == null || deadlines.isEmpty()) {
            Label noDeadlineLabel = new Label("No upcoming deadlines found.");
            noDeadlineLabel.getStyleClass().add("muted-label");
            deadlineContainer.getChildren().add(noDeadlineLabel);
            return;
        }

        // optional: sort nearest first
        deadlines.sort(Comparator.comparing(Deadline::getDueDate));

        for (Deadline d : deadlines) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), d.getDueDate());

            String status;
            String color;

            if (daysLeft < 0) {
                status = "⛔ Overdue";
                color = "#7f8c8d";
            } else if (daysLeft == 0) {
                status = "🔥 Due Today";
                color = "#e74c3c";
            } else if (daysLeft <= 2) {
                status = "⚠️ Due in " + daysLeft + " day(s)";
                color = "#f39c12";
            } else {
                status = "✅ Due in " + daysLeft + " day(s)";
                color = "#27ae60";
            }

            Label label = new Label(d.getTaskName() + " (" + d.getType() + ") – " + status);
            label.setStyle("-fx-background-color: " + color +
                    "; -fx-background-radius: 10; -fx-padding: 10px; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700;");
            label.setWrapText(true);

            deadlineContainer.getChildren().add(label);
        }
    }

    // ===================== NAVIGATION =====================

    @FXML
    private void onHome(ActionEvent actionEvent) {
        stopListeningAndCloseSocket();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setCurrentStudent(student);

            Stage stage = (Stage) home.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void Oncourses(ActionEvent actionEvent) {
        stopListeningAndCloseSocket();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Student/StudentCourses.fxml"));
            Parent root = loader.load();

            StudentCoursesController controller = loader.getController();
            controller.passStudent(student);

            Stage stage = (Stage) courses.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Courses");
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogout(ActionEvent actionEvent) {
        stopListeningAndCloseSocket();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage stage = (Stage) logout.getScene().getWindow();
            stage.setResizable(false);
            stage.setTitle("Course Management System");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopListeningAndCloseSocket() {
        listening = false;
        if (socketWrapper != null) {
            try {
                socketWrapper.closeConnection();
            } catch (Exception ignored) {}
        }
    }
}
