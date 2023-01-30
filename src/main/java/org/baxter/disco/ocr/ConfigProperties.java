package org.baxter.disco.ocr;

/**
 * Enum of possible config properties.
 *
 * @author Blizzard Finnegan
 * @version 2.1.0, 27 Jan. 2023
 */
public enum ConfigProperties
{
    /**
     *X coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_X("Crop X","cropX",275.0),
    /**
     *Y coordinate of the top-left coordinate for the newly cropped image.
     */
    CROP_Y("Crop Y","cropY",205.0),
    /**
     *Width of the newly cropped image.
     */
    CROP_W("Crop Width","cropW",80.0),
    /**
     *Height of the newly cropped image.
     */
    CROP_H("Crop Height","cropH",50.0),
    /**
     * Whether or not to threshold the image during processing.
     */
    THRESHOLD("Toggle threshold","threshold",1.0),
    /**
     * Whether or not to threshold the image during processing.
     */
    CROP("Toggle crop","crop",1.0),
    /**
     *How many frames to composite together while processing this camera's image.
     */
    COMPOSITE_FRAMES("Composite frame count","compositeCount",5.0),
    /**
     * Whether or not to press the button on the device twice, when under test.
     */
    PRIME("Prime device?","prime",0.0),
    /**
     * Where the threshold point should land.
     */
    THRESHOLD_VALUE("Threshold value","thresholdValue",50.0);

    /**
     * Internal storage of human-readable name/meaning
     */
    private final String print;

    /**
     * Internal storage of config-readable name
     */
    private final String config;

    /**
     * Internal storage of config-readable name
     */
    private final double defaultValue;

    private ConfigProperties(String print, String config, double defaultValue) { this.print = print; this.config = config; this.defaultValue = defaultValue; }

    /**
     * Getter for the config-readable name of the value.
     *
     * @return String of the name of the value.
     */
    public double getDefaultValue()
    { return defaultValue; }

    /**
     * Getter for the config-readable name of the value.
     *
     * @return String of the name of the value.
     */
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
