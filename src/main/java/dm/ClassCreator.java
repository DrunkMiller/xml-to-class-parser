package dm;

import java.util.Collection;

public interface ClassCreator {
    void create(String classType, Collection<FieldInfo> fields);
}
