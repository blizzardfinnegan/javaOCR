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
     * <pre>
     * X coordinate of the top-left coordinate for the newly cropped image.
     *
     * Human readable name: "Crop X"
     * Config name:         "cropX"
     * Default value:       "275.0"
     * </pre>
     */
    CROP_X("Crop X","cropX",275.0),
    /**
     * <pre>
     * Y coordinate of the top-left coordinate for the newly cropped image.
     *
     * Human readable name: "Crop Y"
     * Config name:         "cropY"
     * Default value:       "205.0"
     * </pre>
     */
    CROP_Y("Crop Y","cropY",205.0),
    /**
     * <pre>
     * Width of the newly cropped image.
     *
     * Human readable name: "Crop Width"
     * Config name:         "cropW"
     * Default value:       "80.0"
     * </pre>
     */
    CROP_W("Crop Width","cropW",80.0),
    /**
     * <pre>
     * Height of the newly cropped image.
     *
     * Human readable name: "Crop Height"
     * Config name:         "cropH"
     * Default value:       "50.0"
     * </pre>
     */
    CROP_H("Crop Height","cropH",50.0),
    /**
     * <pre>
     * Whether or not to threshold the image during processing.
     *
     * Human readable name: "Toggle Threshold"
     * Config name:         "threshold"
     * Default value:       "1.0"
     * </pre>
     */
    THRESHOLD("Toggle threshold","threshold",1.0),
    /**
     * <pre>
     * Whether or not to crop the image during processing.
     *
     * Human readable name: "Toggle crop"
     * Config name:         "crop"
     * Default value:       "1.0"
     * </pre>
     */
    CROP("Toggle crop","crop",1.0),
    /**
     * <pre>
     *How many frames to composite together while processing this camera's image.
     *
     * Human readable name: "Composite frame count"
     * Config name:         "compositeCount"
     * Default value:       "5.0"
     * </pre>
     */
    COMPOSITE_FRAMES("Composite frame count","compositeCount",5.0),
    /**
     * <pre>
     * Whether or not to press the button on the device twice, when under test.
     *
     * Human readable name: "Prime device"
     * Config name:         "prime"
     * Default value:       "0.0"
     * </pre>
     */
    PRIME("Prime device?","prime",0.0),

    /**
     * <pre>
     * Where the threshold point should land.
     *
     * Human readable name: "Threshold value"
     * Config name:         "thresholdValue"
     * Default value:       "45.0"
     * </pre>
     */
    THRESHOLD_VALUE("Threshold value","thresholdValue",45.0),

    /**
     * <pre>
     * Whether the camera should be active.
     *
     * Human readable name: "Camera active"
     * Config name:         "active"
     * Default value:       "1.0"
     * </pre>
     */
    ACTIVE("Camera active?","active",1.0);

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
     * Getter for the default value for this config property
     *
     * @return double of the default value
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
