package org.baxter.disco.ocr;

/**
 * Wrapper class around Gui.
 *
 * Maven will not build the {@link Gui} properly, since it inherits from {@link javafx.application.Application}. 
 * This will start the Gui's main function, with no other functionality.
 *
 * @author Blizzard Finnegan
 * @version 1.0.0, 30 Jan. 2023
 */
public class GuiStarter
{
    public static void main(String[] args)
    {
        Gui.main(args);
    }
}
