package org.baxter.disco.ocr;

import java.util.Scanner;

import javafx.application.Application;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

/**To be implemented;
 * GUI for the underlying functions.
 *
 * @author Blizzard Finnegan
 * @version 0.1.0, 30 Jan, 2023
 * 
 */
public class Gui extends Application
{
    public static final Scene MAIN_MENU; 
    private static final AnchorPane MAIN_ANCHOR;
    private static final Pane MAIN_PANE;

    public static final Scene CAMERA_MENU; 
    private static final AnchorPane CAMERA_ANCHOR;
    private static final Pane CAMERA_PANE;

    private static Stage STAGE;

    private static int iterationCount = 3;

    public static void main(String[] args) { launch(args); }
    
    static
    {
        ErrorLogging.logError("START OF PROGRAM");
        ErrorLogging.logError("Initialising main menu...");
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

        ErrorLogging.logError("Initialising camera config menu...");
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
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        ErrorLogging.logError("Gui Initialised!");
        STAGE = stage;
        mainMenuBuilder();
        cameraMenuBuilder();
        STAGE.setScene(MAIN_MENU);
        STAGE.show();
        ErrorLogging.logError("DEBUG: Gui loading complete.");
    }

    private static void cameraMenuBuilder()
    {
        VBox layout = new VBox();
        for(String cameraName : OpenCVFacade.getCameraNames())
        {
            layout.getChildren().addAll(cameraSetup(cameraName),
                                        new Separator(Orientation.HORIZONTAL));
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
                    for(String cameraName : OpenCVFacade.getCameraNames())
                    {
                        ConfigFacade.setValue(cameraName,ConfigProperties.PRIME,
                                (newValue ? 1 : 0) );
                    }
                });
        return output;
    }

    private static HBox testFeedback()
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        Label textboxLabel = new Label("Test feedback: ");
        Text textbox = new Text("Awaiting input...");
        textbox.setId("testOutputToUser");

        output.getChildren().addAll(textboxLabel,textbox);
        return output;
    }

    private static HBox setupSection()
    {
        HBox output = userTextField("Cycles",Integer.toString(iterationCount), "Enter the number of times to test the devices in the fixture.");
        TextField textField = (TextField)output.lookup("#cycles");
        textField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { iterationCount = sc.nextInt(); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    newValue = oldValue;
                }
            });
        return output;
    }

    private static HBox userTextField(String prompt, String baseValue, String description)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        Label label = new Label(prompt);
        TextField field = new TextField();
        String[] id = prompt.toLowerCase().strip().split(" ");
        field.setId(id[0]);
        field.setPromptText(baseValue);
        Tooltip tooltip = new Tooltip(description);
        field.setTooltip(tooltip);
        label.setTooltip(tooltip);
        output.getChildren().addAll(label,field);
        return output;
    }

    private static HBox topButtons()
    {
        HBox topButtons = new HBox();
        topButtons.setSpacing(5.0);
        topButtons.setAlignment(Pos.CENTER);
        topButtons.setMinWidth(Region.USE_COMPUTED_SIZE);
        topButtons.setMinHeight(Region.USE_COMPUTED_SIZE);

        Button start = buttonBuilder("Start",true);
        buttonBuilder("Start",true);
        start.setOnAction( (event) -> Cli.runTests() );

        Button stop = buttonBuilder("Stop",true);
        stop.setOnAction( (event) ->
            {
                //TODO: Implement multithreading
            });
        Button calibrateCamera = buttonBuilder("Calibrate Cameras",false);
        calibrateCamera.setOnAction( (event) -> STAGE.setScene(CAMERA_MENU) );

        Button testMovement = buttonBuilder("Test Movement",false);
        testMovement.setOnAction( (event) -> MovementFacade.testMotions() );

        Button cancel = buttonBuilder("Close",false);
        cancel.setOnAction( (event) -> 
            {
                Cli.close();
                STAGE.close();
            });


        topButtons.getChildren().addAll(start,
                                        stop,
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
        for(String camera : OpenCVFacade.getCameraNames())
        {
            output.getChildren().addAll(camera(camera),
                                        new Separator(Orientation.HORIZONTAL));
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
        output.getChildren().addAll(cameraCheckbox(cameraName + "Camera "));
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
        Label sectionHeader = new Label(cameraName + "Camera ");
        output.getChildren().addAll(sectionHeader,
                                    processingInputs(cameraName),
                                    cropInputs(cameraName),
                                    miscInputs(cameraName));
        return output;
    }

    private static HBox processingInputs(String cameraName)
    {
        HBox output = new HBox();
        Button preview = buttonBuilder("Preview");
        preview.setId("previewButton-" + cameraName);
        preview.setOnAction( (event) -> OpenCVFacade.showImage(cameraName));

        CheckBox cropPreview = new CheckBox("Crop preview");
        cropPreview.setId("cropToggle-" + cameraName);
        cropPreview.selectedProperty().addListener(
                (obeservableValue, oldValue, newValue) ->
                {
                    double configValue = ConfigFacade.getValue(cameraName,ConfigProperties.CROP);
                    configValue = Math.abs(configValue - 1);
                    ConfigFacade.setValue(cameraName,ConfigProperties.CROP,configValue);
                });

        CheckBox thresholdPreview = new CheckBox("Threshold preview");
        thresholdPreview.selectedProperty().addListener(
                (obeservableValue, oldValue, newValue) ->
                {
                    double configValue = ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD);
                    configValue = Math.abs(configValue - 1);
                    ConfigFacade.setValue(cameraName,ConfigProperties.THRESHOLD,configValue);
                });
        cropPreview.setId("thresholdToggle-" + cameraName);

        output.getChildren().addAll(preview,
                                    cropPreview,
                                    thresholdPreview);
        return output;
    }

    private static HBox cropInputs(String cameraName)
    {
        HBox output = new HBox();
        HBox cropX = userTextField("X",
                                    Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
                                   "X-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropX,ConfigProperties.CROP_X,cameraName,"X");

        HBox cropY = userTextField("Y",
                                    Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
                                   "Y-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropY,ConfigProperties.CROP_Y,cameraName,"Y");
        
        HBox cropW = userTextField("Width",
                                    Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
                                   "Width, in pixels, of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropW,ConfigProperties.CROP_W,cameraName,"Width");

        HBox cropH = userTextField("Height",
                                    Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
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

        HBox thresholdValue = userTextField("Threshold Value",
                                            Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
                                            "This value can be set from 0 to 255. Higher values mean more black in "+
                                            "the thresholded image. For more information, see the documentation.");
        textFieldSetup(thresholdValue,ConfigProperties.THRESHOLD_VALUE,cameraName,"Threshold Value");

        HBox compositeFrames = userTextField("Composite Frames",
                                            Double.toString(ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X)),
                                             "Number of frames to bitwise-and together.");
        textFieldSetup(compositeFrames,ConfigProperties.COMPOSITE_FRAMES,cameraName,"Composite Frames");

        output.getChildren().addAll(thresholdValue,
                                    compositeFrames);
        return output;
    }

    private static HBox cameraMenuButtons()
    {
        HBox output = new HBox();

        Button defaults = buttonBuilder("Set to Defaults");

        Button save = buttonBuilder("Save");

        Button saveClose = buttonBuilder("Save and Close");

        Button close = buttonBuilder("Close without Saving");

        output.getChildren().addAll(defaults,
                                    save,
                                    saveClose,
                                    close);
        return output;
    }

    private static void textFieldSetup(HBox hbox, ConfigProperties property, String cameraName, String oldId)
    {
        String[] id = oldId.toLowerCase().strip().split(" ");
        TextField field = (TextField)hbox.lookup("#" + id[0]);
        field.setId(property.getConfig() + cameraName);
        field.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,property,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in " + property.getConfig() + " for " + cameraName +  ".");
                    newValue = oldValue;
                }
            });
    }
    private static Button buttonBuilder(String name,boolean disabled)
    {
        String[] id = name.toLowerCase().strip().split(" ");
        Button button = new Button(name);
        button.setId(id[0]);
        button.setFont(Font.getDefault());
        if (disabled) button.disableProperty();
        return button;
    }

    private static Button buttonBuilder(String name)
    { return buttonBuilder(name,false); }
}
