package dm.creators;
import dm.FieldInfo;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClassCreatorWithPublicFields implements ClassCreator {
    private final Path outFilesPath;
    private final Map<String, String> typeReplacementDict;
    private final Map<String, String> importsDict;

    public ClassCreatorWithPublicFields(String outFilesPath,
                                        Map<String, String> typeReplacementDict,
                                        Map<String, String> importsDict) {
        this.outFilesPath = Paths.get(outFilesPath);
        if (!Files.exists(this.outFilesPath) || !Files.isDirectory(this.outFilesPath)) {
            throw new IllegalArgumentException("Output directory '\" + inDir + \"' does not exist");
        }
        this.typeReplacementDict = typeReplacementDict;
        this.importsDict = importsDict;
    }

    @Override
    public void create(String classType, Collection<FieldInfo> fields) {
        Path filePath = outFilesPath.resolve(classType + ".java");
        if (Files.exists(filePath)) {
            throw new RuntimeException("'" + classType + ".java" + "' exist in directory '" + filePath.toAbsolutePath().toString() + "'");
        }
        try {
            Files.createFile(filePath).toFile();
            FileWriter writer = new FileWriter(filePath.toFile());
            Collection<FieldInfo> replacedFields = replaceTypes(fields);
            writer.write(createImporsStrings(replacedFields));
            writer.write(System.lineSeparator());
            writer.write("public class " + classType +" {");
            writer.write(System.lineSeparator());
            for (FieldInfo info : replacedFields) {
                writer.write("\tpublic ");
                if (info.getMultiplicityType() == MultiplicityType.ONE) {
                    writer.write(info.getType() + " " + info.getName() + ";");
                }
                if (info.getMultiplicityType() == MultiplicityType.MANY) {
                    writer.write("List<" + info.getType() + "> " + info.getName() + ";");
                }
                writer.write(System.lineSeparator());
            }
            writer.write("}");
            writer.close();
        }
        catch (Exception exception) {
            throw new RuntimeException("Class file '" + classType + ".java' not created in directory '"
                    + filePath.toAbsolutePath().toString() + "' : " + exception.getMessage());
        }
    }

    private Collection<FieldInfo> replaceTypes(Collection<FieldInfo> fields) {
        List<FieldInfo> fieldsWithNewTypes = new ArrayList<>();
        for (FieldInfo info : fields) {
            if (typeReplacementDict.containsKey(info.getType())) {
                fieldsWithNewTypes.add(new FieldInfo(info.getName(),
                        typeReplacementDict.get(info.getType()),
                        info.getMultiplicityType()));
            }
            else {
                fieldsWithNewTypes.add(info);
            }
        }
        return fieldsWithNewTypes;
    }

    private String createImporsStrings(Collection<FieldInfo> fields) {
        StringBuilder imports = new StringBuilder();
        for (FieldInfo info : fields) {
            if (info.getMultiplicityType() == MultiplicityType.MANY) {
                imports.append("import java.util.List;");
                imports.append(System.lineSeparator());
                break;
            }
        }
        Set<String> addedImportsForTypes = new HashSet<>();
        for (FieldInfo info : fields) {
            if (importsDict.containsKey(info.getType()) && !addedImportsForTypes.contains(info.getType())) {
                addedImportsForTypes.add(info.getType());
                imports.append("import ");
                imports.append(importsDict.get(info.getType()));
                imports.append(";");
                imports.append(System.lineSeparator());
            }
        }
        return imports.toString();
    }
}
