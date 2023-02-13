package org.baxter.disco.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.usermodel.HSSF
import org.apache.poi.hssf.usermodel.HSSFSheet;

/**
 * Facade for saving data out to a file.
 *
 * @author Blizzard Finnegan
 * @version 4.0.0, 13 Feb. 2023
 */
public class DataSaving
{
    /**
     * Workbook object; used for writing to the final XLSX file.
     */
    private static HSSFWorkbook outputWorkbook;
    /**
     * Object defining what sheet within the workbook we are working in.
     */
    private static HSSFSheet outputSheet;

    /**
     * File representing the location of the final output file.
     */
    private static File outputFile;

    /**
     * Default target temperature
     */
    private static double targetTemp = 36.0;

    /**
     * Default range for a measurement to be considered a fail
     */
    private static double failRange = 0.2;

    /**
     * Style of cell if the measurement falls outside the fail range
     */
    private static HSSFCellStyle failStyle;

    /**
     * Style of cell if Tesseract can't read the image
     */
    private static HSSFCellStyle errorStyle;

    /**
     * Style of a default cell
     */
    private static HSSFCellStyle defaultStyle;

    /**
     * Style of the total cells (sets typing to %)
     */
    private static HSSFCellStyle finalValuesStyle;

    /**
     * Prepares writer to write to XLSX file, with default fail values.
     */
    public static boolean initWorkbook(String filename, int camCount)
    { return initWorkbook(filename, camCount, targetTemp, failRange); }

    /**
     * Prepares writer to write to XLSX file, with custom fail values.
     */
    public static boolean initWorkbook(String filename, int camCount, double targetTemp, double failRange)
    {
        DataSaving.targetTemp = targetTemp;
        DataSaving.failRange = failRange;
        boolean output = false;
        outputFile = new File(filename);
        try
        {
            outputWorkbook = new HSSFWorkbook();
            outputSheet = outputWorkbook.createSheet();
            DataFormat format = outputWorkbook.createDataFormat();

            defaultStyle = outputWorkbook.createCellStyle();
            defaultStyle.setDataFormat(format.getFormat("0.0"));

            finalValuesStyle = outputWorkbook.createCellStyle();
            finalValuesStyle.setDataFormat(format.getFormat("0.000%"));

            //Note on backgrounds:
            //Excel cells have a foreground and a background, allowing
            //for various patterned backgrounds. 
            //To set a solid background, and NOT modify the font, 
            //as below is shown, we need to set the foreground color.
            //As of POI 5.2.3, there is no defined fill type 
            //SOLID_BACKGROUND or similar
            failStyle = outputWorkbook.createCellStyle();
            failStyle.setFillForegroundColor(HSSFColorPredefined.RED.getIndex());
            failStyle.setDataFormat(format.getFormat("0.0"));
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            errorStyle = outputWorkbook.createCellStyle();
            errorStyle.setFillForegroundColor(HSSFColorPredefined.YELLOW.getIndex());
            errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int startingRow = outputSheet.getLastRowNum();
            HSSFRow row = outputSheet.createRow(++startingRow);
            int cellnum = 0;
            HSSFCell cell = row.createCell(cellnum++);
            cell.setCellValue("Iteration");
            for(int i = 0; i < camCount; i++)
            {
                cell = row.createCell(cellnum++);
                cell.setCellValue("Serial");
                cell = row.createCell(cellnum++);
                cell.setCellValue("Image Location");
                cell = row.createCell(cellnum++);
                cell.setCellValue("Read Value");
                cell = row.createCell(cellnum++);
                cell.setCellValue("");
            }
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputWorkbook.write(outputStream);
            output = true;
            outputStream.close();
        }
        catch(Exception e) { ErrorLogging.logError(e); }

        return output;
    }

