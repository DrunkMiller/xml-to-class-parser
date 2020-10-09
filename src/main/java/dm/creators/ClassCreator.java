package dm.creators;

import dm.FieldInfo;

import java.util.Collection;

public interface ClassCreator {
    void create(String classType, Collection<FieldInfo> fields);
}
