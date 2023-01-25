package org.baxter.disco.ocr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bytedeco.javacv.CanvasFrame;

/**
 * CLI for the Fixture.
 *
 * Creates a terminal-based user interface for the other 
 * classes in this package (with the exception of {@link Gui} [for now]).
 *
 * @author Blizzard Finnegan
 * @version 0.4.0, 25 Jan. 2023
 */
public class Cli
{
    /**
     * Currently saved iteration count.
     */
    private static int iterationCount = 5;

    /**
     * Scanner used for monitoring user input.
     * This is a global object, so that functions 
     * can access it without requiring to pass the 
     * object around.
     */
    private static Scanner inputScanner;

    /**
     * Number of options currently available in the main menu.
     */
    private static final int mainMenuOptionCount = 6;

    /**
     * Number of options currently available in the movement sub-menu.
     */
    private static final int movementMenuOptionCount = 4;

    /**
     * Number of options currently available in the camera configuration sub-menu.
     */
    private static final int cameraMenuOptionCount = 7;

    public static void main(String[] args)
    {
        try{
            inputScanner = new Scanner(System.in);
            ErrorLogging.logError("Start of program.");

            int userInput = 0;

            do
            {
                printMainMenu();
                userInput = inputFiltering(inputScanner.nextLine());
                switch (userInput)
                {
                    case 1:
                        testMovement();
                        break;
                    case 2:
                        println("Setting up cameras...");
                        println("This may take a moment...");
                        configureCameras();
                        break;
                    case 3:
                        setIterationCount();
                        break;
                    case 4:
                        runTests();
                        println("Test complete!");
                        break;
                    case 5:
                        printHelp();
                        break;
                    case 6:
                        break;
                    default:
                        //Input handling already done by inputFiltering()
                }

        } while (userInput != 6);

        }
        catch(Exception e) { ErrorLogging.logError(e); }
        finally
        {
            inputScanner.close();
            ErrorLogging.closeLogs();
            MovementFacade.closeGPIO();
        }
    }

    /**
     * Wrapper around System.out.println().
     *
     * Because its easier to read, and less to type.
     */
    private static void println(String input) { System.out.println(input); }

    /**
     * Wrapper around System.out.print().
     *
     * Because its easier to read, and less to type.
     */
    private static void prompt(String input) { System.out.print(input); }

    /**
     * Wrapper around System.out.print().
     *
     * Because its easier to read, and less to type.
     */
    private static void print(String input) { System.out.print(input); }

    /**
     * Prints a complete list of descriptions for all 
     * available functions in the menu.
     */
    private static void printHelp()
    {
        println("========================================");
        println("Explanations:");
        println("----------------------------------------");
        println("1. Test fixture movement: Make the " + 
                    "\n\tfixture move in all possible " +
                    "\n\tdirections, to check range"+
                    "\n\tof motion." +
                    "\n\tAvailable variables to change:"+
                    "\n\t\tPWM Duty Cycle"+
                    "\n\t\tPWM Frequency"+
                    "\n\t\tMotor Time-out");
        println("----------------------------------------");
        println("2. Configure camera: Change values " +
                    "\n\tfor the camera, to adjust image" +
                    "\n\tfor use in OCR. Can modify"+
                    "\n\tconfig file, if requested." +
                    "\n\tAvailable variables to change:"+
                    "\n\t\tCrop dimensions"+
                    "\n\t\tGamma");
        println("----------------------------------------");
        println("3. Change test iteration count:"+
                    "\n\tChange the number of times to"+
                    "\n\trun the tests of the device(s)"+
                    "\n\tunder test.");
        println("----------------------------------------");
        println("4. Run tests: Run tests, with defined"+
                    "\n\tnumber of iterations. Uses"+
                    "\n\tvalues defined in config file.");
        println("----------------------------------------");
        println("5. Help: Show this help page.");
        println("----------------------------------------");
        println("6. Exit: Close the program.");
        println("========================================");
        println("Press Enter to continue...");
        inputScanner.nextLine();
    }

    /**
     * Print function for the main menu.
     */
    private static void printMainMenu()
    {
        println("\n\n");
        println("====================================");
        println("Main Menu:");
        println("------------------------------------");
        println("Current iteration count: " + iterationCount);
        println("------------------------------------");
        println("1. Test fixture movement");
        println("2. Configure camera");
        println("3. Change test iteration count");
        println("4. Run tests");
        println("5. Help");
        println("6. Exit");
        println("====================================");
    }

    private static void printMovementMenu()
    {
        println("\n\n");
        println("====================================");
        println("Movement Menu:");
        println("------------------------------------");
        println("Current Duty Cycle: " + MovementFacade.getDutyCycle());
        println("Current Frequency: " + MovementFacade.getFrequency());
        println("Current Motor Time-out: " + MovementFacade.getTimeout());
        println("------------------------------------");
        println("1. Change Duty Cycle");
        println("2. Change Frequency");
        println("3. Change Motor Time-out");
        println("4. Exit");
        println("====================================");
    }

