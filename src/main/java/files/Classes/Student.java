package files.Classes;

import java.io.Serializable;
import java.util.*;

public class Student extends Person implements Serializable {

    private final List<Course> courses = new ArrayList<>();
    private String imagePath; // can be null

    public Student(String studentName, int studentId, String stdPass) {
        super(studentName, studentId, stdPass);
    }

    // ================= IMAGE =================

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = (imagePath == null || imagePath.isBlank())
                ? null
                : imagePath.trim();
    }

    // ================= ID COMPATIBILITY =================
    // 🔑 IMPORTANT: keeps old + new controllers working

    public int getID() {
        return getId();
    }

    // ================= COURSES =================

    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    public boolean enroll(Course c) {
        if (c == null) return false;
        if (!courses.contains(c)) {
            courses.add(c);
            return true;
        }
        return false;
    }

    public boolean drop(Course c) {
        if (c == null) return false;
        return courses.remove(c);
    }

    // Called from Course.addStudent (bidirectional)
    public void addCourses(Course c) {
        enroll(c);
    }

    public int totalCredits() {
        double sum = 0;
        for (Course c : courses) {
            sum += c.getCredit();
        }
        return (int) Math.round(sum);
    }

    // ================= PASSWORD (clarity override) =================

    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
    }

    // ================= DISPLAY =================

    @Override
    public String toString() {
        return getID() + " - " + getName();
    }
}
