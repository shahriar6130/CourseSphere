package files.Classes;

import java.io.Serializable;
import java.util.*;

public class Teacher extends Person implements Serializable {

    private final List<Course> courseAssigned = new ArrayList<>();

    public Teacher(String name, int id, String password) {
        super(name, id, password);
    }

    public List<Course> getCoursesAssigned() {
        return Collections.unmodifiableList(courseAssigned);
    }

    public boolean assignCourse(Course c) {
        if (c == null) return false;
        if (!courseAssigned.contains(c)) {
            courseAssigned.add(c);
            return true;
        }
        return false;
    }

    // called from Course.addTeacher (bidirectional)
    public void assignCourseFromCourse(Course c) {
        assignCourse(c);
    }

    @Override
    public String toString() {
        return getId() + " - " + getName();
    }
}