    /**
     *
     */
    public static void closeWorkbook(int cameraCount)
    {
        int lastRowOfData = outputSheet.getLastRowNum();
        HSSFRow finalRow = outputSheet.createRow(++lastRowOfData);
        HSSFCell titleCell = finalRow.createCell(0);
        titleCell.setCellValue("Totals:");

        ErrorLogging.logError("DEBUG: 3 ?= " + (cameraCount*3));
        for(int column = 3; column <= (cameraCount*3); column+=3)
        {
            HSSFCell cell = finalRow.createCell(column);
            FormulaEvaluator formulaEvaluator = outputWorkbook.getCreationHelper().createFormulaEvaluator();
            String columnName = CellReference.convertNumToColString(column);
            String verticalArray = String.format("$%s$2:$%s$%s",columnName,columnName,lastRowOfData);
            ErrorLogging.logError("DEBUG: Vertical Array: " + verticalArray);
            String formula = String.format(
                "(COUNT(%s)-COUNTIF(%s,{\"<%s\",\"%s\"}))/(COUNT(%s))",
                verticalArray,
                verticalArray, (targetTemp - failRange), (targetTemp + failRange), 
                verticalArray);
            cell.setCellFormula(formula);
            cell.setCellStyle(finalValuesStyle);

            formulaEvaluator.evaluate(cell);

            ErrorLogging.logError("DEBUG: Formula: " + formula);
        }

        try
        {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputWorkbook.write(outputStream);
            outputStream.close();
        }
        catch(Exception e) {ErrorLogging.logError(e);}
    }

    /** 
     * Writes line to XLSX file.
     *
     * @param cycle         What test cycle is being saved to the file 
     * @param inputMap      Map[String,Double] list of inputs
     *
     * @return Returns whether values were saved successfully.
     */
    public static boolean writeValues(int cycle, Map<File,Double> inputMap, Map<String,File> cameraToFile)
    {
        boolean output = false;
        int cellnum = 0;
        int startingRow = outputSheet.getLastRowNum();
        HSSFRow row = outputSheet.createRow(++startingRow);
        List<String> cameraNames = new ArrayList<>(cameraToFile.keySet());
        //ErrorLogging.logError("DEBUG: image locations: " + imageLocations.toString());
        List<Object> objectArray = new LinkedList<>();

        cycle++;

        HSSFCell indexCell = row.createCell(cellnum++);
        indexCell.setCellValue(cycle);
        for(String cameraName : cameraNames)
        {
            File file = cameraToFile.get(cameraName);
            //ErrorLogging.logError("DEBUG: " + cameraName);

            String serialNumber = ConfigFacade.getSerial(cameraName);
            objectArray.add(serialNumber);
            objectArray.add(file.getPath());
            objectArray.add(inputMap.get(file));
            objectArray.add(" ");
        }
        for(Object cellObject : objectArray)
        {
            HSSFCell cell = row.createCell(cellnum++);
            if(cellObject instanceof Double) 
            {
                Double cellValue = (Double)cellObject;
                ErrorLogging.logError("DEBUG: " + cellValue + " ?= " + targetTemp + " +- " + failRange);
                if(cellValue.equals(Double.NEGATIVE_INFINITY))
                {
                    cell.setCellValue("ERROR!");
                    cell.setCellStyle(errorStyle);
                }
                else 
                {
                    cell.setCellValue(cellValue);
                    if( cellValue.doubleValue() > (targetTemp + failRange) ||
                        cellValue.doubleValue() < (targetTemp - failRange) )
                    {
                        ErrorLogging.logError("DEBUG: Cell value " + cellValue.doubleValue() + " is outside the allowed range! (" + (targetTemp -failRange) + "-" + (targetTemp + failRange) + "). Setting cell to fail colouring.");
                        cell.setCellStyle(failStyle);
                    }
                }
            }
            else if(cellObject instanceof String) cell.setCellValue((String) cellObject);
            else 
            { 
                ErrorLogging.logError("XLSX Write Error!!! - Invalid input."); 
                ErrorLogging.logError("\t" + cellObject.toString());
            }

        }
        try
        {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputWorkbook.write(outputStream);
            output = true;
            outputStream.close();
        }
        catch(Exception e) {ErrorLogging.logError(e);}
        return output;
    }
}
