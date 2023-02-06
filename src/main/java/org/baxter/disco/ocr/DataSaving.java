package org.baxter.disco.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Facade for saving data out to a file.
 *
 * @author Blizzard Finnegan
 * @version 3.0.0, 06 Feb. 2023
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
    public static boolean initWorkbook(String filename, int camCount)
    {
        boolean output = false;
        outputFile = new File(filename);
        try
        {
            outputWorkbook = new XSSFWorkbook();
            outputSheet = outputWorkbook.createSheet();
            int startingRow = outputSheet.getLastRowNum();
            Row row = outputSheet.createRow(++startingRow);
            int cellnum = 0;
            Cell cell = row.createCell(cellnum++);
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
        int startingRow = outputSheet.getLastRowNum();
        Row row = outputSheet.createRow(++startingRow);
        List<String> cameraNames = new ArrayList<>(cameraToFile.keySet());
        //ErrorLogging.logError("DEBUG: image locations: " + imageLocations.toString());
        List<Object> objectArray = new LinkedList<>();

        cycle++;
        objectArray.add((double)cycle);
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
        int cellnum = 0;
        for(Object cellObject : objectArray)
        {
            Cell cell = row.createCell(cellnum++);
            if(cellObject instanceof Double) 
            {
                Double cellValue = (Double)cellObject;
                if(cellValue.equals(Double.NEGATIVE_INFINITY))
                    cell.setCellValue("ERROR!");
                else cell.setCellValue(cellValue);
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
