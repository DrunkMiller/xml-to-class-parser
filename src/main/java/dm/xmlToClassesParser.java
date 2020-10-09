package dm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.cli.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class xmlToClassesParser {
    private final Workbook workbook;
    private int fieldNameScopeSize;
    private int columnNumberOfFieldType;
    private Pattern validVariableNameRegexPattern;
    private final ClassCreator classCreator;
    private final Set<String> createdClasses;
    private final Map<String, String> typeReplacementDictionary;

    public xmlToClassesParser(String excelFilePath,
                              ClassCreator classCreator,
                              Map<String, String> typeReplacementDictionary) {
        this.fieldNameScopeSize = 9;
        this.columnNumberOfFieldType = 13;
        this.validVariableNameRegexPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_&]*");
        this.classCreator = classCreator;
        this.typeReplacementDictionary = typeReplacementDictionary;
        this.createdClasses = new HashSet<>();
        this.workbook = openWorkbook(excelFilePath);
    }

    public void setFieldNameScopeSize(int fieldNameScopeSize) {
        this.fieldNameScopeSize = fieldNameScopeSize;
    }

    public void setColumnNumberOfFieldType(int columnNumberOfFieldType) {
        this.columnNumberOfFieldType = columnNumberOfFieldType;
    }

    public void setValidVariableNameRegexPattern(String validVariableNameRegexPattern) {
        this.validVariableNameRegexPattern = Pattern.compile(validVariableNameRegexPattern);
    }

    public void parse(String targetSheet) {
        try {
            Sheet sheet = workbook.getSheet(targetSheet);
            parseSheet(sheet);
        }
        catch (Exception exception) {
            throw new RuntimeException("Sheet '" + targetSheet + "' was not parsed: " + exception.getMessage());
        }
    }

    private void parseSheet(Sheet sheet) {
        Deque<LinkedList<FieldInfo>> nestingQueue = new LinkedList<>();
        LinkedList<FieldInfo> fieldsOfCurrentParsedClass = new LinkedList<>();
        int currentNestingLevel = 0;
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            Iterator<Cell> cellIterator = currentRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();
                if (currentCell.getColumnIndex() > fieldNameScopeSize) {
                    break;
                }
                if (!isValidCell(currentCell)) {
                    continue;
                }
                if (currentCell.getColumnIndex() > currentNestingLevel && !fieldsOfCurrentParsedClass.isEmpty()) {
                    nestingQueue.add(fieldsOfCurrentParsedClass);
                    fieldsOfCurrentParsedClass = new LinkedList<>();
                }
                if (currentCell.getColumnIndex() < currentNestingLevel) {
                    createClass(nestingQueue.getLast().getLast().getType(), fieldsOfCurrentParsedClass);
                    for (int j = 0; j < currentNestingLevel - currentCell.getColumnIndex() - 1; j++) {
                        createClass(nestingQueue.getLast().getLast().getType(), nestingQueue.pollLast());
                    }
                    fieldsOfCurrentParsedClass = nestingQueue.pollLast();
                }
                currentNestingLevel = currentCell.getColumnIndex();
                fieldsOfCurrentParsedClass.add(createField(currentCell, currentRow.getCell(columnNumberOfFieldType)));
                Sheet hyperlinkOnSheet = getCellHyperlink(currentRow.getCell(columnNumberOfFieldType));
                if (hyperlinkOnSheet != null) {
                    parseSheet(hyperlinkOnSheet);
                }
                break;
            }
        }
        while (!nestingQueue.isEmpty()) {
            createClass(nestingQueue.getLast().getLast().getType(), fieldsOfCurrentParsedClass);
            fieldsOfCurrentParsedClass = nestingQueue.pollLast();
        }
    }

    private void createClass(String classType, List<FieldInfo> fields) {
        if (createdClasses.contains(classType)) {
            return;
        }
        classCreator.create(classType, fields);
        createdClasses.add(classType);
        System.out.println("Class '" + classType + "' was created");
    }

    private Sheet getCellHyperlink(Cell cell) {
        if (cell == null) {
            return null;
        }
        Hyperlink hyperlink = cell.getHyperlink();
        if (hyperlink == null) {
            return null;
        }
        String targetSheetName = cell.getHyperlink().getAddress().split("!")[0];
        return workbook.getSheet(targetSheetName);
    }

    private Workbook openWorkbook(String path) {
        try {
            if (path.endsWith(".xlsx")) {
                return new XSSFWorkbook(new FileInputStream(path));
            }
            if (path.endsWith(".xls")) {
                return new HSSFWorkbook(new FileInputStream(path));
            }
            else {
                throw new IOException("File format does not match 'xls', 'xlsx'" );
            }
        }
        catch (IOException exception) {
            throw new RuntimeException("Excel file not opened: " + exception.getMessage());
        }
    }

    private FieldInfo createField(Cell name, Cell type) {
        if (isEmptyCell(type)) {
            System.err.println("Warning! Cell 'type' is empty in sheet '" +
                    name.getSheet().getSheetName() + "' row '" + name.getRow().getRowNum());
        }
        String fieldType = type != null ? type.getStringCellValue() : "";
        if (typeReplacementDictionary.containsKey(fieldType)) {
            fieldType = typeReplacementDictionary.get(fieldType);
        }
        return new FieldInfo(name.getStringCellValue(), fieldType);
    }

    private boolean isValidCell(Cell cell) {
        return cellTypeIsString(cell) && !isEmptyCell(cell) && isValidVariableName(cell);
    }

    private boolean isValidVariableName(Cell cell) {
        return validVariableNameRegexPattern.matcher(cell.getStringCellValue()).find();
    }

    private boolean cellTypeIsString(Cell cell) {
        return cell.getCellType() == XSSFCell.CELL_TYPE_STRING;
    }

    private boolean isEmptyCell(Cell cell) {
        return cell == null || cell.getStringCellValue().trim().isEmpty();
    }

    public static void main(String[] args) {
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
                Option.builder("type")
                        .required(false)
                        .longOpt("type-col-num")
                        .desc("Column number of field type")
                        .hasArg()
                        .type(Integer.class)
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
        CommandLine line = null;
        try {
            line = commandLineParser.parse(options, args);
        }
        catch (ParseException exception) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "", options);
        }

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
        xmlToClassesParser parser = new xmlToClassesParser(
                exelFile,
                new ClassCreatorWithPublicFields(outDir),
                replacementDict);

        if (line.hasOption("size")) {
            parser.setFieldNameScopeSize(Integer.parseInt(line.getOptionValue("size")));
        }
        if (line.hasOption("type")) {
            parser.setColumnNumberOfFieldType(Integer.parseInt(line.getOptionValue("type")));
        }
        if (line.hasOption("reg")) {
            parser.setValidVariableNameRegexPattern(line.getOptionValue("reg"));
        }

        parser.parse(nameSheet);
    }
}
