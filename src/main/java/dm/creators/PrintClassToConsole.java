package dm.creators;

import dm.FieldInfo;

import java.util.Collection;

public class PrintClassToConsole implements ClassCreator {
    @Override
    public void create(String classType, Collection<FieldInfo> fields) {
        System.out.println(" === " + classType + " === ");
        for (FieldInfo info : fields) {
            System.out.print("   - " + info.getName());
            System.out.println(" <" + info.getType() + ">");
        }
    }
}
