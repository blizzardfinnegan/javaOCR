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
 * {@link GuiView}, {@link GuiController}, and GuiModel versions are tied together, and are referred to collectively as Gui.
 *
 * @author Blizzard Finnegan
 * @version 0.2.0, 06 Feb, 2023
 */
public class GuiModel
{
    /**
     * Whether or not the backend is prepared to start running.
     */
    private static boolean readyToRun = false;

    /**
     * The number of iterations.
     */
    private static int iterationCount = 3;

    /**
     * The Lock object, used for multithreading of the testing function
     */
    public static final Lock LOCK = new ReentrantLock();

    /**
     * The testing thread object
     */
    private static Thread testingThread = new Thread();

    /**
     * The Movement Facade instance
     */
    private static final MovementFacade fixture = new MovementFacade(LOCK);

    /**
     * The function called to define the GUI as ready to start testing.
     */
    public static void ready() { readyToRun = true; GuiController.updateStart(); }

    /**
     * Getter for {@link #readyToRun}
     *
     * @return boolean of whether or not testing can be started
     */
    public static boolean isReady() { return readyToRun; }

    /**
     * Setter for the number of iterations
     *
     * @param iterations The number of times to run the tests
     */
    public static void setIterations(int iterations) 
    { 
        iterationCount = iterations; 
        GuiController.userUpdate("Iterations set to: " + iterationCount);
        GuiController.updateIterations(); 
    }

    /**
     * Getter for the number of iterations 
     *
     * @return int of the number of iterations to be perfomed.
     */
    public static int getIterations() { return iterationCount; }

    /**
     * Wrapper around the MovementFacade's testMotions function.
     *
     * Updates the GUI with whether the testing was successful.
     */
    public static void testMovement() 
    { 
        GuiController.testingMotions();
        boolean success = fixture.testMotions(); 
        if(success) GuiController.testingMotionSuccessful();
        else GuiController.testingMotionUnsuccessful("Unknown");
    }

    /**
     * Getter for the list of cameras.
     *
     * @return List[String] of camera names.
     */
    public static List<String> getCameras()
    { return new ArrayList<>(OpenCVFacade.getCameraNames()); }

    /**
     * Wrapper function for showing an image.
     */
    public static void showImage(String cameraName) { OpenCVFacade.showImage(cameraName); }

    /**
     * Setter for a given camera's config value
     *
     * @param cameraName    Name of the camera to be configured
     * @param property      Property to be changed
     * @param value         New value for the given property
     */
    public static void setConfigVal(String cameraName, ConfigProperties property, double value)
    { 
        ConfigFacade.setValue(cameraName,property,value); 
        GuiController.updateConfigValue(cameraName,property);
    }

    /**
     * Getter for a given camera's config value
     *
     * @param cameraName    Name of the camera to get the config value from 
     * @param property      Property to get the value of
     *
     * @return String of the current value in the config
     */
    public static String getConfigVal(String cameraName, ConfigProperties property)
    { return Double.toString(ConfigFacade.getValue(cameraName,property)); }

    /**
     * Wrapper function around the MovementFacade's pressButton function.
     */
    public static void pressButton()
    { fixture.pressButton(); }

    /**
     * Function used to update whether or not cameras should be primed.
     */
    public static void updatePrime()
    {
        for(String cameraName : OpenCVFacade.getCameraNames())
        {
            boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
            ConfigFacade.setValue(cameraName,ConfigProperties.PRIME,(old ? 1 : 0));
        }
    }

    /**
     * Wrapper function to enable all image processing.
     */
    public static void enableProcessing()
    {
        for(String camera : getCameras())
        {
            ConfigFacade.setValue(camera,ConfigProperties.CROP, 1.0);
            ConfigFacade.setValue(camera,ConfigProperties.THRESHOLD, 1.0);
        }
    }

    /**
     * Wrapper function to save the default config values.
     */
    public static void saveDefaults()
    { ConfigFacade.saveDefaultConfig(); }

    /**
     * Save the current config, and ensure it is loaded properly.
     */
    public static void save()
    { ConfigFacade.saveCurrentConfig(); ConfigFacade.loadConfig(); }

    /**
     * Toggles the threshold processing for the given camera.
     *
     * @param cameraName    The name of the camera to be modified
     */
    public static void toggleThreshold(String cameraName)
    {
        boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
        ConfigFacade.setValue(cameraName,ConfigProperties.THRESHOLD,(old ? 1 : 0));
    }

    /**
     * Toggles the cropping of the image for the given camera.
     *
     * @param cameraName    The name of the camera to be modified
     */
    public static void toggleCrop(String cameraName)
    {
        boolean old = (ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) == 0.0 );
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP,(old ? 1 : 0));
    }

    /**
     * Function used to run all tests.
     *
     * Currently not working. Will need to rewrite.
     */
    public static void runTests()
    {
        //testingThread = new Thread(() ->
        //{
            GuiController.startTests();
            DataSaving.initWorkbook(ConfigFacade.getOutputSaveLocation(),OpenCVFacade.getCameraNames().size());
            boolean prime = false;
            List<String> cameraList = new ArrayList<>();
            for(String cameraName : OpenCVFacade.getCameraNames())
            {
                if(ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) != 0)
                {
                    prime = true;
                }
                cameraList.add(cameraName);
            }
            fixture.iterationMovement(prime);
            fixture.pressButton();
            fixture.iterationMovement(prime);
            Map<File,Double> resultMap = new HashMap<>();
            Map<String,File> cameraToFile = new HashMap<>();
            for(String cameraName : cameraList)
            {
                cameraToFile.put(cameraName,new File("/dev/null"));
            }
            for(int i = 0; i < iterationCount; i++)
            {
                while(!LOCK.tryLock()) {}
                fixture.iterationMovement(prime);
                LOCK.unlock();
                for(String cameraName : cameraList)
                {
                    while(!LOCK.tryLock()) {}
                    File file = OpenCVFacade.completeProcess(cameraName);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    cameraToFile.replace(cameraName,file);
                    LOCK.unlock();
                }
                LOCK.unlock();
                for(String cameraName : cameraList)
                {
                    while(!LOCK.tryLock()) {}
                    File file = cameraToFile.get(cameraName);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    Double result = TesseractFacade.imageToDouble(file);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    resultMap.put(file,result);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    ErrorLogging.logError("DEBUG: Tesseract final output: " + result);
                    LOCK.unlock();
                }
                while(!LOCK.tryLock()) {}
                DataSaving.writeValues(i,resultMap,cameraToFile);
                LOCK.unlock();
                GuiController.runningUpdate(i);
            }
            //println("=======================================");
            ErrorLogging.logError("Testing complete!");
        //});
        //testingThread.run();
    }

    /**
     * Wrapper function to close everything.
     */
    public static void close() 
    {
        ErrorLogging.logError("DEBUG: PROGRAM CLOSING.");
        fixture.closeGPIO();
        ErrorLogging.logError("DEBUG: END OF PROGRAM.");
        ErrorLogging.closeLogs();
    }

    /**
     * Function used to interrupt the testing thread.
     *
     * As of Gui 0.2.0, this does not work properly.
     */
    public static void interruptTesting() { testingThread.interrupt(); }

    /**
     * Function to set the serial number for a given camera 
     *
     * @param cameraName    name of the camera to be modified
     * @param serial        serial number to be set
     */
    public static void setSerial(String cameraName, String serial)
    { ConfigFacade.setSerial(cameraName,serial); }

    /**
     * Function to force fixture down before starting to calibrate the cameras.
     */
    public static void calibrateCameras()
    { fixture.goDown(); }
}
