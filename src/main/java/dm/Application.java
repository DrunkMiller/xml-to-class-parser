package dm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dm.creators.ClassCreator;
import dm.creators.ClassCreatorWithPublicFields;
import org.apache.commons.cli.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Application {
    public static void main(String[] args) {
        CommandLine line = getConsoleArgs(args);

        Map<String, String> replacementNameDict = new HashMap<>();
        if (line.hasOption("nd")) {
            replacementNameDict = loadDictionary(line.getOptionValue("nd"));
        }

        Map<String, String> replacementTypeDict = new HashMap<>();
        if (line.hasOption("td")) {
            replacementTypeDict = loadDictionary(line.getOptionValue("td"));
        }

        Map<String, String> importsDict = new HashMap<>();
        if (line.hasOption("id")) {
            importsDict = loadDictionary(line.getOptionValue("id"));
        }

        String exelFile = line.getOptionValue("in");
        String outDir = line.getOptionValue("out");
        String nameSheet = line.getOptionValue("s");
        int fieldNameScopeSize = 9;
        String columnHeaderTemplateWithFieldType = "Тип";
        String columnHeaderTemplateWithFieldMultiplicity = "Обязательность и кратность";
        String validVariableNamePattern = "^[a-zA-ZсС][a-zA-Z0-9_&сС]*";
        if (line.hasOption("size")) {
            fieldNameScopeSize = Integer.parseInt(line.getOptionValue("size"));
        }
        if (line.hasOption("tcolname")) {
            columnHeaderTemplateWithFieldType = line.getOptionValue("tcolname");
        }
        if (line.hasOption("mcolname")) {
            columnHeaderTemplateWithFieldMultiplicity = line.getOptionValue("mcolname");
        }
        if (line.hasOption("vreg")) {
            validVariableNamePattern = line.getOptionValue("vreg");
        }

        ClassCreator classCreator = new ClassCreatorWithPublicFields(
                outDir,
                replacementNameDict,
                replacementTypeDict,
                importsDict);

        XmlToClassesParser parser = new XmlToClassesParser(
                exelFile,
                classCreator,
                fieldNameScopeSize,
                columnHeaderTemplateWithFieldType,
                columnHeaderTemplateWithFieldMultiplicity,
                validVariableNamePattern);
        parser.parse(nameSheet);
    }

    static Map<String, String> loadDictionary(String path) {
        Map<String, String> dict = new HashMap<>();
        try {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(Paths.get(path)));
            dict = gson.fromJson(json, new TypeToken<HashMap<String, String>>() {}.getType());
        }
        catch (Exception exception) {
            System.err.println("Dictionary file '" + path + "' was not read : " + exception.getMessage());
        }
        finally {
            return dict;
        }
    }

    static CommandLine getConsoleArgs(String[] args) {
        final Options options = new Options();
        options.addOption(
                Option.builder("in")
                        .required(true)
                        .longOpt("input")
                        .desc("Input excel file path")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("out")
                        .required(true)
                        .longOpt("output")
                        .desc("Output files path")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("s")
                        .required(true)
                        .longOpt("sheet")
                        .desc("Name sheet for parsing")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("nd")
                        .required(false)
                        .longOpt("name-replacement-dict")
                        .desc("Dictionary file for name replacement")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("td")
                        .required(false)
                        .longOpt("type-replacement-dict")
                        .desc("Dictionary file for type replacement")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("id")
                        .required(false)
                        .longOpt("imports-dict")
                        .desc("Dictionary file for importing required types")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("size")
                        .required(false)
                        .longOpt("scope-size")
                        .desc("Field name scope size")
                        .hasArg()
                        .type(Integer.class)
                        .build()
        );
        options.addOption(
                Option.builder("tcolname")
                        .required(false)
                        .longOpt("type-col-name")
                        .desc("Column header template with field type")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("mcolname")
                        .required(false)
                        .longOpt("mul-col-name")
                        .desc("Column header template with field multiplicity")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("vreg")
                        .required(false)
                        .longOpt("validation-regex")
                        .desc("Regex pattern for validating variable name ")
                        .hasArg()
                        .build()
        );
        CommandLineParser commandLineParser = new DefaultParser();
        try {
            return commandLineParser.parse(options, args);
        }
        catch (ParseException exception) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "ant", options);
            return null;
        }
    }
}
