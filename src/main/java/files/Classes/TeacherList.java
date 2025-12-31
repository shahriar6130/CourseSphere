package files.Classes;

import java.io.Serializable;
import java.util.*;

public class TeacherList implements Serializable {

    private final List<Teacher> teachers = new ArrayList<>();

    public List<Teacher> getTeachers() {
        return Collections.unmodifiableList(teachers);
    }

    public boolean addTeacher(Teacher t) {
        if (t == null) return false;
        if (searchTeacher(t.getId()) != null) return false;
        teachers.add(t);
        return true;
    }

    // ✅ ADD THIS (used by controllers)
    public boolean removeTeacher(Teacher t) {
        if (t == null) return false;
        return teachers.removeIf(existing -> existing.getId() == t.getId());
    }

    // Keep this if you want ID-based removal
    public boolean removeTeacherById(int id) {
        return teachers.removeIf(existing -> existing.getId() == id);
    }

    public Teacher searchTeacher(int enteredId) {
        for (Teacher t : teachers) {
            if (t.getId() == enteredId) return t;
        }
        return null;
    }

    @Override
    public String toString() {
        return "TeacherList{count=" + teachers.size() + "}";
    }
}
