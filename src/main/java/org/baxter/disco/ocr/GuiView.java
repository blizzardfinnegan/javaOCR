package org.baxter.disco.ocr;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

/**
 * View portion of MVC for the Accuracy Over Life test fixture.
 *
 * @author Blizzard Finnegan
 * @version 0.0.1, 01 Feb, 2023
 */
public class GuiView extends Application
{
    public static final Scene MAIN_MENU; 
    private static final AnchorPane MAIN_ANCHOR;
    private static final Pane MAIN_PANE;

    public static final Scene CAMERA_MENU; 
    private static final AnchorPane CAMERA_ANCHOR;
    private static final Pane CAMERA_PANE;

    private static final Map<String,Map<ConfigProperties,TextField>> uiFields = new HashMap<>();

    private static Text userFeedback;

    private static TextField iterationField;

    private static Button startButton;

    private static Stage STAGE;

    public static void main(String[] args) { launch(args); }
    
    static
    {
        ErrorLogging.logError("START OF PROGRAM");
        ErrorLogging.logError("Setting up main menu...");
        MAIN_ANCHOR = new AnchorPane();
        MAIN_ANCHOR.setMinWidth(Double.NEGATIVE_INFINITY);
        MAIN_ANCHOR.setMinHeight(Double.NEGATIVE_INFINITY);
        MAIN_PANE = new Pane();
        AnchorPane.setTopAnchor(MAIN_PANE,10.0);
        AnchorPane.setLeftAnchor(MAIN_PANE,10.0);
        AnchorPane.setRightAnchor(MAIN_PANE,10.0);
        AnchorPane.setBottomAnchor(MAIN_PANE,10.0);
        MAIN_ANCHOR.getChildren().add(MAIN_PANE);
        MAIN_MENU = new Scene(MAIN_ANCHOR);

        ErrorLogging.logError("Setting up camera config menu...");
        CAMERA_ANCHOR = new AnchorPane();
        CAMERA_ANCHOR.setMinWidth(Double.NEGATIVE_INFINITY);
        CAMERA_ANCHOR.setMinHeight(Double.NEGATIVE_INFINITY);
        CAMERA_PANE = new Pane();
        AnchorPane.setTopAnchor(CAMERA_PANE,10.0);
        AnchorPane.setLeftAnchor(CAMERA_PANE,10.0);
        AnchorPane.setRightAnchor(CAMERA_PANE,10.0);
        AnchorPane.setBottomAnchor(CAMERA_PANE,10.0);
        CAMERA_ANCHOR.getChildren().add(CAMERA_PANE);
        CAMERA_MENU = new Scene(CAMERA_ANCHOR);

        for(String camera : GuiModel.getCameras())
            uiFields.put(camera, new HashMap<>());
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        ErrorLogging.logError("Finalising GUI...");
        STAGE = stage;
        mainMenuBuilder();
        cameraMenuBuilder();
        STAGE.setScene(MAIN_MENU);
        STAGE.show();
        ErrorLogging.logError("Gui loading complete.");
    }

    private static void cameraMenuBuilder()
    {
        VBox layout = new VBox();
        layout.setSpacing(5.0);
        layout.setAlignment(Pos.CENTER_LEFT);

        int index = 0;
        for(String cameraName : GuiModel.getCameras())
        {
            if(index != 0) layout.getChildren().add(new Separator(Orientation.HORIZONTAL));
            layout.getChildren().add(cameraSetup(cameraName));
            index++;
        }
        layout.getChildren().add(cameraMenuButtons());
        CAMERA_PANE.getChildren().add(layout);
    }

    private static void mainMenuBuilder()
    {
        VBox layout = new VBox();
        layout.getChildren().addAll(topHalf(),
                                    new Separator(Orientation.HORIZONTAL),
                                    bottomHalf());
        MAIN_PANE.getChildren().add(layout);
    }


    private static VBox topHalf()
    {
        VBox output = new VBox();
        output.setSpacing(5.0);
        output.getChildren().addAll(topButtons(),
                                    new Separator(Orientation.HORIZONTAL),
                                    setupSection(),
                                    primeCheckbox(),
                                    testFeedback());
        return output;
    }

    private static CheckBox primeCheckbox()
    {
        CheckBox output = new CheckBox("Prime devices");
        output.setTooltip(new Tooltip("This presses the button on the device under test twice for every iteration."));
        output.setId("primeCheckbox");
        output.selectedProperty().addListener(
                (obeservableValue, oldValue, newValue) ->
                {
                    GuiController.updatePrime();
                });
        return output;
    }

    private static HBox testFeedback()
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        Label textboxLabel = new Label("Test feedback: ");
        Text textbox = new Text("Awaiting input...");
        userFeedback = textbox;
        textbox.setId("testOutputToUser");

