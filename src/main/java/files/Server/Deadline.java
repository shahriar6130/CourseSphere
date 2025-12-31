// ✅ files/Server/Deadline.java
package files.Server;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class Deadline implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String courseId;
    private final String taskName;
    private final String type;
    private final LocalDate dueDate;

    public Deadline(String courseId, String taskName, String type, LocalDate dueDate) {
        this.courseId = courseId;
        this.taskName = taskName;
        this.type = type;
        this.dueDate = dueDate;
    }

    public String getCourseId() { return courseId; }
    public String getTaskName() { return taskName; }
    public String getType() { return type; }
    public LocalDate getDueDate() { return dueDate; }

    @Override
    public String toString() {
        return courseId + ";" + taskName + ";" + type + ";" + dueDate;
    }
}
