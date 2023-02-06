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
 * GuiView, {@link GuiController}, and {@link GuiModel} versions are tied together, and are referred to collectively as Gui.
 *
 * @author Blizzard Finnegan
 * @version 0.2.0, 06 Feb, 2023
 */
public class GuiView extends Application
{
    /**
     * Scene used for the Main Menu.
     */
    public static final Scene MAIN_MENU; 

    /**
     * The base Node object for the Main menu; used to define window borders.
     */
    private static final AnchorPane MAIN_ANCHOR;

    /**
     * The Node object within the {@link #MAIN_ANCHOR}, where all portions of the main menu are stored.
     */
    private static final Pane MAIN_PANE;


    /**
     * Scene used for the camera configuration menu
     */
    public static final Scene CAMERA_MENU; 

    /**
     * The base Node object for the camera config menu; used to define window borders.
     */
    private static final AnchorPane CAMERA_ANCHOR;

    /**
     * The node object within the {@link #CAMERA_ANCHOR}, where all portions of the camera config menu are stored.
     */
    private static final Pane CAMERA_PANE;

    /**
     * An easily-accessible map of text fields used in the Camera config menu.
     * The outer map's keys are the respective cameras, with the inner map being config properties and the associated text field.
     */
    private static final Map<String,Map<ConfigProperties,TextField>> uiFields = new HashMap<>();

    /**
     * An easily-accessible location for the user-feedback Text object.
     */
    private static Text userFeedback;

    /**
     * An easily accessible location for the TextField the user uses to set the current amount of iterations.
     */
    private static TextField iterationField;

    /**
     * Easily-accessible Button object for the start button.
     */
    private static Button startButton;

    /*
     * Easily-accessible Button object for the stop button.
     */
    //private static Button stopButton;

    /**
     * The main Stage object, used in the GUI.
     *
     * The Stage object is analogous to the window generated.
     */
    private static Stage STAGE;

    /**
     * Value used for spacing within VBoxes and HBoxes
     */
    private static double INTERNAL_SPACING = 5.0;

    /**
     * Value used for window border spacing
     */
    private static double EXTERNAL_SPACING = 10.0;

    /**
     * The wrapper function to spawn a new JavaFX Stage.
     */
    public static void main(String[] args) { launch(args); }
    
