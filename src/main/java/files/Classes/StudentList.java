package files.Classes;

import java.io.Serializable;
import java.util.*;

public class StudentList implements Serializable {

    private final List<Student> students = new ArrayList<>();

    public List<Student> getStudents() {
        return Collections.unmodifiableList(students);
    }

    public boolean addStudent(Student s) {
        if (s == null) return false;
        if (searchStudent(s.getId()) != null) return false;
        students.add(s);
        return true;
    }

    // ✅ ADD THIS (to match TeacherList)
    public boolean removeStudent(Student s) {
        if (s == null) return false;
        return students.removeIf(existing -> existing.getId() == s.getId());
    }

    // keep ID-based removal if needed
    public boolean removeStudentById(int id) {
        return students.removeIf(existing -> existing.getId() == id);
    }

    public Student searchStudent(int enteredId) {
        for (Student s : students) {
            if (s.getId() == enteredId) return s;
        }
        return null;
    }

    // useful for JavaFX filtering
    public List<Student> searchByName(String text) {
        if (text == null) return List.of();
        String q = text.trim().toLowerCase();
        List<Student> result = new ArrayList<>();
        for (Student s : students) {
            if (s.getName().toLowerCase().contains(q)) result.add(s);
        }
        return result;
    }

    @Override
    public String toString() {
        return "StudentList{count=" + students.size() + "}";
    }
}
