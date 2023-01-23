package org.baxter.disco.ocr;

import java.util.Scanner;

/**
 * CLI for the Fixture.
 *
 * Creates a terminal-based user interface for the other 
 * classes in this package (with the exception of {@link Gui} [for now].
 *
 * @author Blizzard Finnegan
 * @version 23 Jan. 2023
 */
public class Cli
{
    /**
     * Currently saved iteration count
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
     * Number of options currently available in the menu.
     */
    private static final int menuOptionCount = 6;

    public static void main(String[] args)
    {
        inputScanner = new Scanner(System.in);

        int userInput = 0;

        do
        {
            printMainMenu();
            userInput = inputFiltering(inputScanner.nextLine());
            switch (userInput)
            {
                case 1:
                    println("Testing movement...");
                    MovementFacade.testMotions();
                    break;
                case 2:
                    configureCamera();
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

        inputScanner.close();
        ErrorLogging.closeLogs();
        MovementFacade.closeGPIO();
    }

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
                    "\n\tof motion.");
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

    /**
     * Sub-function used to configure cameras.
     */
    private static void configureCamera()
    {
        //might want to be a separate function in OpenCVFacade
        println("Configuration complete!");
    }

    /**
     * Setter for {@link #iterationCount}
     */
    private static void setIterationCount() 
    { 
        prompt("Input the number of test iterations to complete: ");
        int input = inputFiltering(inputScanner.nextLine(), false);
        if(input != -1)
        {
            iterationCount = input;
        }
    }

    /**
     * Starts running tests in {@link OpenCVFacade}
     */
    private static void runTests()
    {
        println("Running tests");
    }

    /**
     * Parse the user's input at the main menu, and check it for errors.
     *
     * @param input     The unparsed user input, directly from the {@link Scanner}
     */
    private static int inputFiltering(String input) 
    { return inputFiltering(input,true); }

    /**
     * Parse the user's input, and check it for errors.
     *
     * @param input     The unparsed user input, directly from the {@link Scanner}
     * @param mainMenu  Whether or not the parsed input is a main menu value
     * 
     * @return The parsed value from the user. Returns -1 upon any error.
     */
    private static int inputFiltering(String input, boolean mainMenu)
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
            if(mainMenu)
            {
                if(output > menuOptionCount)
                {
                    invalidPromptInput();
                    output = -1;
                }
            }
        }
        return output;
    }

    /**
     * Prints a message when user inputs an invalid main menu value.
     */
    private static void invalidPromptInput()
    {
        invalidInput("Please input a number from 1 to 6.");
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
     * @param String    Custom message to print in the error block.
     */
    private static void invalidInput(String input)
    {
        println("");
        println("=================================================");
        println("Invalid input! - " + input);
        println("=================================================");
        println("");
    }
}
