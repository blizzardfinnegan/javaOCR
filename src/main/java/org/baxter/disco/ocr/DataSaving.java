package org.baxter.disco.ocr;

import java.io.DataInputStream;
//Standard imports
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//Generic spreadsheet imports
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

//Excel-specific imports
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import static org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;

/**
 * Facade for saving data out to a file.
 *
 * @author Blizzard Finnegan
 * @version 4.1.0, 24 Feb. 2023
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
        DataFormat format = null;

        //Create workbook, Sheet, and DataFormat object
        //HSSF objects are used, as these are compatible with Microsoft Excel
        //XSSF objects were initially used, but caused issues.
        outputWorkbook = new HSSFWorkbook(); 
        outputSheet = outputWorkbook.createSheet();
        format = outputWorkbook.createDataFormat();

        //Create a default style for values.
        defaultStyle = outputWorkbook.createCellStyle();
        defaultStyle.setDataFormat(format.getFormat("0.0"));

        //Create a style for the final percentage values
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

        //Create a style for error-ed, but not out-of-range, values
        errorStyle = outputWorkbook.createCellStyle();
        errorStyle.setFillForegroundColor(HSSFColorPredefined.YELLOW.getIndex());
        errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


        //Create the header
        int startingRow = outputSheet.getLastRowNum();
        HSSFRow row = outputSheet.createRow(++startingRow);
        int cellnum = 0;
        HSSFCell cell = row.createCell(cellnum++);
        cell.setCellValue("Iteration");
        //Create a section for every camera
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

        //Save to file
        try (FileOutputStream outputStream = new FileOutputStream(outputFile))
        { outputWorkbook.write(outputStream); }
        catch(Exception e) {ErrorLogging.logError(e);}

        return output;
    }

    /**
     * Add final totals to the excel document. 
     * Run at the end of testing.
     *
     * @param cameraCount   The number of cameras that were used.
     */
    public static void closeWorkbook(int cameraCount)
    {
        //Get the last row, add another row below it, and name the first cell "Totals:"
        int lastRowOfData = outputSheet.getLastRowNum();
        HSSFRow finalRow = outputSheet.createRow(++lastRowOfData);
        HSSFCell titleCell = finalRow.createCell(0);
        titleCell.setCellValue("Totals:");

        //ErrorLogging.logError("DEBUG: 3 ?= " + (cameraCount*3));
        
        //For each camera, create a unique total line
        for(int column = 3; column <= (cameraCount*3); column+=3)
        {
            //Create a cell in the right column
            HSSFCell cell = finalRow.createCell(column);
            FormulaEvaluator formulaEvaluator = outputWorkbook.getCreationHelper().createFormulaEvaluator();
            String columnName = CellReference.convertNumToColString(column);
            
            //Intermediate variable to store the array of possible values
            //Dollar signs in use here indicate that if the cell is copied and pasted, 
            //the same cells will be referenced
            String verticalArray = String.format("$%s$2:$%s$%s",columnName,columnName,lastRowOfData);
            ErrorLogging.logError("DEBUG: Vertical Array: " + verticalArray);

            //Create the error formula, then set it as the cell's formula.
            String formula = String.format(
                "(COUNT(%s)-COUNTIF(%s,{\"<%s\",\">%s\"}))/(COUNT(%s))",
                verticalArray,
                verticalArray, (targetTemp - failRange), (targetTemp + failRange), 
                verticalArray);
            cell.setCellFormula(formula);

            //Make the percentage human-readable
            cell.setCellStyle(finalValuesStyle);

            //To make the cell be a readable value, you need to
            //evaluate the formula within the cell.
            formulaEvaluator.evaluate(cell);

            ErrorLogging.logError("DEBUG: Formula: " + formula);
        }

        //Once all totals have been created, write to the file
        try (FileOutputStream outputStream = new FileOutputStream(outputFile))
        { outputWorkbook.write(outputStream); }
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
        //List<Object> objectArray = new LinkedList<>();

        cycle++;

        HSSFCell indexCell = row.createCell(cellnum++);
        indexCell.setCellValue(cycle);
        for(String cameraName : cameraNames)
        {
            //put serial number into sheet
            String serialNumber = ConfigFacade.getSerial(cameraName);
            HSSFCell serialCell = row.createCell(cellnum++);
            serialCell.setCellValue(serialNumber);

            //Put the generated image into the spreadsheet
            File file = cameraToFile.get(cameraName);
            HSSFCell imageCell = row.createCell(cellnum++);
            try
            {
                InputStream cameraImage = new DataInputStream(new FileInputStream(file));
                byte[] cameraImageRaw = IOUtils.toByteArray(cameraImage);
                HSSFPatriarch patriarch = outputSheet.createDrawingPatriarch();
                int imageID = outputWorkbook.addPicture(cameraImageRaw,HSSFWorkbook.PICTURE_TYPE_PNG);
                HSSFClientAnchor imageAnchor = new HSSFClientAnchor();
                imageAnchor.setCol1(cellnum-1);
                imageAnchor.setCol2(cellnum);
                imageAnchor.setRow1(startingRow);
                imageAnchor.setRow2(startingRow+1);
                patriarch.createPicture(imageAnchor, imageID);
            } 
            //If the image fails for some reason, fallback to putting the image path into the cell
            catch(Exception e)
            { 
                String fileLocation = file.getPath();
                imageCell.setCellValue(fileLocation);
                ErrorLogging.logError(e); 
            }

            //Put the OCR value into the sheet
            HSSFCell ocrCell = row.createCell(cellnum++);
            Double ocrRead = inputMap.get(file);
            if(ocrRead.equals(Double.NEGATIVE_INFINITY))
            {
                ocrCell.setCellValue("ERROR!");
                ocrCell.setCellStyle(errorStyle);
            }
            else
            {
                ocrCell.setCellValue(ocrRead);
                if( ocrRead.doubleValue() > (targetTemp + failRange) ||
                    ocrRead.doubleValue() < (targetTemp - failRange) )
                    ocrCell.setCellStyle(failStyle);
            }

            //Create a blank cell as a spacer
            row.createCell(cellnum++);
            //ErrorLogging.logError("DEBUG: " + cameraName);

            //objectArray.add(serialNumber);
            //objectArray.add(file.getPath());
            //objectArray.add(inputMap.get(file));
            //objectArray.add(" ");
        }
        //for(Object cellObject : objectArray)
        //{
        //    HSSFCell cell = row.createCell(cellnum++);
        //    if(cellObject instanceof Double) 
        //    {
        //        Double cellValue = (Double)cellObject;
        //        ErrorLogging.logError("DEBUG: " + cellValue + " ?= " + targetTemp + " +- " + failRange);
        //        if(cellValue.equals(Double.NEGATIVE_INFINITY))
        //        {
        //            cell.setCellValue("ERROR!");
        //            cell.setCellStyle(errorStyle);
        //        }
        //        else 
        //        {
        //            cell.setCellValue(cellValue);
        //            if( cellValue.doubleValue() > (targetTemp + failRange) ||
        //                cellValue.doubleValue() < (targetTemp - failRange) )
        //            {
        //                ErrorLogging.logError("DEBUG: Cell value " + cellValue.doubleValue() + " is outside the allowed range! (" + (targetTemp -failRange) + "-" + (targetTemp + failRange) + "). Setting cell to fail colouring.");
        //                cell.setCellStyle(failStyle);
        //            }
        //        }
        //    }
        //    else if(cellObject instanceof String) cell.setCellValue((String) cellObject);
        //    else 
        //    { 
        //        ErrorLogging.logError("XLSX Write Error!!! - Invalid input."); 
        //        ErrorLogging.logError("\t" + cellObject.toString());
        //    }

        //}
        try (FileOutputStream outputStream = new FileOutputStream(outputFile))
        { outputWorkbook.write(outputStream); output = true; }
        catch(Exception e) {ErrorLogging.logError(e);}
        return output;
    }
}
