package org.baxter.disco.ocr;

//Standard imports
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Error parsing import
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Class containing a single function for easily writing errors to a log file, 
 * as well as stderr.
 *
 * @author Blizzard Finnegan
 * @version 1.3.2, 07 Mar. 2023
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

    /**
     * Object used to format UNIX timestamps into human-readable values.
     */
    public static final DateTimeFormatter fileDatetime;

    //This will always run first, before anything else in this file
    static
    {
        //Make sure the filename formatter is compatible with Windows and Linux
        fileDatetime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

        datetime = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        logFile =  "logs/" + fileDatetime.format(LocalDateTime.now()) + "-log.txt";
        File logDirectory = new File("logs");
        File outFile = new File(logFile);
        try
        {
            logDirectory.mkdir();
            outFile.createNewFile();
            fw = new FileWriter(logFile, true);
            bw = new BufferedWriter(fw);
            fileOut = new PrintWriter(bw);
            System.setErr(new PrintStream(new FileOutputStream(logFile,true)));
        }
        catch (Exception e)
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
        String errorStackTrace = ExceptionUtils.getStackTrace(error);
        String errorMessage = datetime.format(LocalDateTime.now())  + " - " + errorStackTrace;
        fileOut.println(errorMessage);
        fileOut.flush();
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
        String errorMessage = datetime.format(LocalDateTime.now())  + "\t- " + error;
        fileOut.println(errorMessage);
        fileOut.flush();
        if(!error.substring(0,5).equals("DEBUG"))
            System.out.println(errorMessage);
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
            catch(Exception e) 
            { /* This is being run because the program is closing. Errors here don't matter. */}
        }
    }
}
