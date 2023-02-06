package org.baxter.disco.ocr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacv.CanvasFrame;

/**
 * CLI for the Fixture.
 *
 * Creates a terminal-based user interface for the other 
 * classes in this package (with the exception of Gui-related 
 * classes).
 *
 * @author Blizzard Finnegan
 * @version 1.3.0, 03 Feb. 2023
 */
public class Cli
{
    /**
     * Currently saved iteration count.
     */
    private static int iterationCount = 10;

    /**
     * Scanner used for monitoring user input.
     * This is a global object, so that functions 
     * can access it without requiring to pass the 
     * object around.
     */
    private static Scanner inputScanner;

    /**
     * Whether the user has set the serial numbers yet.
     */
    private static boolean serialsSet = false;

    /**
     * Whether the user has successfully configured the cameras.
     */
    private static boolean camerasConfigured = false;

    /**
     * Number of options currently available in the main menu.
     */
    private static final int mainMenuOptionCount = 7;

    /**
     * Number of options currently available in the movement sub-menu.
     */
    private static final int movementMenuOptionCount = 4;

    /**
     * Number of options currently available in the camera configuration sub-menu.
     */
    private static final int cameraMenuOptionCount = 9;

    /**
     * Lock object, used for temporary interruption of {@link #runTests()}
     */
    private static Lock LOCK = new ReentrantLock();

    /**
     * Instance of {@link MovementFacade} for controlling the fixture.
     */
    private static MovementFacade fixture;

    static
    {
        ErrorLogging.logError("DEBUG: START OF PROGRAM");
    }

