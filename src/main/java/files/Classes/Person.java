package files.Classes;

import java.io.Serializable;
import java.util.Objects;

public abstract class Person implements Serializable {

    private String name;
    private int id;
    private String password;

    protected Person(String name, int id, String password) {
        this.name = name;
        this.id = id;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Primary (clean Java-style)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // 🔥 COMPATIBILITY METHOD (fixes your errors)
    public int getID() {
        return getId();
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{name='" + name + "', id=" + id + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person p)) return false;
        return id == p.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
