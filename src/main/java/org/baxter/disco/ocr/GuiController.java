package org.baxter.disco.ocr;

import java.util.List;

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;

/**
 * Controller portion of MVC for Accuracy over Life test fixture.
 * Mostly wrapper interface between View and Model.
 *
 * {@link GuiView}, GuiController, and {@link GuiModel} versions are tied together, and are referred to collectively as Gui.
 *
 * @author Blizzard Finnegan
 * @version 0.2.0, 06 Feb, 2023
 */
public class GuiController
{
    /**
     * Wrapper function to get available cameras.
     *
     * @return List[String] of the names of cameras
     */
    public static List<String> getCameras()
    { return GuiModel.getCameras(); }

    /**
     * Wrapper function used to show an image in a separate window
     *
     * @param cameraName    The camera whose image should be shown.
     */
    public static String showImage(String cameraName)
    { return GuiModel.showImage(cameraName); }

    /**
     * Wrapper function to toggle cropping for a given camera
     *
     * @param cameraName    The camera whose image should be shown.
     */
    public static void toggleCrop(String cameraName)
    { GuiModel.toggleCrop(cameraName); }

    /**
     * Wrapper function to toggle threshold for a given camera
     *
     * @param cameraName    The camera whose image should be shown.
     */
    public static void toggleThreshold(String cameraName)
    { GuiModel.toggleThreshold(cameraName); }

    /**
     * Wrapper function to save the default config 
     */
    public static void saveDefaults()
    { GuiModel.saveDefaults(); }

    /**
     * Wrapper function to save the current config
     */
    public static void save()
    { GuiModel.save(); }

    /**
     * Wrapper function to save the current config, and re-enable image processing if necessary.
     */
    public static void saveClose()
    { GuiModel.save(); GuiModel.enableProcessing(); }

    /**
     * Wrapper function to get a config value, for a given camera 
     *
     * @param cameraName    The name of the camera being inspected
     * @param property      The config property to be returned
     *
     * @return String of the value of the current object
     */
    public static String getConfigString(String cameraName, ConfigProperties property)
    { return GuiModel.getConfigString(cameraName,property); }

    /**
     * Wrapper function to get a config value, for a given camera 
     *
     * @param cameraName    The name of the camera being inspected
     * @param property      The config property to be returned
     *
     * @return String of the value of the current object
     */
    public static double getConfigValue(String cameraName, ConfigProperties property)
    { return GuiModel.getConfigValue(cameraName,property); }

    /**
     * Wrapper function to set a config value for a given camera.
     *
     * @param cameraName    The name of the camera being modified
     * @param property      The property to be modified
     * @param value         The new value to set the property to 
     */
    public static void setConfigValue(String cameraName, ConfigProperties property, double value)
    { 
        GuiModel.setConfigVal(cameraName,property,value); 
        if(property == ConfigProperties.CROP_W)
            GuiView.updateImageViewWidth(cameraName);
        if(property == ConfigProperties.CROP_H)
            GuiView.updateImageViewHeight(cameraName);
    }

    /**
     * Setter for the number of iterations
     *
     * @param iterationCount    The new iteration count to be saved
     */
    public static void setIterationCount(int iterationCount)
    { GuiModel.setIterations(iterationCount); }

    /**
     * Wrapper function to interrupt testing.
     */
    public static void interruptTests()
    { GuiModel.interruptTesting(); }

    /**
     * Wrapper function to run tests.
     */
    public static void runTests()
    { GuiModel.runTests(); }

    /**
     * Wrapper function to test the movement of the fixture.
     */
    public static void testMotions()
    { 
        testingMotions();
        GuiModel.testMovement(); 
    }

    /**
     * If the Model is ready, set the view to allow the Start button to be pressed.
     */
    public static void updateStart()
    {
        boolean ready = GuiModel.isReady();
        GuiView.getStart().setDisable(ready);
        if(ready) GuiView.getStart().setTooltip(new Tooltip("Start running automated testing."));
    }

    /**
     * Wrapper function to write to the user feedback Label, stating that the test is starting.
     */
    public static void startTests()
    { userUpdate("Starting tests..."); }

    /**
     * Updates the View's state wit hthe current iteration count
     */
    public static void updateIterations()
    {
        String newIterations = Integer.toString(GuiModel.getIterations());
        GuiView.getIterationField().setText(newIterations);
    }

    /**
     * Update a given config value, given a camera
     *
     * @param cameraName    The name of the camera being updated
     * @param property      The property being updated
     */
    public static void updateConfigValue(String cameraName, ConfigProperties property)
    {
        TextField field = GuiView.getField(cameraName,property);
        field.setText(GuiModel.getConfigString(cameraName,property));
        field.setPromptText(GuiModel.getConfigString(cameraName,property));
    }

    /**
     * Wrapper function to write the current iteration ot the user feedback Label.
     *
     * @param index     The current iteration number
     */
    public static void runningUpdate(int index)
    { userUpdate("Running iteration " + index + "..."); }

    /**
     * Wrapper function used to set a custom message to the user.
     *
     * @param output    What should be sent to the user.
     */
    public static void userUpdate(String output) 
    { GuiView.getFeedbackText().setText(output); }

    /**
     * Wrapper function to tell the user that fixture movement testing has begun.
     */
    public static void testingMotions()
    { userUpdate("Testing fixture movement..."); }

    /**
     * Wrapper function to tell the user that fixture movement testing was successful.
     */
    public static void testingMotionSuccessful()
    { userUpdate("Fixture movement test successful!"); } 

    /**
     * Wrapper function to tell the user that fixture movement testing failed, and where it failed.
     *
     * @param failurePoint  Where the movement failed.
     */
    public static void testingMotionUnsuccessful(String failurePoint)
    { userUpdate("Fixture movement unsuccessful! Fail point: " + failurePoint);}

    /**
     * Wrapper function for the Model's pressButton function.
     */
    public static void pressButton()
    { GuiModel.pressButton(); }

    /**
     * Wrapper function used to update whether or not the DUTs should be primed before testing.
     */
    public static void updatePrime()
    { GuiModel.updatePrime(); }

    /**
     * Wrapper function to set the Serial for a given camera.
     *
     * @param cameraName    The name of the camera to modify
     * @param serial        The serial of the DUT under the given camera
     */
    public static void setSerial(String cameraName, String serial)
    { GuiModel.setSerial(cameraName,serial); }

    /**
     * Getter for the current iteration count 
     *
     * @return String of the current iteration count.
     */
    public static String getIterationCount()
    { return Integer.toString(GuiModel.getIterations()); }

    /**
     * Wrapper function around the GuiModel's calibrateCameras function
     */
    public static void calibrateCameras()
    { GuiModel.calibrateCameras(); }

    /**
     * Close function for the Model; used to end the program
     */
    public static void closeModel() { GuiModel.close(); }

    /**
     * Function used to update the ImageView of the GUID
     *
     * @param cameraName    Name of the camera the image is from 
     * @param fileURL       The URL of the file to be shown
     */
    public static void updateImage(String cameraName, String fileURL)
    {
        GuiView.getViewMap().get(cameraName).setImage(new Image(fileURL));
    }
}