    /**
     * Initialiser for the static objects.
     */
    static
    {
        ErrorLogging.logError("START OF PROGRAM");
        ErrorLogging.logError("Setting up main menu...");
        MAIN_ANCHOR = new AnchorPane();
        MAIN_ANCHOR.setMinWidth(Double.NEGATIVE_INFINITY);
        MAIN_ANCHOR.setMinHeight(Double.NEGATIVE_INFINITY);
        MAIN_PANE = new Pane();

        //Set the window border
        AnchorPane.setTopAnchor(MAIN_PANE,EXTERNAL_SPACING);
        AnchorPane.setLeftAnchor(MAIN_PANE,EXTERNAL_SPACING);
        AnchorPane.setRightAnchor(MAIN_PANE,EXTERNAL_SPACING);
        AnchorPane.setBottomAnchor(MAIN_PANE,EXTERNAL_SPACING);
        MAIN_ANCHOR.getChildren().add(MAIN_PANE);
        MAIN_MENU = new Scene(MAIN_ANCHOR);

        ErrorLogging.logError("Setting up camera config menu...");
        CAMERA_ANCHOR = new AnchorPane();
        CAMERA_ANCHOR.setMinWidth(Double.NEGATIVE_INFINITY);
        CAMERA_ANCHOR.setMinHeight(Double.NEGATIVE_INFINITY);
        CAMERA_PANE = new Pane();

        //Set the window border
        AnchorPane.setTopAnchor(CAMERA_PANE,EXTERNAL_SPACING);
        AnchorPane.setLeftAnchor(CAMERA_PANE,EXTERNAL_SPACING);
        AnchorPane.setRightAnchor(CAMERA_PANE,EXTERNAL_SPACING);
        AnchorPane.setBottomAnchor(CAMERA_PANE,EXTERNAL_SPACING);
        CAMERA_ANCHOR.getChildren().add(CAMERA_PANE);
        CAMERA_MENU = new Scene(CAMERA_ANCHOR);

        //Initialise the camera fields map
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

    /**
     * Camera Configuration Menu builder function. 
     *
     * Creates a {@link VBox}, creates a {@link #cameraSetup(String)} object, and adds a separator between each camera.
     * Finally, sets the created VBox to be the child of the {@link #CAMERA_PANE}, so it can be shown.
     */
    private static void cameraMenuBuilder()
    {
        VBox layout = new VBox();
        layout.setSpacing(INTERNAL_SPACING);
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

    /**
     * Main Menu builder function.
     *
     * Creates a VBox, fills it with the {@link #topHalf()}, a {@link Separator}, and the {@link #bottomHalf()}
     * Finally, sets the created VBox to be the child of the {@link #MAIN_PANE}, so it can be shown.
     */
    private static void mainMenuBuilder()
    {
        VBox layout = new VBox();
        layout.getChildren().addAll(topHalf(),
                                    new Separator(Orientation.HORIZONTAL),
                                    bottomHalf());
        MAIN_PANE.getChildren().add(layout);
    }


    /**
     * Builder for the top half of the main menu.
     *
     * Creates a VBox, fills it with the {@link #topButtons()}, a {@link Separator}, the {@link #setupSection()},
     * the {@link #primeCheckbox()}, and the {@link #testFeedback()}.
     *
     * @return VBox described above
     */
    private static VBox topHalf()
    {
        VBox output = new VBox();
        output.setSpacing(INTERNAL_SPACING);
        output.getChildren().addAll(topButtons(),
                                    new Separator(Orientation.HORIZONTAL),
                                    setupSection(),
                                    primeCheckbox(),
                                    testFeedback());
        return output;
    }

    /**
     * Builder for the priming section of the main menu.
     *
     * Builds a pre-defined checkbox for setting whether the DUTs should be primed.
     *
     * @return CheckBox with a preset Tooltip, Id, and Listener.
     */
    private static CheckBox primeCheckbox()
    {
        CheckBox output = new CheckBox("Prime devices");
        output.setTooltip(new Tooltip("This presses the button on the device under test twice for every iteration."));
        output.setSelected(true);
        output.setId("primeCheckbox");
        output.selectedProperty().addListener(
                (obeservableValue, oldValue, newValue) ->
                {
                    GuiController.updatePrime();
                });
        return output;
    }

    /**
     * Builder for the user feedback section of the main menu.
     *
     * Creates an HBox, fills it with a {@link Label} and a {@link Text} used for communicating 
     * program status. 
     *
     * @return HBox defined above
     */
    private static HBox testFeedback()
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
        Label textboxLabel = new Label("Test feedback: ");
        Text textbox = new Text("Awaiting input...");
        userFeedback = textbox;
        textbox.setId("testOutputToUser");

        output.getChildren().addAll(textboxLabel,textbox);
        return output;
    }

    /**
     * Builder function for the iteration count user input.
     *
     * Creates an HBox, filled with a Label and a TextField for user input. 
     * This TextField is used for setting the number of iterations to complete.
     *
     * @return HBox defined above
     */
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

    /**
     * Builder function for the top buttons of the Main Menu.
     *
     * Creates an HBox for the top buttons, then fills it with a 
     * - start Button
     *   - Runs the tests. As of Gui 0.2.0, this only partially runs the first portion of the test.
     * - stop Button
     *   - Intended to stop the test. As of Gui 0.2.0, this has not yet been properly implemented.
     * - calibrate cameras Button
     *   - Changes Scene to {@link #CAMERA_MENU}, allowing for camera setup.
     * - test movement Button
     *   - Tests the movement of the fixture, informs the user of the test's success/failure
     * - close Button
     *   - Closes the window, and the program. Note that as of Gui 0.2.0, this errors out the JVM
     *
     * @return HBox containing the above-listed buttons.
     */
    private static HBox topButtons()
    {
        //Initial HBox creation
        HBox topButtons = new HBox();
        topButtons.setSpacing(INTERNAL_SPACING);
        topButtons.setAlignment(Pos.CENTER);
        topButtons.setMinWidth(Region.USE_COMPUTED_SIZE);
        topButtons.setMinHeight(Region.USE_COMPUTED_SIZE);

        //Start button creation
        final Button START = buttonBuilder("Start",true);
        startButton = START;

        //Stop button created early, as it is affected by Start, and must be passed in
        final Button STOP = buttonBuilder("Stop",true);

        //Start button action and tooltip setting.
        START.setOnAction( (event) -> 
            {
                START.setDisable(true);
                STOP.setDisable(false);
                GuiController.runTests();
            });
        START.setTooltip(new Tooltip("Configure cameras to start the program."));

        //Stop button action and tooltip setting.
        STOP.setOnAction( (event) -> 
            {
                GuiController.interruptTests();
                START.setDisable(false);
                STOP.setDisable(true);
            });
        STOP.setTooltip(new Tooltip("Pauses current iteration."));

        //Calibrate Cameras button creation
        Button calibrateCamera = buttonBuilder("Calibrate Cameras",false);
        calibrateCamera.setOnAction( 
            (event) -> 
            {
                GuiController.calibrateCameras();
                STAGE.setScene(CAMERA_MENU);
            });
        

        //Test Movement button creation
        Button testMovement = buttonBuilder("Test Movement",false);
        testMovement.setOnAction( (event) -> GuiController.testMotions() );

        //Close button creation
        Button cancel = buttonBuilder("Close",false);
        cancel.setOnAction( (event) -> 
            {
                GuiController.closeModel();
                STAGE.close();
            });


        //Put the above buttons into the HBox
        topButtons.getChildren().addAll(START,
                                        STOP,
                                        calibrateCamera,
                                        testMovement,
                                        cancel);
        return topButtons;
    }

    /**
     * Builder function for the bottom half of the main menu.
     *
     * Creates an HBox, with however many cameras exist, and their associated {@link #camera(String)} views.
     *
     * @return Hbox described above
     */
    private static HBox bottomHalf()
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        output.setSpacing(INTERNAL_SPACING);

        int index = 0;
        for(String camera : GuiModel.getCameras())
        {
            if(index != 0) output.getChildren().add(new Separator(Orientation.VERTICAL));
            output.getChildren().add(camera(camera));
        }
        return output;
    }

