package dm;

import dm.creators.MultiplicityType;

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
}
