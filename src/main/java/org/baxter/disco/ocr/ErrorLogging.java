package org.baxter.disco.ocr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class containing a single function for easily writing errors to a log file, 
 * as well as stderr.
 *
 * @author Blizzard Finnegan
 * @version 23 Jan. 2023
 */

public class ErrorLogging
{
    /**
     * Name of the location for the logfile.
     */
    private static String logFile;

    /**
     * Object used to write streams of characters to the file.
     *
     * This intermediate object is required to use {@link BufferedWriter} properly.
     */
    private static FileWriter fw;

    /**
     * Object used for buffering file write functions, for improved efficiency.
     */
    private static BufferedWriter bw;

    /**
     * Object called to write to the file.
     */
    private static PrintWriter fileOut;

    /**
     * Object used to format UNIX timestamps into human-readable values.
     */
    private static DateTimeFormatter datetime;

    static
    {
        datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
        logFile = datetime.format(LocalDateTime.now()) + "-log.txt";
        File outFile = new File(logFile);
        try
        {
            outFile.createNewFile();
            fw = new FileWriter(logFile, true);
            bw = new BufferedWriter(fw);
            fileOut = new PrintWriter(bw);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
    }

    /**
     * Logs error thrown by runtime.
     * Prepends the current date and time to the log line.
     *
     * @param error     Pass in the appropriate error,
     *                  for it to be parsed and logged.
     */
    public static void logError(Throwable error)
    {
        String errorMessage = datetime.format(LocalDateTime.now())  + " - " + error.toString();
        fileOut.println(errorMessage);
        System.err.println(errorMessage);
    }

    /**
     * Logs error manually caught by user.
     * Prepends the current date and time to the log line.
     * Particularly useful for catching potential errors that do not 
     * eplicitly throw an error.
     *
     * @param error     Pass in the necessary error information,
     *                  as a string.
     */
    public static void logError(String error)
    {
        String errorMessage = datetime.format(LocalDateTime.now())  + " - " + error;
        fileOut.println(errorMessage);
        System.err.println(errorMessage);
    }

    /**
     * Close all open logs. 
     *
     * !!! CALL ONCE, AT END OF PROGRAM !!!
     */
    public static void closeLogs() 
    { 
        try{}
        finally
        {
            if(fileOut != null) fileOut.close();
            try 
            {
            if(bw != null) bw.close();
            if(fw != null) fw.close();
            }
            catch(IOException e) 
            { /* This is being run because the program is closing. Errors here don't matter. */}
        }
    }
}