        output.getChildren().addAll(textboxLabel,textbox);
        return output;
    }

    private static HBox setupSection()
    {
        HBox output = userTextField("Cycles:",GuiController.getIterationCount(), "Enter the number of times to test the devices in the fixture.");
        TextField field = null;
        for(Node child : output.getChildren())
        {
            if(child instanceof TextField)
            {
                field = (TextField)child;
                break;
            }
        }
        if(field == null)
        {
            ErrorLogging.logError("GUI INIT ERROR!!! - Failed text field setup.");
            GuiController.closeModel();
        }

        iterationField = field;
        //TextField textField = (TextField)(output.lookup("#cycles"));
        field.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { GuiController.setIterationCount(sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    newValue = oldValue;
                }
            });

        return output;
    }

    private static HBox topButtons()
    {
        HBox topButtons = new HBox();
        topButtons.setSpacing(5.0);
        topButtons.setAlignment(Pos.CENTER);
        topButtons.setMinWidth(Region.USE_COMPUTED_SIZE);
        topButtons.setMinHeight(Region.USE_COMPUTED_SIZE);

        final Button START = buttonBuilder("Start",true);
        startButton = START;
        final Button STOP = buttonBuilder("Stop",true);
        START.setOnAction( (event) -> 
            {
                START.setDisable(true);
                STOP.setDisable(false);
                GuiController.runTests();
            });
        START.setTooltip(new Tooltip("Configure cameras to start the program."));

        STOP.setOnAction( (event) -> 
            {
                GuiController.interruptTests();
                START.setDisable(false);
                STOP.setDisable(true);
            });

        STOP.setTooltip(new Tooltip("Pauses current iteration."));

        Button calibrateCamera = buttonBuilder("Calibrate Cameras",false);
        calibrateCamera.setOnAction( (event) -> STAGE.setScene(CAMERA_MENU) );

        Button testMovement = buttonBuilder("Test Movement",false);
        testMovement.setOnAction( (event) -> GuiController.testMotions() );

        Button cancel = buttonBuilder("Close",false);
        cancel.setOnAction( (event) -> 
            {
                GuiController.closeModel();
                STAGE.close();
            });


        topButtons.getChildren().addAll(START,
                                        STOP,
                                        calibrateCamera,
                                        testMovement,
                                        cancel);
        return topButtons;
    }

    private static HBox bottomHalf()
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        output.setSpacing(5.0);

        int index = 0;
        for(String camera : GuiModel.getCameras())
        {
            if(index != 0) output.getChildren().add(new Separator(Orientation.VERTICAL));
            output.getChildren().add(camera(camera));
        }
        return output;
    }

    private static VBox camera(String cameraName)
    {
        VBox output = new VBox();
        output.setAlignment(Pos.CENTER_LEFT);
        output.setSpacing(5.0);
        //HBox serialNumber = userTextField("DUT Serial Number:","","Enter the serial number for the device under test.");
        output.getChildren().addAll(cameraHeader(cameraName),
                                    //serialNumber,
                                    cameraView(cameraName));
        return output;
    }

    private static HBox cameraHeader(String cameraName)
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        output.setSpacing(5.0);
        output.getChildren().addAll(cameraCheckbox("Camera: " + cameraName));
        return output;
    }

    private static HBox cameraCheckbox(String prompt)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER);
        output.setAlignment(Pos.CENTER);
        Label label = new Label(prompt);
        CheckBox checkBox = new CheckBox("Active");
        checkBox.setId(prompt.toLowerCase());
        output.getChildren().addAll(label,
                                    checkBox);
        return output;
    }

    private static HBox cameraView(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("OCR Read:");
        Label ocrRead = new Label("[ ]");
        ocrRead.setId("cameraOCR-" + cameraName);
        ImageView imageView = new ImageView();
        output.getChildren().addAll(label,
                                    ocrRead,
                                    imageView);
        return output;
    }

    private static VBox cameraSetup(String cameraName)
    {
        VBox output = new VBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);

        Label sectionHeader = new Label("Camera: " + cameraName);
        output.getChildren().addAll(sectionHeader,
                                    processingInputs(cameraName),
                                    cropInputs(cameraName),
                                    miscInputs(cameraName));
        return output;
    }

    private static HBox processingInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);

        Button preview = buttonBuilder("Preview");
        preview.setId("previewButton-" + cameraName);
        preview.setOnAction( (event) -> 
            {
                GuiController.pressButton();
                try{ Thread.sleep(2000); } catch(Exception e){ ErrorLogging.logError(e); }
                GuiController.showImage(cameraName);
            });

        CheckBox cropPreview = new CheckBox("Crop preview");
        cropPreview.setId("cropToggle-" + cameraName);
        cropPreview.selectedProperty().addListener((obeservableValue, oldValue, newValue) -> 
            GuiController.toggleCrop(cameraName));

        CheckBox thresholdPreview = new CheckBox("Threshold preview");
        thresholdPreview.selectedProperty().addListener((obeservableValue, oldValue, newValue) ->
                    GuiController.toggleThreshold(cameraName));
        cropPreview.setId("thresholdToggle-" + cameraName);

        output.getChildren().addAll(preview,
                                    cropPreview,
                                    thresholdPreview);
        return output;
    }

    private static HBox cropInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);

        HBox cropX = userTextField("X:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_X),
                                   "X-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropX,ConfigProperties.CROP_X,cameraName,"X");

        HBox cropY = userTextField("Y:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_Y),
                                   "Y-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropY,ConfigProperties.CROP_Y,cameraName,"Y");
        
        HBox cropW = userTextField("Width:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_W),
                                   "Width, in pixels, of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropW,ConfigProperties.CROP_W,cameraName,"Width");

        HBox cropH = userTextField("Height:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_H),
                                   "Height, in pixels, of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropH, ConfigProperties.CROP_H, cameraName, "Height");

        output.getChildren().addAll(cropX,
                                    cropY,
                                    cropW,
                                    cropH);
        return output;
    }

    private static HBox miscInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);

        HBox thresholdValue = userTextField("Threshold Value:",
                                            GuiController.getConfigValue(cameraName,ConfigProperties.THRESHOLD),
                                            "This value can be set from 0 to 255. Higher values mean more black in "+
                                            "the thresholded image. For more information, see the documentation.");
        textFieldSetup(thresholdValue,ConfigProperties.THRESHOLD_VALUE,cameraName,"Threshold Value");

        HBox compositeFrames = userTextField("Composite Frames:",
                                            GuiController.getConfigValue(cameraName,ConfigProperties.COMPOSITE_FRAMES),
                                             "Number of frames to bitwise-and together.");
        textFieldSetup(compositeFrames,ConfigProperties.COMPOSITE_FRAMES,cameraName,"Composite Frames");

        output.getChildren().addAll(thresholdValue,
                                    compositeFrames);
        return output;
    }

    private static HBox cameraMenuButtons()
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        output.setSpacing(10.0);

        Button defaults = buttonBuilder("Save Defaults");
        defaults.setOnAction( (event) ->
            {
                GuiController.saveDefaults();
            });

        Button save = buttonBuilder("Save");
        save.setOnAction( (event) ->
            {
                GuiController.save();
            });

        Button saveClose = buttonBuilder("Save and Close");
        saveClose.setOnAction( (event) -> 
            {
                GuiController.saveClose();
                STAGE.setScene(MAIN_MENU);
            });

        Button close = buttonBuilder("Close without Saving");
        close.setOnAction( (event) -> 
            {
                STAGE.setScene(MAIN_MENU);
            });

        output.getChildren().addAll(defaults,
                                    save,
                                    saveClose,
                                    close);
        return output;
    }

    private static void textFieldSetup(HBox hbox, ConfigProperties property, String cameraName, String oldId)
    {
        TextField field = null;
        for(Node child : hbox.getChildren())
        {
            if(child instanceof TextField)
            {
                field = (TextField)child;
                break;
            }
        }
        if(field == null)
        {
            ErrorLogging.logError("GUI INIT ERROR!!! - Failed text field setup.");
            GuiController.closeModel();
        }

        //GuiController.addToMap(cameraName,property,field);
        Map<ConfigProperties,TextField> cameraFields = uiFields.get(cameraName);
        if(cameraFields.containsKey(property))
        { ErrorLogging.logError("GUI Setup Error!!! - Duplicate field: " + cameraName + " " + property.getConfig()); }
        cameraFields.put(property,field);
        uiFields.replace(cameraName,cameraFields);
        field.setId(property.getConfig() + cameraName);
        field.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { GuiController.setConfigValue(cameraName,property,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in " + property.getConfig() + " for " + cameraName +  ".");
                    newValue = oldValue;
                }
            });
    }

    private static Button buttonBuilder(String name,boolean disabled)
    {
        String[] id = name.strip().substring(0, name.length() - 1).toLowerCase().strip().split(" ");
        Button button = new Button(name);
        button.setId(id[0]);
        button.setDisable(disabled);
        return button;
    }

    private static Button buttonBuilder(String name)
    { return buttonBuilder(name,false); }

    private static HBox userTextField(String prompt, String baseValue, String description)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        output.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(prompt);
        TextField field = new TextField();
        String[] id = prompt.strip().substring(0, prompt.length() - 1).toLowerCase().strip().split(" ");
        field.setId(id[0]);
        output.setId(id[0] + "-box");
        field.setPromptText(baseValue);
        Tooltip tooltip = new Tooltip(description);
        field.setTooltip(tooltip);
        label.setTooltip(tooltip);
        output.getChildren().addAll(label,field);
        return output;
    }

    public static TextField getField(String cameraName, ConfigProperties property)
    { return uiFields.get(cameraName).get(property); }

    public static Button getStart()
    { return startButton; }

    public static Text getFeedbackText()
    { return userFeedback; }

    public static TextField getIterationField()
    { return iterationField; }
}
