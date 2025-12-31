// ✅ files/Server/Notification.java
package files.Server;

import java.io.Serial;
import java.io.Serializable;

public final class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String notification;

    public Notification() {}

    public Notification(String notification) {
        setNotification(notification);
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = (notification == null) ? "" : notification.trim();
    }

    @Override
    public String toString() {
        return "Notification{notification='" + notification + "'}";
    }
}
