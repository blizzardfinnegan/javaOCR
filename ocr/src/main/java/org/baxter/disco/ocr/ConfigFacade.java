package org.baxter.disco.ocr;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Facade for working with config files.
 * Stores current config setup in RAM for easy access.
 * Can write to file when requested, reads from file on 
 * initial start.
 *
 * @author Blizzard Finnegan
 * @version 19 Jan. 2023
 */
public class ConfigFacade
{
    /**
     * Location for the current config to be saved to.
     * 
     * TODO: Set default path
     * Defaults to [currentWorkingDirectory]/config.properties
     */
    private static String configFileLocation = "config.ini";

    /**
     * Location to save images to. 
     *
     * TODO: Set default path
     */
    private static String imageSaveLocation;

    /**
     * Read-Only List of available cameras.
     * 
     * Is not set as final, due to current initialisation procedure.
     */
    public static final List<String> activeCameras = new ArrayList<>();

    //For values that are ultimately ints, truncate.
    //For values that are ultimately booleans, anything that isn't 0 should be considered True.
    /**
     * Map of all config values relating to the camera.
     */
    private static final Map<String,Map<ConfigProperties,Double>> configMap = new HashMap<>();

    private static final Configurations CONFIGURATIONS = new Configurations();
    private static FileBasedConfigurationBuilder<INIConfiguration> CONFIG_BUILDER = CONFIGURATIONS.iniBuilder(configFileLocation);
    private static INIConfiguration CONFIG_STORE;

    static
    {
        CONFIG_BUILDER = CONFIGURATIONS.iniBuilder(configFileLocation);
        try { CONFIG_STORE = CONFIG_BUILDER.getConfiguration(); }
        catch(Exception e){ ErrorLogging.logError(e); }
        loadConfig();
    }
    /**
     * Get a given config value. 
     * All values are stored as doubles.
     * Ints should be truncated.
     * Any boolean that should be false should be stored as 0.
     *
     * @param cameraName    Name of the camera (as defined in {@value #activeCameras})
     * @param property      name of the property ({@link}ConfigProperties)
     * @return double of config value. Returns -1 if invalid input.
     */
    public static double getValue(String cameraName, ConfigProperties property)
    {
        double output = -1.0;
        if(!activeCameras.contains(cameraName)) return output;
        Map<ConfigProperties,Double> cameraConfig = configMap.get(cameraName);
        if(cameraConfig.equals(null)) return output;
        output = cameraConfig.get(property);
        return output;
    }

    /**
     * Getter for the saved image location.
     *
     * @return Absolute path of the image save location.
     */
    public static String getImgSaveLocation()
    {
        return imageSaveLocation;
    }

    /**
     * Setter for the saved image location.
     *
     * @return false if path does not exist; otherwise true
     */
    public static boolean setImgSaveLocation(String path)
    {
        boolean output = false;
        if(Files.exists(Paths.get(path)))
        {
            imageSaveLocation = path;
            output = true;
        }
        return output;
    }
    
    /**
     * Set a given config value.
     * DOES NOT SAVE VALUE TO FILE.
     *
     * @param cameraName    Name of the camera (as defined in {@value #activeCameras})
     * @param property      name of the property ({@link}ConfigProperties)
     * @param propertyValue Value of the property
     * @return true if set successfully, otherwise false
     */
    public static boolean setValue(String cameraName, ConfigProperties property, double propertyValue)
    {
        boolean output = false;
        if(!activeCameras.contains(cameraName)) return output;
        Map<ConfigProperties,Double> cameraConfig = configMap.get(cameraName);
        if(cameraConfig.equals(null)) return output;
        Double oldValue = cameraConfig.get(property);
        return cameraConfig.replace(property,oldValue,propertyValue);
    }

    /**
     * Save current config to a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveConfig(String filename)
    {
        boolean output = false;
        try
        { 
            CONFIG_BUILDER.save(); 
            output = true;
        }
        catch(Exception e)
        { 
            ErrorLogging.logError(e); 
        }

        return output;
    }

    /**
     * Save current config to the default file location.
     *
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveConfig() { return saveConfig(configFileLocation); }

    /** TODO: Implement
     * Load config from a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig(String filename)
    {
        boolean output = false;

        return output;
    }

    /**
     * Load config from the default file location.
     *
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig() { return loadConfig(configFileLocation); }
}
