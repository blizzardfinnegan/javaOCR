package org.baxter.disco.ocr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Model portion of MVC for the Accuracy Over Life test fixture.
 * Primarily a wrapper around other classes, but does store some information.
 *
 * @author Blizzard Finnegan
 * @version 0.0.1, 01 Feb, 2023
 */
public class GuiModel
{
    private static boolean readyToRun = false;

    private static int iterationCount = 3;

    public static final Lock LOCK = new ReentrantLock();

    private static Thread testingThread = new Thread();

    private static final MovementFacade fixture = new MovementFacade(LOCK);

    public static void ready() { readyToRun = true; GuiController.updateStart(); }

    public static boolean isReady() { return readyToRun; }

    public static void setIterations(int iterations) 
    { 
        iterationCount = iterations; 
        GuiController.updateIterations(); 
    }

    public static int getIterations() { return iterationCount; }

    public static void testMovement() 
    { 
        GuiController.testingMotions();
        boolean success = fixture.testMotions(); 
        if(success) GuiController.testingMotionSuccessful();
        else GuiController.testingMotionUnsuccessful("Unknown");
    }

    public static List<String> getCameras()
    { return new ArrayList<>(OpenCVFacade.getCameraNames()); }

    public static void showImage(String cameraName) { OpenCVFacade.showImage(cameraName); }

    public static void setConfigVal(String cameraName, ConfigProperties property, double value)
    { 
        ConfigFacade.setValue(cameraName,property,value); 
        GuiController.updateConfigValue(cameraName,property);
    }

    public static String getConfigVal(String cameraName, ConfigProperties property)
    { return Double.toString(ConfigFacade.getValue(cameraName,property)); }

    public static void pressButton()
    { fixture.pressButton(); }

    public static void updatePrime()
    {
        for(String cameraName : OpenCVFacade.getCameraNames())
        {
            boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
            ConfigFacade.setValue(cameraName,ConfigProperties.PRIME,(old ? 1 : 0));
        }
    }

    public static void enableProcessing()
    {
        for(String camera : getCameras())
        {
            ConfigFacade.setValue(camera,ConfigProperties.CROP, 1.0);
            ConfigFacade.setValue(camera,ConfigProperties.THRESHOLD, 1.0);
        }
    }

    public static void saveDefaults()
    { ConfigFacade.saveDefaultConfig(); }

    public static void save()
    { ConfigFacade.saveCurrentConfig(); ConfigFacade.loadConfig(); }

    public static void toggleThreshold(String cameraName)
    {
        boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
        ConfigFacade.setValue(cameraName,ConfigProperties.THRESHOLD,(old ? 1 : 0));
    }

    public static void toggleCrop(String cameraName)
    {
        boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP,(old ? 1 : 0));
    }

    public static void runTests()
    {
        testingThread = new Thread(() ->
        {
            DataSaving.initWorkbook(ConfigFacade.getOutputSaveLocation());
            boolean prime = false;
            for(String cameraName : OpenCVFacade.getCameraNames())
            {
                if(ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) != 0)
                {
                    prime = true;
                }
            }
            for(int i = 0; i < iterationCount; i++)
            {
                Map<String, Double> resultMap = new HashMap<>();
                LOCK.lock();
                fixture.iterationMovement(prime);
                LOCK.unlock();
                LOCK.lock();
                List<File> iteration = OpenCVFacade.singleIteration();
                LOCK.unlock();
                for(File file : iteration)
                {
                    LOCK.lock();
                    Double result = TesseractFacade.imageToDouble(file);
                    LOCK.unlock();
                    LOCK.lock();
                    String fileLocation = file.getAbsolutePath();
                    LOCK.unlock();
                    LOCK.lock();
                    resultMap.put(fileLocation,result);
                    LOCK.unlock();
                    LOCK.lock();
                    ErrorLogging.logError("DEBUG: Tesseract final output: " + result);
                    LOCK.unlock();
                }
                LOCK.lock();
                DataSaving.writeValues(i,resultMap);
                LOCK.unlock();
                GuiController.runningUpdate(i);
            }
            //println("=======================================");
            ErrorLogging.logError("Testing complete!");
        });
        testingThread.run();
    }

    public static void close() 
    {
        ErrorLogging.logError("DEBUG: PROGRAM CLOSING.");
        fixture.closeGPIO();
        ErrorLogging.logError("DEBUG: END OF PROGRAM.");
        ErrorLogging.closeLogs();
    }

    public static void interruptTesting() { testingThread.interrupt(); }
}
