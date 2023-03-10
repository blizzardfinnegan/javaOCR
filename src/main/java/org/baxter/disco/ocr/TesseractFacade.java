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
 * @version 2.2.1, 27 Feb. 2023
 */
public class TesseractFacade
{
    
    /**
     * API object for Tesseract.
     */
    private static TessBaseAPI api;

    /**
     * OCR engine mode.
     *
     * From https://ai-facets.org/tesseract-ocr-best-practices/:
     *  0: Legacy engine only
     *  1: Neural nets Long Short-Term Memory (LSTM) engine only. This form of neural network has feedback, as well as feedforward within the design, allowing the neural network to learn from itself.
     *  2: Legacy + LSTM engines
     *  3: Default, based on what is available
     *
     * As I didn't write the training data, and don't actually know what kind of network the training set requires, this value is set to default.
     */
    private static final int OCR_ENGINE_MODE = 3;

    /**
     * OCR language name, or training data filename.
     */
    private static final String OCR_LANGUAGE = "Pro6_temp_test";

    /**
     * Location on the file system that the OCR languages are stored.
     *
     * This value requires that the folder "tessdata" be in the same location as your current working directory. 
     */
    private static final String OCR_LANGUAGE_LOCATION = "tessdata";

    static
    {
        api = new TessBaseAPI();
        api.Init(OCR_LANGUAGE_LOCATION, OCR_LANGUAGE, OCR_ENGINE_MODE);
    }

    /** 
     * Converts an image file to a double.
     *
     * @param file  File object of the image to be parsed by Tesseract.
     * @return Double, as read from the image by Tesseract. Anomalous data returns Double.NEGATIVE_INFINITY
     */
    public static double imageToDouble(File file)
    {
        double output = Double.NEGATIVE_INFINITY;

        PIX importedImage = pixRead(file.getAbsolutePath());
        api.SetImage(importedImage);
        String stringOutput = api.GetUTF8Text().getString();

        if(!stringOutput.isEmpty())
        {
            try( Scanner sc = new Scanner(stringOutput.trim()); )
            {
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
                        else if(output >= 200)  ErrorLogging.logError("OCR WARNING - OCR output is too high for DUT, potential misread.");
                        else            ErrorLogging.logError("OCR output successfully adjusted. Disregard warning.");
                    }
                    if(output <= -10)   ErrorLogging.logError("OCR ERROR!!! - OCR output is too low  for DUT, potential misread.");
                }
                else ErrorLogging.logError("OCR ERROR!!! - OCR output is not a Double.");
            }
        }
        return output;
    }
}
