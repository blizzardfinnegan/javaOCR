package org.baxter.disco.ocr;

import java.util.List;

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Controller portion of MVC for Accuracy over Life test fixture.
 * Mostly wrapper interface between View and Model.
 *
 * @author Blizzard Finnegan
 * @version 0.0.1, 01 Feb, 2023
 */
public class GuiController
{
    public static List<String> getCameras()
    { return GuiModel.getCameras(); }

    public static void showImage(String cameraName)
    { GuiModel.showImage(cameraName); }

    public static void toggleCrop(String cameraName)
    { GuiModel.toggleCrop(cameraName); }

    public static void toggleThreshold(String cameraName)
    { GuiModel.toggleThreshold(cameraName); }

    public static void saveDefaults()
    { GuiModel.saveDefaults(); }

    public static void save()
    { GuiModel.save(); }

    public static void saveClose()
    { GuiModel.save(); GuiModel.enableProcessing(); }

    public static String getConfigValue(String cameraName, ConfigProperties property)
    { return GuiModel.getConfigVal(cameraName,property); }

    public static void setConfigValue(String cameraName, ConfigProperties property, double value)
    { GuiModel.setConfigVal(cameraName,property,value); }

    public static void setIterationCount(int iterationCount)
    { GuiModel.setIterations(iterationCount); }

    public static void interruptTests()
    { GuiModel.interruptTesting(); }

    public static void runTests()
    { GuiModel.runTests(); }

    public static void testMotions()
    { GuiModel.testMovement(); }

    public static void updateStart()
    {
        boolean ready = GuiModel.isReady();
        GuiView.getStart().setDisable(ready);
        if(ready) GuiView.getStart().setTooltip(new Tooltip("Start running automated testing."));
    }

    public static void updateIterations()
    {
        String newIterations = Integer.toString(GuiModel.getIterations());
        GuiView.getIterationField().setPromptText(newIterations);
        GuiView.getIterationField().setText(newIterations);
    }

    public static void updateConfigValue(String cameraName, ConfigProperties property)
    {
        TextField field = GuiView.getField(cameraName,property);
        field.setText(GuiModel.getConfigVal(cameraName,property));
        field.setPromptText(GuiModel.getConfigVal(cameraName,property));
    }

    public static void runningUpdate(int index)
    { userUpdate("Running iteration " + index + "..."); }

    public static void userUpdate(String output) 
    { GuiView.getFeedbackText().setText(output); }

    public static void testingMotions()
    { userUpdate("Testing fixture movement..."); }

    public static void testingMotionSuccessful()
    { userUpdate("Fixture movement test successful!"); } 

    public static void testingMotionUnsuccessful(String failurePoint)
    { userUpdate("Fixture movement unsuccessful! Fail point: " + failurePoint);}

    public static void pressButton()
    { GuiModel.pressButton(); }

    public static void updatePrime()
    { GuiModel.updatePrime(); }

    public static String getIterationCount()
    { return Integer.toString(GuiModel.getIterations()); }

    public static void closeModel() { GuiModel.close(); }
}