    /**
     * Builder for a camera view for the main menu.
     *
     * Creates a VBox, containing:
     * - {@link #cameraHeader(String)}
     * - HBox with a Label and TextField for the user to set a DUT's serial number.
     * - {@link #cameraView(String)}
     *
     * @param cameraName The name of the camera to be attached to.
     *
     * @return VBox described above
     */
    private static VBox camera(String cameraName)
    {
        VBox output = new VBox();
        output.setAlignment(Pos.CENTER_LEFT);
        output.setSpacing(INTERNAL_SPACING);

        HBox serialNumber = userTextField("DUT Serial Number:","","Enter the serial number for the device under test.");

        TextField field = null;
        for(Node child : serialNumber.getChildren())
        {
            if(child instanceof TextField)
            {
                field = (TextField)child;
                break;
            }
        }

        field.setId("serial" + cameraName);
        field.textProperty().addListener( 
            (observable, oldValue, newValue) ->  GuiController.setSerial(cameraName, newValue));

        output.getChildren().addAll(cameraHeader(cameraName),
                                    serialNumber,
                                    cameraView(cameraName));
        return output;
    }

    /**
     * Builder for the camera header, for the main menu.
     *
     * Creates an HBox, containing a Label with the camera's name, and a checkbox to mark whether it is active.
     * As of Gui 0.2.0, the checkbox does not work properly.
     *
     * @param cameraName The name of the camera being accessed.
     *
     * @return HBox described above.
     */
    private static HBox cameraHeader(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
        output.setAlignment(Pos.CENTER);
        Label label = new Label("Camera: " + cameraName);
        CheckBox checkBox = new CheckBox("Active");
        checkBox.setSelected(true);
        checkBox.setOnAction( (event) -> {/*implement*/});
        checkBox.setId(cameraName.toLowerCase());
        output.getChildren().addAll(label,
                                    checkBox);
        return output;
    }

