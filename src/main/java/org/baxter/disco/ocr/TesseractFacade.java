package org.baxter.disco.ocr;

//Standard imports
import java.io.File;
import java.util.Scanner;

//Static import of image reader
import static org.bytedeco.leptonica.global.leptonica.pixRead;

//Import Tesseract-capable image class
import org.bytedeco.leptonica.PIX;

//Import Tesseract API
import org.bytedeco.tesseract.TessBaseAPI;

/**
 * Facade for Tesseract API.
 *
 * Wrapper around Tesseract API, feeding in defaults and necessary
 * information for this specific testing aparatus.
 *
 * @author Blizzard Finnegan
 * @version 2.2.0, 27 Feb. 2023
 */
public class TesseractFacade
{
    
    /**
     * API object for Tesseract.
     */
    private static TessBaseAPI api;

    static
    {
        //Initialise the Tesseract API
        api = new TessBaseAPI();

        //Magic number below.
        //The seemingly random 3 in the following line
        //is used to define the OCR Engine mode. 
        //This mode autoselects the OCR Engine, based on
        //available hardware.
        //This line also sets the location of the language 
        //files, and declares the language as "Pro6_temp_test".
        //Considering changing this to be more understandable, 
        //but potential consequences are unclear.
        api.Init("tessdata", "Pro6_temp_test", 3);
    }

    /** 
     * Converts an image file to a double.
     *
     * @param file  File object of the image to be parsed by Tesseract.
     * @return Double, as read from the image by Tesseract. Anomalous data returns Double.NEGATIVE_INFINITY
     */
    public static double imageToDouble(File file)
    {
        //Set default output
        double output = Double.NEGATIVE_INFINITY;

        //Import image, parse image
        PIX importedImage = pixRead(file.getAbsolutePath());
        api.SetImage(importedImage);
        String stringOutput = api.GetUTF8Text().getString();

        //Determine whether the OCR output is actually a double
        if(!stringOutput.isEmpty())
        {
            try(Scanner sc = new Scanner(stringOutput.trim());)
            {
                /*
                 * Discos have error messages (LO, HI, POS, ?). Consider parsing as well.
                 * Update on above note: Requires retraining Tesseract
                 */
                if(sc.hasNextDouble()) 
                {
                    output = sc.nextDouble();
                    if(output >= 200) 
                    {
                        ErrorLogging.logError("OCR WARNING - OCR output is too high for DUT, attempting to adjust...");
                        output = output / 10;
                        if(output >= 300)
                        {
                            output = output / 10;
                            ErrorLogging.logError("OCR output saved, as value appears to be real. Value needs to be verified.");
                        }
                        else if(output >= 200)
                            ErrorLogging.logError("OCR WARNING - OCR output is too high for DUT, potential misread.");
                        else
                            ErrorLogging.logError("OCR output successfully adjusted. Disregard warning.");
                    }
                    if(output <= -10) 
                        ErrorLogging.logError("OCR ERROR!!! - OCR output is too low  for DUT, potential misread.");
                }
                else ErrorLogging.logError("OCR ERROR!!! - OCR output is not a Double.");
            }
        }

        //Return output
        return output;
    }
}
