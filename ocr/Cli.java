package org.baxter.disco.ocr;

/**
 *
 */
public class Cli
{
    public static void main(String[] args)
    {
        MovementFacade.testMotions();
        MovementFacade.closeGPIO();
    }
}
