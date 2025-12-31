package files.Server;

import java.io.Serial;
import java.io.Serializable;

public final class GetDeadlinesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String courseId;

    public GetDeadlinesRequest(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId cannot be null/blank");
        }
        this.courseId = courseId.trim();
    }

    public String getCourseId() {
        return courseId;
    }

    @Override
    public String toString() {
        return "GetDeadlinesRequest{courseId='" + courseId + "'}";
    }
}
