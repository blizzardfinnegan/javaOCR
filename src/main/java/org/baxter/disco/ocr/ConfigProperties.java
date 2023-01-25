package org.baxter.disco.ocr;

/**
 * Enum of possible config properties.
 *
 * @author Blizzard Finnegan
 * @version 1.0.0, 25 Jan. 2023
 */
public enum ConfigProperties
{
    /**
     *X coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_X("Crop X","cropX"),
    /**
     *Y coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_Y("Crop Y","cropY"),
    /**
     *Width of the newly cropped image.
     */
    CROP_W("Crop Width","cropW"),
    /**
     *Height of the newly cropped image.
     */
    CROP_H("Crop Height","cropH"),
    /**
     * Gamma value set to the camera.
     */
    GAMMA("Gamma value","gamma"),
    /**
     *How many frames to composite together while processing this camera's image.
     */
    COMPOSITE_FRAMES("Composite frame count","compositeCount"),
    /**
     * Whether or not to press the button on the device twice, when under test.
     */
    PRIME("Prime device?","prime");

    /**
     * Internal storage of human-readable name/meaning
     */
    private final String print;

    /**
     * Internal storage of config-readable name
     */
    private final String config;

    private ConfigProperties(String print, String config) { this.print = print; this.config = config; }

    public String getConfig()
    { return config; }

    /**
     * Getter for the human-readable name of the value.
     *
     * @return String of the name of the value.
     */
    @Override
    public String toString(){ return this.print; }
}
