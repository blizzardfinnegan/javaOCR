package org.baxter.disco.ocr;

import java.util.List;
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
        CheckBox output = new CheckBox("Prime devices (pushes button twice)");
        output.setId("primeCheckbox");
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
        HBox output = userTextbox("Cycles",Integer.toString(iterationCount), "Enter the number of times to test the devices in the fixture.");
        TextField textField = (TextField)output.lookup("#cycles");
        textField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                if(!newValue.matches("\\d*")) textField.setText(oldValue);
                else 
                {
                    try(Scanner sc = new Scanner(newValue);)
                    { iterationCount = sc.nextInt(); }
                    catch(Exception e)
                    {
                        ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                        textField.setText(oldValue);
                    }
                }
            });
        return output;
    }

    private static HBox userTextbox(String prompt, String baseValue, String description)
    {
        HBox output = new HBox();
        output.setSpacing(5.0);
        Label label = new Label(prompt);
        TextField field = new TextField();
        field.setId(prompt.toLowerCase());
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
        start.setOnAction(
            new EventHandler<ActionEvent>() 
            {
                @Override
                public void handle(ActionEvent event)
                {
                    Cli.runTests();
                }
            });

        Button stop = buttonBuilder("Stop",true);
        stop.setOnAction(
                new EventHandler<ActionEvent>() 
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        //Interupt testing process. (Unsure how to implement rn)
                    }
                });
        Button calibrateCamera = buttonBuilder("Calibrate Cameras",false);
        calibrateCamera.setOnAction(
                new EventHandler<ActionEvent>() 
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        STAGE.setScene(CAMERA_MENU);
                    }
                });
        Button testMovement = buttonBuilder("Test Movement",false);
        testMovement.setOnAction(
                new EventHandler<ActionEvent>() 
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        MovementFacade.testMotions();
                    }
                });

        Button cancel = buttonBuilder("Close",false);
        cancel.setOnAction(
                new EventHandler<ActionEvent>() 
                {
                    @Override
                    public void handle(ActionEvent event)
                    {
                        Cli.close();
                        STAGE.close();
                    }
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
        //HBox serialNumber = userTextbox("DUT Serial Number:","","Enter the serial number for the device under test.");
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
        preview.setOnAction(
            new EventHandler<ActionEvent>() 
            {
                @Override
                public void handle(ActionEvent event)
                {
                    OpenCVFacade.showImage(cameraName);
                }
            });

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
        //How to handle text inputs:
        //textField.textProperty().addListener( 
        //    (observable, oldValue, newValue) -> 
        //    { 
        //        if(!newValue.matches("\\d*")) textField.setText(oldValue);
        //        else 
        //        {
        //            try(Scanner sc = new Scanner(newValue);)
        //            { iterationCount = sc.nextInt(); }
        //            catch(Exception e)
        //            {
        //                ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
        //                textField.setText(oldValue);
        //            }
        //        }
        //    });
        HBox cropX = userTextbox("X",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "X-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        TextField xField = (TextField)cropX.lookup("#x");
        xField.setId("cropX-" + cameraName);
        xField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.CROP_X,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    xField.setText(oldValue);
                }
            });

        HBox cropY = userTextbox("Y",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "Y-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        TextField yField = (TextField)cropY.lookup("#y");
        yField.setId("cropY-" + cameraName);
        yField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.CROP_Y,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    yField.setText(oldValue);
                }
            });
        
        HBox cropW = userTextbox("Width",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "Width, in pixels, of the newly cropped image. Only accepts whole numbers.");
        TextField wField = (TextField)cropW.lookup("#width");
        wField.setId("cropW-" + cameraName);
        wField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.CROP_W,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    wField.setText(oldValue);
                }
            });

        HBox cropH = userTextbox("Height",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "Height, in pixels, of the newly cropped image. Only accepts whole numbers.");
        TextField hField = (TextField)cropH.lookup("#height");
        hField.setId("cropH-" + cameraName);
        hField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.CROP_H,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in Crop Height for " + cameraName +  ".");
                    hField.setText(oldValue);
                }
            });

        output.getChildren().addAll(cropX,
                                    cropY,
                                    cropW,
                                    cropH);
        return output;
    }

    private static HBox miscInputs(String cameraName)
    {
        HBox output = new HBox();

        HBox thresholdValue = userTextbox("Threshold Value",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "This value can be set from 0 to 255. Higher values mean more black in the thresholded image. For more information, see the documentation.");
        TextField thresholdField = (TextField)thresholdValue.lookup("#w");
        thresholdField.setId("threshold-" + cameraName);
        thresholdField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.THRESHOLD_VALUE,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in cycles count.");
                    thresholdField.setText(oldValue);
                }
            });

        HBox cropH = userTextbox("Height",
                Double.toString(ConfigFacade.getValue(cameraName,
                                                      ConfigProperties.CROP_X)),
                                "Height, in pixels, of the newly cropped image. Only accepts whole numbers.");
        TextField hField = (TextField)cropH.lookup("#h");
        hField.setId("cropH-" + cameraName);
        hField.textProperty().addListener( 
            (observable, oldValue, newValue) -> 
            { 
                try(Scanner sc = new Scanner(newValue);)
                { ConfigFacade.setValue(cameraName,ConfigProperties.CROP_H,sc.nextInt()); }
                catch(Exception e)
                {
                    ErrorLogging.logError("USER INPUT ERROR: Illegal input in Crop Height for " + cameraName +  ".");
                    hField.setText(oldValue);
                }
            });

        output.getChildren().addAll(thresholdValue,
                                    compositeFrames);
        return output;
    }

    private static HBox cameraMenuButtons()
    {
        HBox output = new HBox();
        return output;
    }

    private static Button buttonBuilder(String name,boolean disabled)
    {
        Button button = new Button(name);
        button.setId(name.toLowerCase());
        button.setFont(Font.getDefault());
        if (disabled) button.disableProperty();
        return button;
    }

    private static Button buttonBuilder(String name)
    { return buttonBuilder(name,false); }
}
