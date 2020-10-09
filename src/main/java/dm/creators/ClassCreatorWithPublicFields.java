package dm.creators;
import dm.FieldInfo;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class ClassCreatorWithPublicFields implements ClassCreator {
    private final Path outFilesPath;

    public ClassCreatorWithPublicFields(String outFilesPath) {
        this.outFilesPath = Paths.get(outFilesPath);
        if (!Files.exists(this.outFilesPath) || !Files.isDirectory(this.outFilesPath)) {
            throw new IllegalArgumentException("Output directory '\" + inDir + \"' does not exist");
        }
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
            writer.write("public class " + classType +" {");
            writer.write(System.lineSeparator());
            for (FieldInfo info : fields) {
                writer.write("\tpublic " + info.getType() + " " + info.getName() + ";");
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
}