    private static void printCameraMenu(List<String> cameraList)
    {
        println("Available cameras to configure:");
        println("------------------------------------");
        for(int index = 0; index < cameraList.size(); index++)
        {
            int humanIndex = index+1;
            String cameraName = (String)cameraList.get(index);
            println(humanIndex + " - " + cameraName);
        }
        println( (cameraList.size() + 1) + " - Exit to Main Menu");
        println("------------------------------------");
    }

    private static void printCameraConfigMenu(String cameraName)
    {
        println("\n\n");
        println("====================================");
        println("Camera Config Menu:");
        println("------------------------------------");
        println("Current Crop values: ");
        println("************************************");
        print("X: " + ConfigFacade.getValue(cameraName,
                                            ConfigProperties.CROP_X));
        print(" | Y: " + ConfigFacade.getValue(cameraName,
                                               ConfigProperties.CROP_Y));
        print(" | Width: " + ConfigFacade.getValue(cameraName,
                                                   ConfigProperties.CROP_W));
        println(" | Height: " + ConfigFacade.getValue(cameraName,
                                                    ConfigProperties.CROP_H));
        println("************************************");
        println("Current Gamma value: " + ConfigFacade.getValue(cameraName,
                                                                ConfigProperties.GAMMA));
        println("Current composite frame count: " + 
                ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES));
        println("------------------------------------");
        println("1. Change Crop X");
        println("2. Change Crop Y");
        println("3. Change Crop Width");
        println("4. Change Crop Height");
        println("5. Change Gamma Value");
        println("6. Change Composite Frame Count");
        println("7. Exit");
        println("====================================");
    }


    private static void testMovement()
    {
        int userInput = -1;
        do
        {
            println("Testing movement...");
            MovementFacade.testMotions();
            printMovementMenu();
            userInput = inputFiltering(inputScanner.nextLine());
            switch (userInput)
            {
                /*
                 * Menu options:
                 * 1. Change Duty Cycle
                 * 2. Change Frequency
                 * 3. Change Motor Time-out
                 * 4. Exit
                 */
                case 1:
                    prompt("Input the desired duty cycle value: ");
                    int newDutyCycle = inputFiltering(inputScanner.nextLine());
                    if (newDutyCycle != -1)
                    {
                        MovementFacade.setDutyCycle(newDutyCycle);
                        break;
                    }
                case 2:
                    prompt("Input the desired frequency value: ");
                    int newFrequency = inputFiltering(inputScanner.nextLine());
                    if (newFrequency != -1) 
                    {
                        MovementFacade.setFrequency(newFrequency);
                        break;
                    }
                case 3:
                    prompt("Input the desired time-out (in seconds): ");
                    int newTimeout = inputFiltering(inputScanner.nextLine());
                    if (newTimeout != -1) 
                    {
                        MovementFacade.setTimeout(newTimeout);
                        break;
                    }
                case 4:
                    break;
                default:
                    ErrorLogging.logError("User Input Error!!! - Invalid input.");
            }
        } 
        while(userInput != 4);
    }

    /** 
     * Sub-function used to configure cameras.
     */
    private static void configureCameras()
    {
        List<String> cameraList = new ArrayList<>(OpenCVFacade.getCameraNames());
        //println(cameraList.toString());
        
        //Open a single new thread, so the canvas 
        //used further down to display the temporary 
        //image doesn't accidentally kill the program.
        //Created at beginning of function call to reduce 
        //thread spawn count.
        //See also: https://docs.oracle.com/javase/8/docs/api/java/awt/doc-files/AWTThreadIssues.html#Autoshutdown
        Runnable r = new Runnable() {
            public void run() {
                Object o = new Object();
                try {
                    synchronized (o) {
                        o.wait();
                    }
                } catch (InterruptedException ie) {
                }
            }
        };
        Thread t = new Thread(r);
        t.setDaemon(false);
        t.start();

        do
        {
            //Main menu
            printCameraMenu(cameraList);

            //Pick a camera to configure
            int userInput;

            String cameraName = "";
            do
            {
                prompt("Enter a camera number to configure: ");
                userInput = inputFiltering(inputScanner.nextLine());
                userInput--;
            } while (cameraList.size() < userInput);

            //Leave do-while loop if the user asks to
            if(userInput == (cameraList.size())) break;
            else cameraName = cameraList.get((userInput));

            do
            {
                //Show image (need to implement in OpenCVFacade)
                CanvasFrame canvas = OpenCVFacade.showImage(cameraName);


                //User input parsing
                ConfigProperties modifiedProperty = null;
                do
                {
                    //list configurable settings
                    printCameraConfigMenu(cameraName);

                    userInput = inputFiltering(inputScanner.nextLine(),Menus.CAMERA);
                    switch (userInput)
                    {
                        case 1:
                            modifiedProperty = ConfigProperties.CROP_X;
                            break;
                        case 2:
                            modifiedProperty = ConfigProperties.CROP_Y;
                            break;
                        case 3:
                            modifiedProperty = ConfigProperties.CROP_W;
                            break;
                        case 4:
                            modifiedProperty = ConfigProperties.CROP_H;
                            break;
                        case 5:
                            modifiedProperty = ConfigProperties.GAMMA;
                            break;
                        case 6:
                            modifiedProperty = ConfigProperties.COMPOSITE_FRAMES;
                            break;
                        case 7:
                            modifiedProperty = ConfigProperties.PRIME;
                        default:
                    }
                } while(modifiedProperty == null);
                
                if(modifiedProperty != ConfigProperties.PRIME)
                {
                    prompt("Enter new value for this property (" + modifiedProperty.toString() + "): ");
                    userInput = inputFiltering(inputScanner.nextLine());
                    ConfigFacade.setValue(cameraName,modifiedProperty,userInput);
                    if(canvas != null) canvas.dispose();
                }
                else break;
            } while(true);

        } while(true);

        println("Configuration complete!");
    }


    /**
     * Setter for {@link #iterationCount}
     */
    private static void setIterationCount() 
    { 
        int input;
        do 
        {
            prompt("Input the number of test iterations to complete: ");
            input = inputFiltering(inputScanner.nextLine());
        } while(input == -1);
        iterationCount = input;
    }

    /**
     * Starts running tests in {@link OpenCVFacade}
     */
    private static void runTests()
    {
        DataSaving.initWorkbook(ConfigFacade.getOutputSaveLocation());
        Map<String, Double> resultMap = new HashMap<>();
        for(int i = 0; i < iterationCount; i++)
        {
            List<List<File>> iterationList = OpenCVFacade.multipleIterations(iterationCount);
            for(List<File> iteration : iterationList)
            {
                for(File file : iteration)
                {
                    Double result = TesseractFacade.imageToDouble(file);
                    String fileLocation = file.getAbsolutePath();
                    resultMap.put(fileLocation,result);
                }
                DataSaving.writeValues(i,resultMap);
            }
        }
        println("=======================================");
        println("Tests complete!");
    }

    /**
     * Parse the user's input at the main menu, and check it for errors.
     *
     * @param input     The unparsed user input, directly from the {@link Scanner}
     */
    private static int inputFiltering(String input) 
    { return inputFiltering(input, Menus.OTHER); }

    /**
     * Parse the user's input, and check it for errors.
     *
     * @param input     The unparsed user input, directly from the {@link Scanner}
     * @param mainMenu  Whether or not the parsed input is a main menu value
     * 
     * @return The parsed value from the user. Returns -1 upon any error.
     */
    private static int inputFiltering(String input, Menus menu)
    {
        int output = -1;
        input.trim();
        try(Scanner sc = new Scanner(input))
        {
            if(!sc.hasNextInt()) 
            { 
                invalidInput();
                return output; 
            }
            output = sc.nextInt();
                if(output < 0)
                {
                    negativeInput();
                    output = -1;
                }
            switch (menu)
            {
                case MAIN:
                    if(output > mainMenuOptionCount)
                    {
                        invalidMainMenuInput();
                        output = -1;
                    }
                    break;
                case MOVEMENT:
                    if(output > movementMenuOptionCount)
                    {
                        invalidMovementMenuInput();
                        output = -1;
                    }
                    break;
                case CAMERA:
                    if(output > cameraMenuOptionCount)
                    {
                        invalidCameraMenuInput();
                        output = -1;
                    }
                    break;
                case OTHER:
                    break;
            }
        }
        return output;
    }

    /**
     * Prints a message when user inputs an invalid main menu value.
     */
    private static void invalidMainMenuInput()
    {
        invalidMenuInput(mainMenuOptionCount);
    }

    /**
     * Prints a message when user inputs an invalid main menu value.
     */
    private static void invalidMovementMenuInput()
    {
        invalidMenuInput(movementMenuOptionCount);
    }

    /**
     * Prints a message when user inputs an invalid main menu value.
     */
    private static void invalidCameraMenuInput()
    {
        invalidMenuInput(cameraMenuOptionCount);
    }

    /**
     * Prints a message when user inputs an invalid menu value.
     */
    private static void invalidMenuInput(int menuLength)
    {
        invalidInput("Please input a number from 1 to " + menuLength + ".");
    }

    /**
     * Prints a response for when the user's input is negative.
     */
    private static void negativeInput()
    {
        invalidInput("Please input a positive number.");
    }

    /**
     * Prints a generic response when the user's input is invalid.
     */
    private static void invalidInput()
    {
        invalidInput("");
    }
    
    /**
     * Prints a defined response; used when user input is invalid.
     *
     * @param input    Custom message to print in the error block.
     */
    private static void invalidInput(String input)
    {
        ErrorLogging.logError("Invalid User Input!!! - Message to user: '" + input + "'");
        println("");
        println("=================================================");
        println("Invalid input! - " + input);
        println("=================================================");
        println("");
    }

    private enum Menus { MAIN,MOVEMENT,CAMERA,OTHER; }
}
