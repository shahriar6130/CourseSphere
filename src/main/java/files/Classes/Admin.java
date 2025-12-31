package files.Classes;
public class Admin {
    private final static int adminID = 707070;
    private final static String adminPassword = "csedu30";
    private static Admin adminInstance = null;

    private Admin() {
    }

    public static Admin getAdminInstance() {
        if (adminInstance == null) {
            adminInstance = new Admin();
        }
        return adminInstance;
    }

    public boolean verifyCredentials(int id, String password) {
        return adminID == id && adminPassword.equals(password);
    }
}