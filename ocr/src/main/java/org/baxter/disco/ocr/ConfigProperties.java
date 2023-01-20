package org.baxter.disco.ocr;

/**
 * Enum of possible config properties.
 *
 * @author Blizzard Finnegan
 * @version 17 Jan. 2023
 */
public enum ConfigProperties
{
    CROP_X("Crop X"),
    CROP_Y("Crop Y"),
    CROP_W("Crop Width"),
    CROP_H("Crop Height"),
    GAMMA("Gamma value"),
    COMPOSITE_FRAMES("Composite frame count"),
    PRIME("Prime device?");

    //Internal storage of human-readable name/meaning
    private final String value;

    private ConfigProperties(String value) { this.value = value; }

    @Override
    public String toString(){ return this.value; }
}
