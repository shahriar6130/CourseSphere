package files.Classes;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class CourseList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Course> courses = new ArrayList<>();

    // ✅ For UI: safe read-only view
    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    // ✅ For loaders: allow clearing safely
    public void clearCourses() {
        courses.clear();
    }

    public Course searchCourse(String courseId) {
        if (courseId == null) return null;
        String key = courseId.trim();
        for (Course c : courses) {
            if (c.getCourseID().equalsIgnoreCase(key)) return c;
        }
        return null;
    }

    // ✅ NEW: search by name (case-insensitive exact match)
    public Course searchCourseByName(String courseName) {
        if (courseName == null) return null;
        String key = courseName.trim().toLowerCase();
        for (Course c : courses) {
            if (c.getCourseName() != null &&
                    c.getCourseName().trim().toLowerCase().equals(key)) {
                return c;
            }
        }
        return null;
    }

    public boolean addCourse(Course c) {
        if (c == null) return false;

        // ✅ block duplicate ID
        if (searchCourse(c.getCourseID()) != null) return false;

        // ✅ block duplicate NAME (even with different ID)
        if (searchCourseByName(c.getCourseName()) != null) return false;

        courses.add(c);
        return true;
    }

    // ✅ Remove from memory only
    public boolean removeCourseById(String courseId) {
        Course c = searchCourse(courseId);
        if (c == null) return false;
        return courses.remove(c);
    }

    // ================= STUDENT =================
    public boolean addStudentToCourse(String courseId, Student student) {
        Course c = searchCourse(courseId);
        if (c == null || student == null) return false;
        return c.addStudent(student);
    }

    // ================= TEACHER =================
    public boolean addTeacherToCourse(String courseId, Teacher teacher) {
        Course c = searchCourse(courseId);
        if (c == null || teacher == null) return false;
        return c.addTeacher(teacher);
    }

    public void addTeacherToCourse(Course course, Teacher teacher) {
        if (course == null || teacher == null) return;
        Course c = searchCourse(course.getCourseID());
        if (c != null) c.addTeacher(teacher);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CourseList:\n");
        for (Course c : courses) {
            sb.append("  ").append(c.displayLabel()).append("\n");
        }
        return sb.toString();
    }
}
