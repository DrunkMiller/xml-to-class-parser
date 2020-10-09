package dm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dm.creators.ClassCreatorWithPublicFields;
import dm.xmlToClassesParser;
import org.apache.commons.cli.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Application {
    public static void main(String[] args) {
        CommandLine line = getConsoleArgs(args);
        Map<String, String> replacementDict = new HashMap<>();
        String jsonInputReplacementDictionary = "";
        if (line.hasOption("d")) {
            try {
                jsonInputReplacementDictionary = line.getOptionValue("d");
                Gson gson = new Gson();
                String json = new String(Files.readAllBytes(Paths.get(jsonInputReplacementDictionary)));
                replacementDict = gson.fromJson(json, new TypeToken<HashMap<String, String>>() {}.getType());
            }
            catch (IOException exception) {
                System.err.println("Dictionary file '" + jsonInputReplacementDictionary + "' was not read : " + exception.getMessage());
                exception.printStackTrace(System.err);
            }
        }

        String exelFile = line.getOptionValue("in");
        String outDir = line.getOptionValue("out");
        String nameSheet = line.getOptionValue("s");
        int fieldNameScopeSize = 9;
        String columnHeaderTemplateWithFieldType = "Тип";
        String validVariableNamePattern = "^[a-zA-Z][a-zA-Z0-9_&]*";
        if (line.hasOption("size")) {
            fieldNameScopeSize = Integer.parseInt(line.getOptionValue("size"));
        }
        if (line.hasOption("tcolname")) {
            columnHeaderTemplateWithFieldType = line.getOptionValue("tcolname");
        }
        if (line.hasOption("reg")) {
            validVariableNamePattern = line.getOptionValue("reg");
        }

        xmlToClassesParser parser = new xmlToClassesParser(
                exelFile,
                new ClassCreatorWithPublicFields(outDir),
                replacementDict,
                fieldNameScopeSize,
                columnHeaderTemplateWithFieldType,
                validVariableNamePattern);
        parser.parse(nameSheet);
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
                Option.builder("d")
                        .required(false)
                        .longOpt("replacement-dict")
                        .desc("Dictionary file for type replacement")
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
                        .longOpt("type-col-pattern")
                        .desc("Column header template with field type")
                        .hasArg()
                        .build()
        );
        options.addOption(
                Option.builder("reg")
                        .required(false)
                        .longOpt("name-regex")
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
            formatter.printHelp( "", options);
            return null;
        }
    }
}
