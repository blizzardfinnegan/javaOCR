package org.baxter.disco.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Facade for saving data out to a file.
 *
 * @author Blizzard Finnegan
 * @version 1.0.1, 01 Feb. 2023
 */
public class DataSaving
{
    /**
     * Workbook object; used for writing to the final XLSX file.
     */
    private static XSSFWorkbook outputWorkbook;
    /**
     * Object defining what sheet within the workbook we are working in.
     */
    private static XSSFSheet outputSheet;

    /**
     * File representing the location of the final output file.
     */
    private static File outputFile;

    /**
     * Prepares writer to write to XLSX file.
     */
    public static boolean initWorkbook(String filename)
    {
        boolean output = false;
        outputFile = new File(filename);
        try
        {
            outputWorkbook = new XSSFWorkbook();
            outputSheet = outputWorkbook.createSheet();
        }
        catch(Exception e) { ErrorLogging.logError(e); }
        return output;
    }
    /** 
     * Writes line to XLSX file.
     *
     * @param cycle         What test cycle is being saved to the file 
     * @param inputMap      Map[String,Double] list of inputs
     *
     * @return Returns whether values were saved successfully.
     */
    public static boolean writeValues(int cycle, Map<String,Double> inputMap)
    {
        boolean output = false;
        int startingRow = outputSheet.getLastRowNum();
        Row row = outputSheet.createRow(++startingRow);
        Set<String> imageLocations = inputMap.keySet();
        List<Object> objectArray = new LinkedList<>();
        objectArray.add((double)cycle);
        for(String imageLocation : imageLocations)
        {
            objectArray.add(imageLocation);
            objectArray.add(inputMap.get(imageLocation));
        }
        int cellnum = 0;
        for(Object cellObject : objectArray)
        {
            Cell cell = row.createCell(cellnum++);
            if(cellObject instanceof Double) cell.setCellValue((Double) cellObject);
            else if(cellObject instanceof String) cell.setCellValue((String) cellObject);
            else { ErrorLogging.logError("XLSX Write Error!!! - Invalid input."); }

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
