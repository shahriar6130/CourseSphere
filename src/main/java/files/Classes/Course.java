package files.Classes;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Course implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String courseName;
    private final String courseID;
    private double credit;

    private final List<Student> courseStudents = new ArrayList<>();
    private final List<Teacher> courseTeachers = new ArrayList<>();

    public Course(String courseID, String courseName, double credit){
        if (courseID == null || courseID.isBlank()) throw new IllegalArgumentException("courseID required");
        if (courseName == null || courseName.isBlank()) throw new IllegalArgumentException("courseName required");
        if (credit <= 0) throw new IllegalArgumentException("credit must be > 0");

        this.courseID = courseID.trim();
        this.courseName = courseName.trim();
        this.credit = credit;
    }

    public String getCourseID() { return courseID; }
    public String getCourseName() { return courseName; }
    public double getCredit() { return credit; }

    public void setCourseName(String courseName) {
        if (courseName == null || courseName.isBlank()) return;
        this.courseName = courseName.trim();
    }

    public void setCredit(double credit) {
        if (credit > 0) this.credit = credit;
    }

    public List<Student> getCourseStudents() {
        return Collections.unmodifiableList(courseStudents);
    }

    public List<Teacher> getCourseTeachers() {
        return Collections.unmodifiableList(courseTeachers);
    }

    public boolean addStudent(Student s){
        if (s == null) return false;
        if (!courseStudents.contains(s)) {
            courseStudents.add(s);
            s.addCourses(this);
            return true;
        }
        return false;
    }

    public boolean addTeacher(Teacher t){
        if (t == null) return false;
        if (!courseTeachers.contains(t)) {
            courseTeachers.add(t);
            t.assignCourse(this);
            return true;
        }
        return false;
    }

    public String displayLabel() {
        return courseID + " - " + courseName + " (" + credit + ")";
    }

    @Override
    public String toString() {
        return displayLabel();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Course other)) return false;
        return courseID.equalsIgnoreCase(other.courseID);
    }

    @Override
    public int hashCode() {
        return courseID.toLowerCase().hashCode();
    }
}
