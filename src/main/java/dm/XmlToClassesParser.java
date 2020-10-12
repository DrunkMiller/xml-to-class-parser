package dm;

import dm.creators.ClassCreator;
import dm.creators.MultiplicityType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class XmlToClassesParser {
    private final Workbook workbook;
    private final ClassCreator classCreator;
    private final int fieldNameScopeSize;
    private final String columnHeaderTemplateWithFieldType;
    private final String columnHeaderTemplateWithFieldMultiplicity;
    private final Pattern validVariableNameRegexPattern;
    private final Set<String> parsedClasses;

    public XmlToClassesParser(String excelFilePath,
                              ClassCreator classCreator,
                              int fieldNameScopeSize,
                              String columnHeaderTemplateWithFieldType,
                              String columnHeaderTemplateWithFieldMultiplicity,
                              String validVariableNamePattern) {
        this.workbook = openWorkbook(excelFilePath);
        this.classCreator = classCreator;
        this.fieldNameScopeSize = fieldNameScopeSize;
        this.columnHeaderTemplateWithFieldType = columnHeaderTemplateWithFieldType;
        this.columnHeaderTemplateWithFieldMultiplicity = columnHeaderTemplateWithFieldMultiplicity;
        this.validVariableNameRegexPattern = Pattern.compile(validVariableNamePattern);
        this.parsedClasses = new HashSet<>();
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
        int columnNumberOfFieldType = findColumnNumberByName(sheet, columnHeaderTemplateWithFieldType);
        int columnNumberOfFieldMultiplicity = findColumnNumberByName(sheet, columnHeaderTemplateWithFieldMultiplicity);
        Deque<LinkedList<FieldInfo>> nestingQueue = new LinkedList<>();
        nestingQueue.add(new LinkedList<>());
        int currentNestingLevel = -1;
        for (Row currentRow : sheet){
            for (Cell nameCell : currentRow) {
                if (nameCell.getColumnIndex() > fieldNameScopeSize) break;
                Cell typeCell = currentRow.getCell(columnNumberOfFieldType);
                Cell multiplicityCell = currentRow.getCell(columnNumberOfFieldMultiplicity);
                if (isValidCell(nameCell) || (isValidCell(typeCell) && currentNestingLevel == -1)) {
                    if (nameCell.getColumnIndex() > currentNestingLevel && currentNestingLevel > 0) {
                        nestingQueue.add(new LinkedList<>());
                    }
                    if (nameCell.getColumnIndex() < currentNestingLevel) {
                        popAndCreateClasses(nestingQueue, currentNestingLevel - nameCell.getColumnIndex());
                    }
                    currentNestingLevel = nameCell.getColumnIndex();
                    nestingQueue.getLast().add(parseRow(nameCell, typeCell, multiplicityCell));
                    Sheet sheetWithFieldClassDeclaration = findSheetWithClassDeclaration(typeCell);
                    if (sheetWithFieldClassDeclaration != null ) {
                        parseSheet(sheetWithFieldClassDeclaration);
                    }
                    break;
                }
            }
        }
        popAndCreateClasses(nestingQueue, nestingQueue.size() - 1);
    }

    private void popAndCreateClasses(Deque<LinkedList<FieldInfo>> queue, int count) {
        for (int j = 0; j < count ; j++) {
            LinkedList<FieldInfo> classFields = queue.pollLast();
            createClass(queue.getLast().getLast(), classFields);
        }
    }

    private void createClass(FieldInfo classInfo, List<FieldInfo> fields) {
        String className =  classInfo.getType();
        if (parsedClasses.contains(className)) {
            return;
        }
        classCreator.create(className, fields);
        parsedClasses.add(className);
        System.out.println("Class '" + className + "' created");
    }

    private int findColumnNumberByName(Sheet sheet, String name) {
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            Iterator<Cell> cellIterator = currentRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();
                if (isStringTypeCell(currentCell) && currentCell.getStringCellValue().trim().equals(name)) {
                    return currentCell.getColumnIndex();
                }
            }
        }
        throw new RuntimeException("Column '" + name +"' with type not found in sheet " + "'" + sheet.getSheetName() + "'");
    }

    private Sheet findSheetWithClassDeclaration(Cell classTypeCell) {
        if (isEmptyCell(classTypeCell)) {
            return null;
        }
        Sheet sheetWithClassDeclaration = getSheetAtCellHyperlink(classTypeCell);
        if (sheetWithClassDeclaration == null) {
            sheetWithClassDeclaration = findSheetOf(classTypeCell.getStringCellValue());
        }
        if (classTypeCell.getSheet().equals(sheetWithClassDeclaration) || parsedClasses.contains(classTypeCell.getStringCellValue())) {
            return null;
        }
        else {
            return sheetWithClassDeclaration;
        }
    }

    private Sheet getSheetAtCellHyperlink(Cell classTypeCell) {
        Hyperlink hyperlink = classTypeCell.getHyperlink();
        if (hyperlink == null) return null;
        String targetSheetName = classTypeCell.getHyperlink().getAddress().split("!")[0].replace("'", "");
        Sheet targetSheet = workbook.getSheet(targetSheetName);
        if (targetSheet == null) return null;
        if (targetSheet.equals(classTypeCell.getSheet())) return null;
        return targetSheet;
    }

    private Sheet findSheetOf(String str) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String currentSheetName = workbook.getSheetName(i).toLowerCase();
            if (currentSheetName.contains(str.toLowerCase())) {
                return workbook.getSheet(currentSheetName);
            }
        }
        return null;
    }

    private FieldInfo parseRow(Cell name, Cell type, Cell multiplicity) {
        String fieldName = parseNameCell(name);
        String fieldType = parseTypeCell(type);
        MultiplicityType multiplicityType = parseMultiplicityCell(multiplicity);
        if (fieldType.trim().isEmpty() && !fieldName.isEmpty()) {
            fieldType += Character.toUpperCase(fieldName.charAt(0));
            fieldType += fieldName.substring(1);
        }
        return new FieldInfo(fieldName, fieldType, multiplicityType);
    }
    
    private String parseNameCell(Cell nameCell) {
        if (isEmptyCell(nameCell)) {
            System.err.println("Warning! Cell 'name' is empty in sheet '"
                    + (nameCell == null ? "null" : nameCell.getSheet().getSheetName()) + "' row '"
                    + (nameCell == null ? "null" : nameCell.getRow().getRowNum()));
            return "";
        }
        return replaceRussianSymbolC(nameCell.getStringCellValue());
    }

    private String parseTypeCell(Cell typeCell) {
        if (isEmptyCell(typeCell)) {
            System.err.println("Warning! Cell 'type' is empty in sheet '"
                    + (typeCell == null ? "null" : typeCell.getSheet().getSheetName())
                    + "' row '" +  (typeCell == null ? "null" : typeCell.getRow().getRowNum()));
            return "";
        }
        else {
            String typeValue = "";
            if (Character.isLowerCase(typeCell.getStringCellValue().charAt(0))) {
                typeValue += Character.toUpperCase(typeCell.getStringCellValue().charAt(0));
                typeValue += typeCell.getStringCellValue().substring(1);
            }
            else {
                typeValue = typeCell.getStringCellValue();
            }
            return replaceRussianSymbolC(typeValue);
        }
    }

    private MultiplicityType parseMultiplicityCell(Cell multiplicityCell) {
        if (isEmptyCell(multiplicityCell)) {
            System.err.println("Warning! Cell 'multiplicity' is empty in sheet '"
                    + (multiplicityCell == null ? "null" : multiplicityCell.getSheet().getSheetName()) + "' row '"
                    + (multiplicityCell == null ? "null" : multiplicityCell.getRow().getRowNum()));
            return MultiplicityType.ONE;
        }
        if (multiplicityCell.getStringCellValue().contains("*")) {
            return MultiplicityType.MANY;
        }
        else {
            return MultiplicityType.ONE;
        }
    }

    private Workbook openWorkbook(String path) {
        try {
            if (path.endsWith(".xlsx")) return new XSSFWorkbook(new FileInputStream(path));
            if (path.endsWith(".xls")) return new HSSFWorkbook(new FileInputStream(path));
            else throw new IOException("File format does not match 'xls', 'xlsx'" );
        }
        catch (IOException exception) {
            throw new RuntimeException("Excel file not opened: " + exception.getMessage());
        }
    }

    private String replaceRussianSymbolC(String str) {
        return str.replace('с', 'c').replace('С','C');
    }

    private boolean isValidCell(Cell cell) {
        return cell != null && isStringTypeCell(cell) && !isEmptyCell(cell) && isContainsValidContent(cell);
    }

    private boolean isContainsValidContent(Cell cell) {
        return validVariableNameRegexPattern.matcher(cell.getStringCellValue()).matches();
    }

    private boolean isStringTypeCell(Cell cell) {
        return cell.getCellType() == Cell.CELL_TYPE_STRING;
    }

    private boolean isEmptyCell(Cell cell) {
        return cell == null || cell.getStringCellValue().trim().isEmpty();
    }
}
