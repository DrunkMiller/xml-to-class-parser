package dm;

import dm.creators.MultiplicityType;

import java.util.Objects;

public class FieldInfo {
    private final String name;
    private final String type;
    private final MultiplicityType multiplicityType;

    public FieldInfo(String name, String type, MultiplicityType multiplicityType) {
        this.name = name.trim();
        this.type = type.trim();
        this.multiplicityType = multiplicityType;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public MultiplicityType getMultiplicityType() {
        return multiplicityType;
    }

    @Override
    public String toString() {
        return name + "  <" + type + "> ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo info = (FieldInfo) o;
        return Objects.equals(name, info.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
