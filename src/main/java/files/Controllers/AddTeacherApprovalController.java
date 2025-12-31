package files.Controllers;

import files.Classes.Loader;
import files.Classes.Teacher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class AddTeacherApprovalController {

    private static final String FILE_PATH = "database/TeacherCredentials.txt";

    @FXML private TableView<Teacher> pendingTeacherTable;
    @FXML private TableColumn<Teacher, String> nameColumn;
    @FXML private TableColumn<Teacher, Integer> idColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Teacher> pendingTeachers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        pendingTeacherTable.getColumns().forEach(col -> col.setReorderable(false));
        pendingTeacherTable.getColumns().forEach(col -> col.setResizable(false));

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        // because your Person/Teacher uses getID()
        idColumn.setCellValueFactory(new PropertyValueFactory<>("ID"));

        pendingTeacherTable.setItems(pendingTeachers);

        setStatus("Loading pending teachers...", true);
        loadPendingTeachersAsync();
    }

    // ================= LOAD =================

    private void loadPendingTeachersAsync() {
        new Thread(() -> {
            List<Teacher> loaded = loadPendingTeachersFromFile();
            Platform.runLater(() -> {
                pendingTeachers.setAll(loaded);
                setStatus("Loaded " + pendingTeachers.size() + " pending teacher(s).", true);
            });
        }).start();
    }

    private List<Teacher> loadPendingTeachersFromFile() {
        List<Teacher> result = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    boolean approved = Boolean.parseBoolean(parts[3].trim());
                    if (!approved) {
                        int id = Integer.parseInt(parts[0].trim());
                        if (seenIds.add(id)) {
                            String name = parts[1].trim();
                            String pass = parts[2].trim();
                            result.add(new Teacher(name, id, pass));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> setStatus("Failed to load: " + e.getMessage(), false));
        }

        return result;
    }

    // ================= APPROVE =================

    @FXML
    private void approveSelected() {
        Teacher selected = pendingTeacherTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a teacher first.", false);
            return;
        }

        if (!confirm("Approve Teacher",
                "Approve " + selected.getName() + " (" + selected.getID() + ")?")) return;

        runUpdateAsync(Collections.singletonList(selected), true);
    }

    @FXML
    private void approveAll() {
        if (pendingTeachers.isEmpty()) {
            setStatus("No pending teachers to approve.", false);
            return;
        }

        if (!confirm("Approve All",
                "Approve ALL pending teachers (" + pendingTeachers.size() + ")?")) return;

        runUpdateAsync(new ArrayList<>(pendingTeachers), true);
    }

    // ================= DELETE =================

    @FXML
    private void deleteSelected() {
        Teacher selected = pendingTeacherTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a teacher first.", false);
            return;
        }

        if (!confirm("Delete Teacher",
                "Delete " + selected.getName() + " (" + selected.getID() + ") from pending list?")) return;

        runUpdateAsync(Collections.singletonList(selected), false);
    }

    @FXML
    private void deleteAll() {
        if (pendingTeachers.isEmpty()) {
            setStatus("No pending teachers to delete.", false);
            return;
        }

        if (!confirm("Delete All",
                "Delete ALL pending teachers (" + pendingTeachers.size() + ")? This cannot be undone.")) return;

        runUpdateAsync(new ArrayList<>(pendingTeachers), false);
    }

    // ================= CORE UPDATE =================

    /**
     * approve=true  => sets approved flag true
     * approve=false => deletes from file
     */
    private void runUpdateAsync(List<Teacher> target, boolean approve) {
        setStatus(approve ? "Approving..." : "Deleting...", true);

        new Thread(() -> {
            boolean ok = updateFile(target, approve);

            Platform.runLater(() -> {
                if (!ok) {
                    setStatus("Operation failed. Try again.", false);
                    return;
                }

                // Update in-memory list
                if (approve) {
                    for (Teacher t : target) Loader.teacherList.addTeacher(t);
                    setStatus("Approved successfully ✅", true);
                } else {
                    for (Teacher t : target) Loader.teacherList.removeTeacher(t);
                    setStatus("Deleted successfully 🗑", true);
                }

                // Update UI list
                pendingTeachers.removeAll(target);
            });
        }).start();
    }

    private boolean updateFile(List<Teacher> target, boolean approve) {
        Path path = Paths.get(FILE_PATH);

        Set<Integer> ids = new HashSet<>();
        for (Teacher t : target) ids.add(t.getID());

        try {
            List<String> lines = Files.exists(path) ? Files.readAllLines(path) : new ArrayList<>();
            List<String> updated = new ArrayList<>();

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    updated.add(line);
                    continue;
                }

                int id;
                try {
                    id = Integer.parseInt(parts[0].trim());
                } catch (NumberFormatException e) {
                    updated.add(line);
                    continue;
                }

                if (!ids.contains(id)) {
                    updated.add(line);
                    continue;
                }

                if (approve) {
                    parts[3] = "true";
                    updated.add(String.join(",", parts));
                }
                // else delete => skip
            }

            Files.write(path, updated, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    // ================= HELPERS =================

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill: #1b5e20; -fx-font-weight: bold;"
                : "-fx-text-fill: #b71c1c; -fx-font-weight: bold;"
        );
    }
}
