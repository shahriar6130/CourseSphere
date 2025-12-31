package files;

import java.io.Serial;
import java.io.Serializable;

public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum RequestType {
        GET_ALL_COORDINATED_DATA,
        WRITE_TO_FILE
    }

    private final RequestType requestType;

    // only for WRITE_TO_FILE
    private final String path;
    private final String line;

    // for GET_ALL_COORDINATED_DATA
    public Request(RequestType requestType) {
        this.requestType = requestType;
        this.path = null;
        this.line = null;
    }

    // for WRITE_TO_FILE
    public Request(String path, String line) {
        this.requestType = RequestType.WRITE_TO_FILE;
        this.path = path;
        this.line = line;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getPath() {
        return path;
    }

    public String getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "Request{type=" + requestType + ", path='" + path + "', line='" + line + "'}";
    }
}
