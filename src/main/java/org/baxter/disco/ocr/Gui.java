package org.baxter.disco.ocr;

import org.bytedeco.opencv.opencv_saliency.StaticSaliency;

import javafx.application.Application;
import javafx.event.*;
import javafx.fxml.*;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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

    public static void main(String[] args)
    { launch(args); }
    
    static
    {
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
        STAGE = stage;
        mainMenuBuilder();
        cameraMenuBuilder();
        STAGE.setScene(MAIN_MENU);
    }

    private static void cameraMenuBuilder()
    {
    }

    private static void mainMenuBuilder()
    {
        VBox layout = new VBox();
        layout.getChildren().addAll(topHalf(),new Separator(Orientation.HORIZONTAL),bottomHalf());
        MAIN_ANCHOR.getChildren().add(layout);
    }


    private static VBox topHalf()
    {
        VBox output = new VBox();
        output.getChildren().addAll(topButtons(),new Separator(Orientation.HORIZONTAL),setupSection(),primeCheckbox(),testFeedback());
        return topHalf();
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

        Label textboxLabel = new Label("Test feedback: ");
        Text textbox = new Text("Awaiting input...");
        textbox.setId("testOutputToUser");

        output.getChildren().addAll(textboxLabel,textbox);
        return output;
    }

    private static HBox setupSection()
    {
        return userTextbox("Cycles",Integer.toString(iterationCount), "Enter the number of times to test the devices in the fixture.");
    }

    private static HBox userTextbox(String prompt, String baseValue, String description)
    {
        HBox output = new HBox();
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
                        Cli.close();
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
                    }
                });


        topButtons.getChildren().addAll(start,stop,calibrateCamera,testMovement,cancel);
        return topButtons;
    }

    private static HBox bottomHalf()
    {
        HBox output = new HBox();
        output.getChildren().addAll(camera(1),new Separator(Orientation.VERTICAL),camera(2));
        return output;
    }

    private static VBox camera(int number)
    {
        VBox output = new VBox();
        HBox serialNumber = userTextbox("DUT Serial Number:","","Enter the serial number for the device under test.");
        output.getChildren().addAll(cameraHeader(number),serialNumber,cameraView(number));
        return output;
    }

    private static HBox cameraHeader(int number)
    {
        HBox output = new HBox();
        output.getChildren().addAll(cameraCheckbox("Camera " + number));
        return output;
    }

    private static HBox cameraCheckbox(String prompt)
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER);
        Label label = new Label(prompt);
        CheckBox checkBox = new CheckBox("Active");
        checkBox.setId(prompt.toLowerCase());
        output.getChildren().addAll(label,checkBox);
        return output;
    }

    private static HBox cameraView(int number)
    {
        HBox output = new HBox();
        output.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("OCR Read:");
        Label ocrRead = new Label("[ ]");
        ocrRead.setId("cameraOCR" + number);
        ImageView imageView = new ImageView();
        output.getChildren().addAll(label,ocrRead,imageView);
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
}