    /**
     * Builder for the camera view, used in the main menu.
     *
     * Creates an HBox, containing: 
     * - A Label for defining what the following label means (OCR Read:)
     * - A Label for showing what the OCR reading is.
     *   - As of Gui 0.2.0, this has not been implemented
     * - An ImageView object for showing the final image
     *   - As of Gui 0.2.0, this has not been implemented
     * @param cameraName Name of the camera being accessed
     *
     * @return HBox described above.
     */
    private static HBox cameraView(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
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

    /**
     * Builder function for a single section in the camera config section.
     *
     * Creates a VBox, containing:
     * - A Label (used for a section header)
     * - A series of CheckBoxes used to define whether to crop and/or threshold the image 
     * - An HBox of inputs, used to define cropping values. (Defined by {@link #cropInputs(String)})
     * - An HBox of inputs, used to define the threshold value, and how many images to compose together (Defined by {@link #miscInputs(String)})
     * 
     * @param cameraName The name of the camera being modified
     *
     * @return The VBox described above
     */
    private static VBox cameraSetup(String cameraName)
    {
        VBox output = new VBox();
        output.setSpacing(INTERNAL_SPACING);
        output.setAlignment(Pos.CENTER_LEFT);

        Label sectionHeader = new Label("Camera: " + cameraName);
        output.getChildren().addAll(sectionHeader,
                                    processingInputs(cameraName),
                                    cropInputs(cameraName),
                                    miscInputs(cameraName));
        return output;
    }

    /**
     * Builder for the processing section of the {@link #cameraSetup(String)}, used in the Camera Config section.
     *
     * Creates an HBox containing:
     * - A Button for creating a temporary preview
     * - A CheckBox to toggle the cropping of the image
     * - A CheckBox to toggle the thresholding of the image 
     *
     * @param cameraName The name of the camera being modified
     *
     * @return HBox, as described above
     */
    private static HBox processingInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
        output.setAlignment(Pos.CENTER_LEFT);

        //Preview button generation
        Button preview = buttonBuilder("Preview");
        preview.setId("previewButton-" + cameraName);
        preview.setOnAction( (event) -> 
            {
                GuiController.pressButton();
                try{ Thread.sleep(2000); } catch(Exception e){ ErrorLogging.logError(e); }
                GuiController.showImage(cameraName);
            });

        //Crop image toggle checkbox creation
        CheckBox cropPreview = new CheckBox("Crop preview");
        cropPreview.setSelected(true);
        cropPreview.setId("cropToggle-" + cameraName);
        cropPreview.selectedProperty().addListener((obeservableValue, oldValue, newValue) -> 
            GuiController.toggleCrop(cameraName));
        cropPreview.setOnAction( (event) -> GuiController.toggleCrop(cameraName) );

        //Threshold image toggle switch creation
        CheckBox thresholdPreview = new CheckBox("Threshold preview");
        thresholdPreview.setSelected(true);
        thresholdPreview.selectedProperty().addListener((obeservableValue, oldValue, newValue) ->
                    GuiController.toggleThreshold(cameraName));
        thresholdPreview.setId("thresholdToggle-" + cameraName);
        thresholdPreview.setOnAction( (event) -> GuiController.toggleThreshold(cameraName) );


        output.getChildren().addAll(preview,
                                    cropPreview,
                                    thresholdPreview);
        return output;
    }

    /**
     * Builder function for the crop values, stored within a {@link #cameraSetup(String)} in the Camera Config menu.
     *
     * Creates an HBox, containing:
     * - A Label and TextField for each of the following:
     *   - Crop X
     *   - Crop Y
     *   - Crop Width
     *   - Crop Height
     *
     * @param cameraName The name of the camera being modified
     *
     * @return HBox, as defined above
     */
    private static HBox cropInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
        output.setAlignment(Pos.CENTER_LEFT);

