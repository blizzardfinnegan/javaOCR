package org.baxter.disco.ocr;

/**
 * Enum of possible config properties.
 *
 * @author Blizzard Finnegan
 * @version 23 Jan. 2023
 */
public enum ConfigProperties
{
    /**
     *X coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_X("Crop X"),
    /**
     *Y coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_Y("Crop Y"),
    /**
     *Width of the newly cropped image.
     */
    CROP_W("Crop Width"),
    /**
     *Height of the newly cropped image.
     */
    CROP_H("Crop Height"),
    /**
     * Gamma value set to the camera.
     */
    GAMMA("Gamma value"),
    /**
     *How many frames to composite together while processing this camera's image.
     */
    COMPOSITE_FRAMES("Composite frame count"),
    /**
     * Whether or not to press the button on the device twice, when under test.
     */
    PRIME("Prime device?");

    /**
     *Internal storage of human-readable name/meaning
     */
    private final String value;

    private ConfigProperties(String value) { this.value = value; }

    /**
     * Getter for the human-readable name of the value.
     *
     * @return String of the name of the value.
     */
    @Override
    public String toString(){ return this.value; }
}
