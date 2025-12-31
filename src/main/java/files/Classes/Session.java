package files.Classes;

public final class Session {

    private static Student currentStudent;
    private static Teacher currentTeacher;

    private Session() {}

    // ===== Student =====
    public static Student getStudent() {
        return currentStudent;
    }

    public static void setStudent(Student s) {
        currentStudent = s;
    }

    // ===== Teacher (optional but useful) =====
    public static Teacher getTeacher() {
        return currentTeacher;
    }

    public static void setTeacher(Teacher t) {
        currentTeacher = t;
    }

    public static void clear() {
        currentStudent = null;
        currentTeacher = null;
    }
}
