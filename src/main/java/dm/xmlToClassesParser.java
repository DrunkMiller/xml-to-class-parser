package dm;

import dm.creators.ClassCreator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class xmlToClassesParser {
    private final Workbook workbook;
    private int fieldNameScopeSize;
    private String columnHeaderTemplateWithFieldType;
    private Pattern validVariableNameRegexPattern;
    private final ClassCreator classCreator;
    private final Set<String> createdClasses;
    private final Map<String, String> typeReplacementDictionary;

    public xmlToClassesParser(String excelFilePath,
                              ClassCreator classCreator,
                              Map<String, String> typeReplacementDictionary,
                              int fieldNameScopeSize,
                              String columnHeaderTemplateWithFieldType,
                              String validVariableNamePattern) {
        this.fieldNameScopeSize = fieldNameScopeSize;
        this.columnHeaderTemplateWithFieldType = columnHeaderTemplateWithFieldType;
        this.validVariableNameRegexPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_&]*");
        this.classCreator = classCreator;
        this.typeReplacementDictionary = typeReplacementDictionary;
        this.createdClasses = new HashSet<>();
        this.workbook = openWorkbook(excelFilePath);
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
        int columnNumberOfFieldType = findNumbreOfTypeColumn(sheet);
        Deque<LinkedList<FieldInfo>> nestingQueue = new LinkedList<>();
        nestingQueue.add(new LinkedList<>());
        int currentNestingLevel = -1;
        for (Row currentRow : sheet){
            for (Cell nameCell : currentRow) {
                if (nameCell.getColumnIndex() > fieldNameScopeSize) {
                    break;
                }
                if (isValidCell(nameCell)) {
                    if (nameCell.getColumnIndex() > currentNestingLevel && currentNestingLevel > 0) {
                        nestingQueue.add(new LinkedList<>());
                    }
                    if (nameCell.getColumnIndex() < currentNestingLevel) {
                        popAndCreateClasses(nestingQueue, currentNestingLevel - nameCell.getColumnIndex());
                    }
                    currentNestingLevel = nameCell.getColumnIndex();
                    nestingQueue.getLast().add(createField(nameCell, currentRow.getCell(columnNumberOfFieldType)));
                    parseCellHyperlink(currentRow.getCell(columnNumberOfFieldType));
                    break;
                }
            }
        }
        popAndCreateClasses(nestingQueue, nestingQueue.size() - 1);
    }

    private void popAndCreateClasses(Deque<LinkedList<FieldInfo>> queue, int count) {
        for (int j = 0; j < count ; j++) {
            LinkedList<FieldInfo> classFields = queue.pollLast();
            createClass(queue.getLast().getLast().getType(), classFields);
        }
    }

    private void createClass(String classType, List<FieldInfo> fields) {
        if (createdClasses.contains(classType)) {
            return;
        }
        classCreator.create(classType, fields);
        createdClasses.add(classType);
        System.out.println("Class '" + classType + "' created");
    }

    private void parseCellHyperlink(Cell cell) {
        if (cell == null) {
            return;
        }
        Hyperlink hyperlink = cell.getHyperlink();
        if (hyperlink == null) {
            return;
        }
        String targetSheetName = cell.getHyperlink().getAddress().split("!")[0];
        Sheet sheet = workbook.getSheet(targetSheetName);
        if (sheet != null) {
            parseSheet(sheet);
        }
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

    private int findNumbreOfTypeColumn(Sheet sheet) {
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            Iterator<Cell> cellIterator = currentRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell currentCell = cellIterator.next();
                if (cellTypeIsString(currentCell) && currentCell.getStringCellValue().trim().equals(columnHeaderTemplateWithFieldType)) {
                    return currentCell.getColumnIndex();
                }
            }
        }
        throw new RuntimeException(" column with type not found" );
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
        return cell != null && cell.getCellType() == XSSFCell.CELL_TYPE_STRING;
    }

    private boolean isEmptyCell(Cell cell) {
        return cell == null || cell.getStringCellValue().trim().isEmpty();
    }
}
