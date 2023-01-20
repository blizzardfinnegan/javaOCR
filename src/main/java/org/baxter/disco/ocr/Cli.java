package org.baxter.disco.ocr;

/**
 * CLI for the Fixture.
 *
 * Current build runs a preset Main function.
 * Will build out a proper CLI interface.
 *
 * @author Blizzard Finnegan
 * @version 19 Jan. 2023
 */
public class Cli
{
    public static void main(String[] args)
    {
        //Test movement
        MovementFacade.testMotions();

        OpenCVFacade.iteration();
        MovementFacade.closeGPIO();
    }
}
