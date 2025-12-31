// ✅ files/Server/FilePacket.java
package files.Server;

import java.io.Serial;
import java.io.Serializable;

public class FilePacket implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String courseId;
    private final String fileName;
    private final byte[] fileData;

    public FilePacket(String courseId, String fileName, byte[] fileData) {
        this.courseId = courseId;
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public String getCourseId() { return courseId; }
    public String getFileName() { return fileName; }
    public byte[] getFileData() { return fileData; }
}
