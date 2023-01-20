package org.baxter.disco.ocr;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Class containing a single function for easily writing errors to a log file, as well as stderr.
 *
 * @author Blizzard Finnegan
 * @version 19 Jan. 2023
 */

public class ErrorLogging
{
    public static String logFile;
    private static FileWriter fw;
    private static BufferedWriter bw;
    private static PrintWriter fileOut;
    private static DateTimeFormatter datetime;

    static
    {
        try
        {
        fw = new FileWriter(logFile, true);
        bw = new BufferedWriter(fw);
        fileOut = new PrintWriter(bw);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
        datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Logs error thrown by runtime.
     *
     * @param error     Pass in the appropriate error,
     *                  for it to be parsed and logged.
     */
    public static void logError(Throwable error)
    {
        String errorMessage = datetime.format(LocalDateTime.now()) + error.toString();
        fileOut.println(errorMessage);
        System.err.println(errorMessage);
    }

    /**
     * Logs error manually caught by user.
     *
     * @param error     Pass in the necessary error information,
     *                  as a string.
     */
    public static void logError(String error)
    {
        String errorMessage = datetime.format(LocalDateTime.now()) + error;
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