    public static void main(String[] args)
    {
        try{
            inputScanner = new Scanner(System.in);

            //ErrorLogging.logError("DEBUG: Setting up multithreading...");
            fixture = new MovementFacade(LOCK);
            //ErrorLogging.logError("DEBUG: Multithreading complete!");
            
            //ErrorLogging.logError("DEBUG: Importing config...");
            ConfigFacade.init();
            //ErrorLogging.logError("DEBUG: Config imported!");

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
                        camerasConfigured = true;
                        break;
                    case 3:
                        setDUTSerials();
                        serialsSet = true;
                        break;
                    case 4:
                        setIterationCount();
                        break;
                    case 5:
                        if(!camerasConfigured)
                        {
                            prompt("You have not configured the cameras yet! Are you sure you would like to continue? (y/N): ");
                            String input = inputScanner.nextLine().toLowerCase();
                            if( input.isBlank())
                            {
                                break;
                            }
                            else if (input.charAt(0) != 'y' ) 
                            {
                                break;
                            }
                            else 
                            {
                                ErrorLogging.logError("WARNING! - Potential for error: Un-initialised cameras.");
                            }
                        }
                        if(!serialsSet)
                        {
                            prompt("You have not set the serial numbers for your DUTs yet! Are you sure you would like to continue? (y/N): ");
                            String input = inputScanner.nextLine().toLowerCase();
                            if( input.isBlank())
                            {
                                break;
                            }
                            else if (input.charAt(0) != 'y' ) 
                            {
                                break;
                            }
                            else
                            {
                                ErrorLogging.logError("WARNING! - Potential for error: Un-initialised DUT Serial numbers.");
                            }
                        }
                        runTests();
                        break;
                    case 6:
                        printHelp();
                        break;
                    case 7:
                        break;
                    default:
                        //Input handling already done by inputFiltering()
                }

        } while (userInput != mainMenuOptionCount);

        }
        catch(Exception e) 
        { 
            ErrorLogging.logError(e); 
            ErrorLogging.logError("ERROR CAUGHT - CLOSING PROGRAM.");
        }
        finally
        {
            close();
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
                    "\n\t\tCrop dimensions");
        println("----------------------------------------");
        println("3. Set serial numbers: Set the serial " +
                "\n\tnumber for the device under test." +
                "\n\tThis is used in final data saving.");
        println("----------------------------------------");
        println("4. Change test iteration count:"+
                    "\n\tChange the number of times to"+
                    "\n\trun the tests of the device(s)"+
                    "\n\tunder test.");
        println("----------------------------------------");
        println("5. Run tests: Run tests, with defined"+
                    "\n\tnumber of iterations. Uses"+
                    "\n\tvalues defined in config file.");
        println("----------------------------------------");
        println("6. Help: Show this help page.");
        println("----------------------------------------");
        println("7. Exit: Close the program.");
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
        println("======================================");
        println("Main Menu:");
        println("--------------------------------------");
        println("Current iteration count: " + iterationCount);
        println("--------------------------------------");
        println("1. Test and configure fixture movement");
        println("2. Configure camera");
        println("3. Set serial numbers");
        println("4. Change test iteration count");
        println("5. Run tests");
        println("6. Help");
        println("7. Exit");
        println("======================================");
    }

    /**
     * Predefined print statements for the movement submenu.
     */
    private static void printMovementMenu()
    {
        println("\n\n");
        println("====================================");
        println("Movement Menu:");
        println("------------------------------------");
        println("Current Duty Cycle: " + fixture.getDutyCycle());
        println("Current Frequency: " + fixture.getFrequency());
        println("Current Motor Time-out: " + fixture.getTimeout());
        println("------------------------------------");
        println("1. Change Duty Cycle");
        println("2. Change Frequency");
        println("3. Change Motor Time-out");
        println("4. Exit");
        println("====================================");
    }

    /**
     * Pre-defined method for printing all available cameras in a menu
     */
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

    /**
     * Pre-defined method for printing all available cameras and the associated serials in a menu
     */
    private static void printSerialMenu(List<String> cameraList)
    {
        println("Available serial numbers to set:");
        println("------------------------------------");
        for(int index = 0; index < cameraList.size(); index++)
        {
            int humanIndex = index+1;
            String cameraName = (String)cameraList.get(index);
            print(humanIndex + " - " + cameraName + " : ");
            if(ConfigFacade.getSerial(cameraName) != null) 
                println(ConfigFacade.getSerial(cameraName));
            else
                println("");
        }
        println( (cameraList.size() + 1) + " - Exit to Main Menu");
        println("------------------------------------");
    }

    /**
     * Pre-defined menu for printing camera configuration options
     */
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
        println("Current composite frame count: " + 
                ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES));
        String cropValue = ((ConfigFacade.getValue(cameraName,ConfigProperties.CROP) != 0) ? "yes" : "no");
        println("Will the image be cropped? " + cropValue);
        String thresholdImage = ((ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD) != 0) ? "yes" : "no");
        println("Will the image be thresholded? " + thresholdImage);
        println("------------------------------------");
        println("1. Change Crop X");
        println("2. Change Crop Y");
        println("3. Change Crop Width");
        println("4. Change Crop Height");
        println("5. Change Composite Frame Count");
        println("6. Change Threshold Value");
        println("7. Toggle crop");
        println("8. Toggle threshold");
        println("9. Exit");
        println("====================================");
    }


    /**
     * Function for testing movement, and modifying hardware values
     */
    private static void testMovement()
    {
        int userInput = -1;
        do
        {
            println("Testing movement...");
            fixture.testMotions();
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
                        fixture.setDutyCycle(newDutyCycle);
                        break;
                    }
                case 2:
                    prompt("Input the desired frequency value: ");
                    int newFrequency = inputFiltering(inputScanner.nextLine());
                    if (newFrequency != -1) 
                    {
                        fixture.setFrequency(newFrequency);
                        break;
                    }
                case 3:
                    prompt("Input the desired time-out (in seconds): ");
                    int newTimeout = inputFiltering(inputScanner.nextLine());
                    if (newTimeout != -1) 
                    {
                        fixture.setTimeout(newTimeout);
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
        
        // The below code should be unnecessary now. Leaving in for now to ensure things work properly.
        ////Open a single new thread, so the canvas 
        ////used further down to display the temporary 
        ////image doesn't accidentally kill the program.
        ////Created at beginning of function call to reduce 
        ////thread spawn count.
        ////See also: https://docs.oracle.com/javase/8/docs/api/java/awt/doc-files/AWTThreadIssues.html#Autoshutdown
        //Runnable r = new Runnable() {
        //    public void run() {
        //        Object o = new Object();
        //        try {
        //            synchronized (o) {
        //                o.wait();
        //            }
        //        } catch (InterruptedException ie) {
        //        }
        //    }
        //};
        //Thread t = new Thread(r);
        //t.setDaemon(false);
        //t.start();

        fixture.iterationMovement(true);

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
            else if(userInput < 0)
            {}
            else cameraName = cameraList.get((userInput));

            do
            {
                fixture.pressButton();
                try{ Thread.sleep(2000); } catch(Exception e){ ErrorLogging.logError(e); }
                //Show image 
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
                            modifiedProperty = ConfigProperties.COMPOSITE_FRAMES;
                            break;
                        case 6:
                            modifiedProperty = ConfigProperties.THRESHOLD_VALUE;
                            break;
                        case 7:
                            modifiedProperty = ConfigProperties.CROP;
                            break;
                        case 8:
                            modifiedProperty = ConfigProperties.THRESHOLD;
                            break;
                        case 9:
                            modifiedProperty = ConfigProperties.PRIME;
                            break;
                        default:
                    }
                } while(modifiedProperty == null);
                
                if(modifiedProperty == ConfigProperties.THRESHOLD || modifiedProperty == ConfigProperties.CROP)
                {
                    double newValue = ConfigFacade.getValue(cameraName,modifiedProperty);
                    newValue = Math.abs(newValue - 1);
                    ConfigFacade.setValue(cameraName,modifiedProperty,newValue);
                }
                else if(modifiedProperty != ConfigProperties.PRIME)
                {
                    prompt("Enter new value for this property (" + modifiedProperty.toString() + "): ");
                    userInput = inputFiltering(inputScanner.nextLine());
                    ConfigFacade.setValue(cameraName,modifiedProperty,userInput);
                    if(canvas != null) canvas.dispose();
                }
                else break;
            } while(true);

        } while(true);

        ConfigFacade.saveCurrentConfig();
        println("Configuration complete!");
    }

    /**
     * Sub-function used for defining the serial numbers of the devices under test
     */
    private static void setDUTSerials()
    {
        List<String> cameraList = new ArrayList<>(OpenCVFacade.getCameraNames());
        do
        {
            //Main menu
            printSerialMenu(cameraList);

            //Pick a camera to configure
            int userInput;

            String cameraName = "";
            do
            {
                prompt("Enter the camera you wish to set the serial of: ");
                userInput = inputFiltering(inputScanner.nextLine());
                userInput--;
            } while (cameraList.size() < userInput || userInput < 0);

            //Leave do-while loop if the user asks to
            if(userInput == (cameraList.size())) break;
            else cameraName = cameraList.get((userInput));

            prompt("Enter the serial number you wish to use for this camera: ");
            ConfigFacade.setSerial(cameraName,inputScanner.nextLine());

        } while(true);
    }


    /**
     * CLI-level setter for {@link #iterationCount}
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
     * Starts running tests
     */
    private static void runTests()
    {
        final int localIterations = iterationCount;
        //testingThread = new Thread(() ->
        //{
            DataSaving.initWorkbook(ConfigFacade.getOutputSaveLocation(),OpenCVFacade.getCameraNames().size());
            boolean prime = false;
            List<String> cameraList = new ArrayList<>();
            for(String cameraName : OpenCVFacade.getCameraNames())
            {
                //if(cameraName != null) { /*println(cameraName);*/ }
                //else ErrorLogging.logError("Null camera!");
                if(ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) != 0)
                {
                    prime = true;
                }
                cameraList.add(cameraName);
            }
            ErrorLogging.logError("DEBUG: Waking devices.");
            fixture.iterationMovement(prime);
            fixture.pressButton();
            fixture.iterationMovement(prime);
            ErrorLogging.logError("DEBUG: Starting tests...");
            Map<File,Double> resultMap = new HashMap<>();
            Map<String,File> cameraToFile = new HashMap<>();
            for(String cameraName : cameraList)
            {
                cameraToFile.put(cameraName,new File("/dev/null"));
            }
            for(int i = 0; i < localIterations; i++)
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
                for(String cameraName : cameraList)
                {
                    while(!LOCK.tryLock()) {}
                    File file = cameraToFile.get(cameraName);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    //ErrorLogging.logError("DEBUG: File passed to Tesseract: " + file.getAbsolutePath());
                    Double result = TesseractFacade.imageToDouble(file);
                    LOCK.unlock();
                    while(!LOCK.tryLock()) {}
                    resultMap.put(file,result);
                    ErrorLogging.logError("DEBUG: Tesseract final output: " + result);
                    LOCK.unlock();
                }
                while(!LOCK.tryLock()) {}
                DataSaving.writeValues(i,resultMap,cameraToFile);
                LOCK.unlock();
                resultMap.clear();
            }
            println("=======================================");
            println("Testing complete!");
        //});
        //testingThread.start();
    }


    /**
     * Function used if a config file was successfully imported.
     */
    public static void configImported()
    { camerasConfigured = true; }

    /**
     * Function that closes GPIO and logging.
     */
    private static void close()
    {
        ErrorLogging.logError("DEBUG: PROGRAM CLOSING.");
        if(inputScanner != null) inputScanner.close();
        fixture.closeGPIO();
        ErrorLogging.logError("DEBUG: END OF PROGRAM.");
        ErrorLogging.closeLogs();
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
     * @param menu      Which menu is being parsed
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
        invalidInput("Please input a valid number.");
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

    /**
     * Enum of possible menus available
     */
    private enum Menus { MAIN,MOVEMENT,CAMERA,OTHER; }
}
