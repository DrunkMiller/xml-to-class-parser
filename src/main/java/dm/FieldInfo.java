package dm;

public class FieldInfo {
    private final String name;
    private final String type;

    public FieldInfo(String name, String type) {
        this.name = name.trim();
        this.type = type.trim();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        if (type.trim().isEmpty() && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return type;
    }

    @Override
    public String toString() {
        return name + "  <" + type + "> ";
    }
}
