package org.baxter.disco.ocr;

//Standard imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CLI for the Fixture.
 *
 * Creates a terminal-based user interface for the other 
 * classes in this package (with the exception of Gui-related 
 * classes).
 *
 * @author Blizzard Finnegan
 * @version 1.5.0, 10 Feb. 2023
 */
public class Cli
{
    /**
     * Complete build version number
     */
    private static final String version = "4.3.0";

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
    private static final int mainMenuOptionCount = 8;

    /**
     * Number of options currently available in the movement sub-menu.
     */
    private static final int movementMenuOptionCount = 4;

    /**
     * Number of options currently available in the camera configuration sub-menu.
     */
    private static final int cameraMenuOptionCount = 10;

    /**
     * Lock object, used for temporary interruption of {@link #runTests()}
     */
    private static Lock LOCK = new ReentrantLock();

    /**
     * Instance of {@link MovementFacade} for controlling the fixture.
     */
    private static MovementFacade fixture;

    //private static Thread safeThread;

    //Start of program message; always runs first
    static
    {
        ErrorLogging.logError("START OF PROGRAM");
    }

    public static void main(String[] args)
    {
        //Beginning message to user
        ErrorLogging.logError("========================");
        ErrorLogging.logError("Accuracy Over Life Test");
        ErrorLogging.logError("Version: " + version);
        ErrorLogging.logError("========================");
        try{
            //Create scanner for user input from the console
            inputScanner = new Scanner(System.in);

            //Initialise the fixture, start monitor thread
            fixture = new MovementFacade(LOCK);
            
            //Initialise the config
            ConfigFacade.init();

            //Create the user input value
            int userInput = 0;

            //Main menu loop
            do
            {
                //Show the main menu, wait for user input
                printMainMenu();
                userInput = inputFiltering(inputScanner.nextLine());

                //Perform action based on user input
                switch (userInput)
                {
                    case 1:
                        //Test fixture movement, modify fixture values as necessary
                        testMovement();
                        break;
                    case 2:
                        //Warn user that starting cameras will take a moment.
                        println("Setting up cameras...");
                        println("This may take a moment...");
                        configureCameras();
                        //Set that cameras are successfully configured, to mute runTests warning
                        camerasConfigured = true;
                        break;
                    case 3:
                        //Set serials of the DUTs
                        setDUTSerials();
                        break;
                    case 4:
                        //Change the number of iterations to run the tests
                        setIterationCount();
                        break;
                    case 5:
                        //Set cameras to use in testing
                        setActiveCameras();
                        break;
                    case 6:
                        //Warn user that cameras haven't been set up, if they haven't been set up
                        //Won't warn user if config was imported successfully
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
                            //Save in the logs that cameras may not have been configured.
                            else 
                            {
                                ErrorLogging.logError("WARNING! - Potential for error: Un-initialised cameras.");
                            }
                        }

                        //If there's an unset camera serial, prompt the user to go back and set that up
                        for(String cameraName : OpenCVFacade.getCameraNames())
                        {
                            if(ConfigFacade.getValue(cameraName,ConfigProperties.ACTIVE) != 0 && 
                               ConfigFacade.getSerial(cameraName) == null )
                            {
                                serialsSet = false;
                                break;
                            }
                            else serialsSet = true;
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

                        //Run tests for the given number of iterations
                        runTests();
                        break;
                    case 7:
                        //Show help menu
                        printHelp();
                        break;
                    case 8:
                        //Leave the menu
                        break;
                    default:
                        //Input handling already done by inputFiltering()
                }

        } while (userInput != mainMenuOptionCount);

        }
        //If anything ever goes wrong, catch the error and exit
        catch(Exception e) 
        { 
            ErrorLogging.logError(e); 
            ErrorLogging.logError("ERROR CAUGHT - CLOSING PROGRAM.");
        }
        //Always return the fixture back to the upper limit switch, close all open connections safely
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
                    "\n\t\tCrop dimensions"+
                    "\n\t\tComposite frame count"+
                    "\n\t\tThreshold value");
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
        println("5. Toggle active cameras: Change which cameras" +
                    "\n\twill be used during Run Tests.");
        println("----------------------------------------");
        println("6. Run tests: Run tests, with defined"+
                    "\n\tnumber of iterations. Uses"+
                    "\n\tvalues defined in config file.");
        println("----------------------------------------");
        println("7. Help: Show this help page.");
        println("----------------------------------------");
        println("8. Exit: Close the program.");
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
        println("5. Toggle active cameras");
        println("6. Run tests");
        println("7. Help");
        println("8. Exit");
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
     * Pre-defined method for printing all available cameras and the associated serials in a menu
     */
    private static void printActiveToggleMenu(List<String> cameraList)
    {
        println("Available cameras to toggle:");
        println("------------------------------------");
        for(int index = 0; index < cameraList.size(); index++)
        {
            int humanIndex = index+1;
            String cameraName = (String)cameraList.get(index);
            print(humanIndex + " - " + cameraName + " : ");
            String activity = (ConfigFacade.getValue(cameraName, ConfigProperties.ACTIVE) != 0 ? "active" : "disabled");
            println(activity);
        }
        println( (cameraList.size() + 1) + " - Exit to Main Menu");
        println("------------------------------------");
    }

