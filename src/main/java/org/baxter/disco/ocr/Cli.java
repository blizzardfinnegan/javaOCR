package org.baxter.disco.ocr;

import java.util.Scanner;

/**
 * CLI for the Fixture.
 *
 * Current build runs a preset Main function.
 * Will build out a proper CLI interface.
 *
 * @author Blizzard Finnegan
 * @version 23 Jan. 2023
 */
public class Cli
{
    private static int iterationCount = 5;
    private static Scanner inputScanner;
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

    private static void println(String input) { System.out.println(input); }

    private static void prompt(String input) { System.out.print(input); }

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

    private static void configureCamera()
    {
        println("Configuring camera...");
        //might want to be a separate function in OpenCVFacade
        println("Configuration complete!");
    }

    private static void setIterationCount() 
    { 
        prompt("Input the number of test iterations to complete: ");
        int input = inputFiltering(inputScanner.nextLine(), false);
        if(input != -1)
        {
            iterationCount = input;
        }
    }

    private static void runTests()
    {
        println("Running tests");
    }

    private static int inputFiltering(String input) 
    { return inputFiltering(input,true); }

    private static int inputFiltering(String input, boolean bounded)
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
            if(bounded)
            {
                if(output < 0 || output > menuOptionCount)
                {
                    invalidInput();
                    output = -1;
                    return output;
                }
            }
        }
        return output;
    }

    private static void invalidInput()
    {
        println("");
        println("=================================================");
        println("Invalid input! Please input a number from 1 to 6.");
        println("=================================================");
        println("");
    }
}