        HBox cropX = userTextField("X:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_X),
                                   "X-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropX,ConfigProperties.CROP_X,cameraName);

        HBox cropY = userTextField("Y:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_Y),
                                   "Y-value of the top left corner of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropY,ConfigProperties.CROP_Y,cameraName);
        
        HBox cropW = userTextField("Width:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_W),
                                   "Width, in pixels, of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropW,ConfigProperties.CROP_W,cameraName);

        HBox cropH = userTextField("Height:",
                                    GuiController.getConfigValue(cameraName,ConfigProperties.CROP_H),
                                   "Height, in pixels, of the newly cropped image. Only accepts whole numbers.");
        textFieldSetup(cropH, ConfigProperties.CROP_H, cameraName);

        output.getChildren().addAll(cropX,
                                    cropY,
                                    cropW,
                                    cropH);
        return output;
    }

    /**
     * Builder function for the other modifiable values for the {@link #cameraSetup(String)} portion of the camera config menu.
     *
     * Creates an HBox, containing a Label and TextField for:
     * - threshold value 
     * - number of composite frames
     *
     * @param cameraName The name of the camera being configured
     *
     * @return HBox, defined above
     */
    private static HBox miscInputs(String cameraName)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
        output.setAlignment(Pos.CENTER_LEFT);

        HBox thresholdValue = userTextField("Threshold Value:",
                                            GuiController.getConfigValue(cameraName,ConfigProperties.THRESHOLD),
                                            "This value can be set from 0 to 255. Higher values mean more black in "+
                                            "the thresholded image. For more information, see the documentation.");
        textFieldSetup(thresholdValue,ConfigProperties.THRESHOLD_VALUE,cameraName);

        HBox compositeFrames = userTextField("Composite Frames:",
                                            GuiController.getConfigValue(cameraName,ConfigProperties.COMPOSITE_FRAMES),
                                             "Number of frames to bitwise-and together.");
        textFieldSetup(compositeFrames,ConfigProperties.COMPOSITE_FRAMES,cameraName);

        output.getChildren().addAll(thresholdValue,
                                    compositeFrames);
        return output;
    }

    /**
     * Builder function for the final buttons in the Camera Config menu.
     *
     * Creates an HBox, containing:
     * - Save Defaults button
     * - Save Current button
     * - Save and Close button 
     * - Close without Saving button
     *
     * @return HBox, as described above
     */
    private static HBox cameraMenuButtons()
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        output.setSpacing(10.0);

        Button defaults = buttonBuilder("Save Defaults");
        defaults.setOnAction( (event) ->
            {
                GuiController.saveDefaults();
                GuiController.updateStart();
            });

        Button save = buttonBuilder("Save");
        save.setOnAction( (event) ->
            {
                GuiController.save();
                GuiController.updateStart();
            });

        Button saveClose = buttonBuilder("Save and Close");
        saveClose.setOnAction( (event) -> 
            {
                GuiController.saveClose();
                GuiController.updateStart();
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

    /**
     * Modifying function for a text field. 
     *
     * Brings in an HBox, stores it in the Map with the correct {@link ConfigProperties} value.
     * Also sets the action of the text box to be correct.
     *
     * @param hbox          The HBox containing the TextField to be modified and remembered
     * @param property      The property to be associated with the TextField
     * @param cameraName    The name of the camera to be associated with the TextField
     */
    private static void textFieldSetup(HBox hbox, ConfigProperties property, String cameraName)
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

    /**
     * Builder function for a button.
     *
     * Creates a button with a set ID, name, and disabled/enables status.
     *
     * @param name      The name of the new button
     * @param disabled  Whether or not the button should be disabled on startup
     *
     * @return Button , with a preset ID, name, and optionally disabled.
     */
    private static Button buttonBuilder(String name,boolean disabled)
    {
        String[] id = name.strip().substring(0, name.length() - 1).toLowerCase().strip().split(" ");
        Button button = new Button(name);
        button.setId(id[0]);
        button.setDisable(disabled);
        return button;
    }

    /**
     * Builder function for an enabled button.
     *
     * Creates a button with a set ID and name.
     *
     * @param name      The name of the new button
     * @return Button , with a preset ID and name.
     */
    private static Button buttonBuilder(String name)
    { return buttonBuilder(name,false); }

    /**
     * Builder function for a user-interactable TextField, with built-in label.
     *
     * Creates an HBox, with a Label for the TextField, along with the TextField itself.
     *
     * @param prompt        The name used for the Label
     * @param baseValue     The default value used in the TextField
     * @param description   The Tooltip of the TextField/Label
     *
     * @return Hbox, described above
     */
    private static HBox userTextField(String prompt, String baseValue, String description)
    {
        HBox output = new HBox();
        output.setSpacing(INTERNAL_SPACING);
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

    /**
     * Getter for a given TextField, associated with a camera and property.
     *
     * @param cameraName    The name of the camera the TextField is associated with
     * @param property      The name of the property the TextField is associated with
     *
     * @return TextField
     */
    public static TextField getField(String cameraName, ConfigProperties property)
    { return uiFields.get(cameraName).get(property); }

    /**
     * Getter for the Start button.
     *
     * @return Button used for starting the tests.
     */
    public static Button getStart()
    { return startButton; }

    /**
     * Getter for the user feedback Text object.
     *
     * @return Text object used for communicating statuses to the user.
     */
    public static Text getFeedbackText()
    { return userFeedback; }

    /**
     * Getter for the TextField used by the user to set the number of iterations.
     *
     * @return TextField 
     */
    public static TextField getIterationField()
    { return iterationField; }
}