    /**
     * Pre-defined menu for printing camera configuration options
     */
    private static void printCameraConfigMenu(String cameraName, double tesseractValue)
    {
        println("\n\n");
        println("====================================");
        println("Camera Config Menu:");
        println("------------------------------------");
        println("Current composite frame count: " + 
                ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES));
        println("Current threshold value: " + 
                ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD_VALUE));
        String cropValue = ((ConfigFacade.getValue(cameraName,ConfigProperties.CROP) != 0) ? "yes" : "no");
        println("Will the image be cropped? " + cropValue);
        String thresholdImage = ((ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD) != 0) ? "yes" : "no");
        println("Will the image be thresholded? " + thresholdImage);
        println("Tesseract parsed value for camera " + cameraName + ": " + tesseractValue);
        println("------------------------------------");
        println("1. Change Crop Point");
        println("2. Change Composite Frame Count");
        println("3. Change Threshold Value");
        println("4. Toggle crop");
        println("5. Toggle threshold");
        println("6. Exit");
        println("====================================");
    }


    /**
     * Function for testing movement, and modifying hardware values
     */
    private static void testMovement()
    {
        int userInput = -1;
        //Loop to allow multiple changes to device GPIO settings
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

        //Always wake the camera, to ensure that the image is useful
        fixture.iterationMovement(true);
        double tesseractValue = 0.0;

        //Main camera config loop
        do
        {
            //Show the menu
            printCameraMenu(cameraList);

            //Pick a camera to configure
            int userInput;
            String cameraName = "";
            do
            {
                prompt("Enter a camera number to configure: ");
                userInput = inputFiltering(inputScanner.nextLine());
                userInput--;
            } while (cameraList.size() < userInput && userInput < 0);

            //Leave do-while loop if the user asks to
            if(userInput == (cameraList.size())) break;
            else if(userInput < 0) continue;
            else cameraName = cameraList.get((userInput));

            //Single camera config loop
            do
            {
                //Press button twice, to make sure the DUT is awake
                fixture.pressButton();
                try{ Thread.sleep(2000); } catch(Exception e){ ErrorLogging.logError(e); }
                fixture.pressButton();
                try{ Thread.sleep(2000); } catch(Exception e){ ErrorLogging.logError(e); }

                //Show image 
                File image = OpenCVFacade.showImage(cameraName);

                //Parse the image with Tesseract, to show user what the excel output will be
                tesseractValue = TesseractFacade.imageToDouble(image);

                //User input parsing
                ConfigProperties modifiedProperty = null;
                do
                {
                    //list configurable settings
                    printCameraConfigMenu(cameraName,tesseractValue);

                    userInput = inputFiltering(inputScanner.nextLine(),Menus.CAMERA);
                    switch (userInput)
                    {
                        case 1:
                            modifiedProperty = ConfigProperties.CROP_X;
                            break;
                        case 2:
                            modifiedProperty = ConfigProperties.COMPOSITE_FRAMES;
                            break;
                        case 3:
                            modifiedProperty = ConfigProperties.THRESHOLD_VALUE;
                            break;
                        case 4:
                            modifiedProperty = ConfigProperties.CROP;
                            break;
                        case 5:
                            modifiedProperty = ConfigProperties.THRESHOLD;
                            break;
                        case 6:
                            modifiedProperty = ConfigProperties.PRIME;
                            break;
                        default:
                    }
                } while(modifiedProperty == null);
                
                //Toggle threshold/crop
                if(modifiedProperty == ConfigProperties.THRESHOLD || 
                        modifiedProperty == ConfigProperties.CROP)
                {
                    double newValue = ConfigFacade.getValue(cameraName,modifiedProperty);
                    newValue = Math.abs(newValue - 1);
                    ConfigFacade.setValue(cameraName,modifiedProperty,newValue);
                }

                //Redefine crop points
                else if(modifiedProperty == ConfigProperties.CROP_X)
                { OpenCVFacade.setCrop(cameraName); }

                //Modify number of composite frames, or threshold value
                else if(modifiedProperty == ConfigProperties.COMPOSITE_FRAMES ||
                        modifiedProperty == ConfigProperties.THRESHOLD_VALUE)
                {
                    prompt("Enter new value for this property (" + modifiedProperty.toString() + ": " +
                            //Prompt is in int, as the ultimate values are cast
                            //to int anyways, a decimal would be confusing
                            (int)ConfigFacade.getValue(cameraName,modifiedProperty) + "): ");
                    userInput = inputFiltering(inputScanner.nextLine());
                    ConfigFacade.setValue(cameraName,modifiedProperty,userInput);
                }

                //Exit loop
                else break;
            } while(true);

        } while(true);

        //Save the current config to the config file
        ConfigFacade.saveCurrentConfig();
        println("Configuration complete!");
    }

    /**
     * Sub-function used for defining the serial numbers of the devices under test
     */
    private static void setDUTSerials()
    {
        //Get a list of available cameras
        List<String> cameraList = new ArrayList<>(OpenCVFacade.getCameraNames());
        //Main serial setting loop
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
                //Compensate for off-by-one errors
                userInput--;
            } while (cameraList.size() < userInput || userInput < 0);

            //Leave do-while loop if the user asks to
            if(userInput == (cameraList.size())) break;
            else cameraName = cameraList.get((userInput));

            //Save the serial number.
            //No parsing is ever done on this serial number.
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
     * Function to modify the currently active cameras
     */
    private static void setActiveCameras()
    {
        //Get available cameras
        List<String> cameraList = new ArrayList<>(OpenCVFacade.getCameraNames());

        //Main loop
        do
        {
            //Print menu
            printActiveToggleMenu(cameraList);

            //Pick a camera to configure
            int userInput;
            String cameraName = "";
            do
            {
                prompt("Enter the camera you wish to toggle: ");
                userInput = inputFiltering(inputScanner.nextLine());
                userInput--;
            } while (cameraList.size() < userInput || userInput < 0);

            //Leave do-while loop if the user asks to
            if(userInput == (cameraList.size())) break;
            else cameraName = cameraList.get((userInput));

            //Toggle whether the camera is active, at the config level
            double newValue = ConfigFacade.getValue(cameraName,ConfigProperties.ACTIVE);
            newValue = Math.abs(newValue - 1);
            ConfigFacade.setValue(cameraName,ConfigProperties.ACTIVE,newValue);

        } while(true);
    }

    /**
     * Starts running tests
     */
    private static void runTests()
    {
        println("====================================");
        ErrorLogging.logError("Initialising tests...");

        //Bring the iteration count into the function as a final variable
        //useful for multithreading, which isn't necessary in CLI
        final int localIterations = iterationCount;

        //TODO: Hard-coded value that needs fixing
        boolean prime = false;

        //Create a List of *active* cameras.
        List<String> cameraList = new ArrayList<>();
        for(String cameraName : OpenCVFacade.getCameraNames())
        {
            //if(cameraName != null) { /*println(cameraName);*/ }
            //else ErrorLogging.logError("Null camera!");
            if(ConfigFacade.getValue(cameraName,ConfigProperties.PRIME) != 0)
            {
                prime = true;
            }
            if(ConfigFacade.getValue(cameraName,ConfigProperties.ACTIVE) != 0)
            {
                cameraList.add(cameraName);
            }
        }

        //Initialise the workbook, with the number of cameras and the final output location
        DataSaving.initWorkbook(ConfigFacade.getOutputSaveLocation(),cameraList.size());

        //Do 2 dummy passes, to make completely sure that the devices are awake
        ErrorLogging.logError("DEBUG: Waking devices...");
        fixture.iterationMovement(prime);
        fixture.pressButton();
        fixture.iterationMovement(prime);

        //Create final maps for result images, result values, and camera names
        Map<File,Double> resultMap = new HashMap<>();
        Map<String,File> cameraToFile = new HashMap<>();

        //Initialise cameraToFile, so keys don't shuffle.
        for(String cameraName : cameraList)
        {
            cameraToFile.put(cameraName,new File("/dev/null"));
        }

        ErrorLogging.logError("DEBUG: Starting tests...");
        //Start actually running tests
        //All portions of the test check with the GPIO Run/Pause switch before 
        //continuing, using the Lock object.
        for(int i = 0; i < localIterations; i++)
        {
            println("");
            ErrorLogging.logError("====================================");
            ErrorLogging.logError("Starting iteration " + (i+1) + " of " + localIterations + "...");

            //Move the fixture for one iteration, with whether or not the DUTs need to be primed
            while(!LOCK.tryLock()) {}
            fixture.iterationMovement(prime);
            LOCK.unlock();

            //Wait for the DUT to display an image
            try{ Thread.sleep(1500); } catch(Exception e){ ErrorLogging.logError(e); }

            //For all available cameras:
            //  take an image, process it, and save it to a file
            //  put that file into the camera name file Map
            for(String cameraName : cameraList)
            {
                while(!LOCK.tryLock()) {}
                File file = OpenCVFacade.completeProcess(cameraName);
                LOCK.unlock();

                while(!LOCK.tryLock()) {}
                cameraToFile.replace(cameraName,file);
                LOCK.unlock();
            }

            //ONCE ALL IMAGES ARE CREATED
            //Re-iterate over list of cameras, parse the images with Tesseract, then add 
            //the parsed value to the map
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
                ErrorLogging.logError("Tesseract final output: " + result);
                LOCK.unlock();
            }
            //Write all given values to the Excel file
            while(!LOCK.tryLock()) {}
            DataSaving.writeValues(i,resultMap,cameraToFile);
            LOCK.unlock();

            //LO detection and avoidance
            for(Double result : resultMap.values())
            {
                if(result <= 1.0 || result >= 117.0)
                {
                    fixture.goUp();
                    try{ Thread.sleep(20000); } catch(Exception e){ ErrorLogging.logError(e); }
                }
            }
            //Clear the result map
            //DO NOT CLEAR camera to file Map. This will change the order of the objects within it
            resultMap.clear();
        }
        //Close the Excel workbook
        DataSaving.closeWorkbook(cameraList.size());
        //Alert the user to testing being complete
        println("=======================================");
        println("Testing complete!");
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
        println("The program has exited successfully. Please press Ctrl-c to return to the terminal prompt.");
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
        ErrorLogging.logError("DEBUG: Invalid User Input!!! - Message to user: '" + input + "'");
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
