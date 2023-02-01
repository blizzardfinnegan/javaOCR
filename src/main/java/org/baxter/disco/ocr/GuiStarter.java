package org.baxter.disco.ocr;

/**
 * Wrapper class around GuiView.
 *
 * Maven will not build the {@link GuiView} properly, since it inherits from {@link javafx.application.Application}. 
 * This will start the Gui's main function, with no other functionality.
 *
 * @author Blizzard Finnegan
 * @version 1.0.1, 01 Feb. 2023
 */
public class GuiStarter
{
    public static void main(String[] args)
    { GuiView.main(args); }
}
