package org.baxter.disco.ocr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.*;
import org.apache.commons.configuration2.builder.fluent.*;

import java.util.List;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Facade for working with config files, using the Apache Commons 
 * Configuration library.
 * Stores current config setup in a HashMap for easy and quick access.
 * Can write to file when requested, reads from file on initial start.
 *
 * @author Blizzard Finnegan
 * @version 20 Jan. 2023
 */
public class ConfigFacade
{
    /**
     * Location for the current config to be saved to.
     * 
     * Defaults to [currentWorkingDirectory]/config.ini
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
     * TODO: Fill activeCameras
     */
    public static final List<String> activeCameras = new ArrayList<>();

    //For values that are ultimately ints, truncate.
    //For values that are ultimately booleans, anything that isn't 0 should be considered True.
    /**
     * Map of all config values relating to the camera.
     */
    private static final Map<String, Map<ConfigProperties, Double>> configMap = new HashMap<>();

    private static final Configurations CONFIGURATIONS = new Configurations();
    private static FileBasedConfigurationBuilder<INIConfiguration> CONFIG_BUILDER = CONFIGURATIONS.iniBuilder(configFileLocation);
    private static INIConfiguration CONFIG_STORE;

    static
    {
        //FIXME
        loadConfig();
    }
    /**
     * Get a given config value. 
     * All values are stored as doubles.
     * Ints should be truncated.
     * Any boolean that should be false should be stored as 0.
     *
     * @param cameraName    Name of the camera (as defined in {@link #activeCameras})
     * @param property      name of the property ({@link ConfigProperties})
     *
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
     * @param cameraName    Name of the camera 
     * @param property      name of the property 
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
     * @param filename  Name and location of the config file (typically, config.ini)
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveDefaultConfig(String filename)
    {
        boolean output = false;
        Set<String> cameraNames = OpenCVFacade.getCameraNames();
        for(String camera : cameraNames)
        {
            for(ConfigProperties property : ConfigProperties.values())
            {
                String propertyName = camera + "/" + property.toString();
                String propertyValue = "-1.0";
                switch(property)
                {
                    case PRIME:
                        propertyValue = "true";
                        break;
                    case GAMMA:
                        propertyValue = "1.0";
                        break;
                    case CROP_Y:
                    case CROP_X:
                        propertyValue = "250.0";
                        break;
                    case CROP_H:
                    case CROP_W:
                        propertyValue = "300.0";
                        break;
                    case COMPOSITE_FRAMES:
                        propertyValue = "5.0";
                }
                CONFIG_STORE.addProperty(propertyName,propertyValue);
            }
        }
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
    public static boolean saveDefaultConfig() { return saveDefaultConfig(configFileLocation); }

    /**FIXME
     * Save current config to a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveCurrentConfig(String filename)
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
    public static boolean saveCurrentConfig() { return saveCurrentConfig(configFileLocation); }

    /** 
     * Load config from a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig(String filename)
    {
        boolean output = false;
        if(Files.isRegularFile(Path.of(URI.create(filename))))
        {
            try{ CONFIG_STORE = CONFIGURATIONS.ini(filename); }
            catch(Exception e){ ErrorLogging.logError(e); }

            for(String sectionName : CONFIG_STORE.getSections())
            {
                Map<ConfigProperties,Double> savedSection = new HashMap<>();
                var section = CONFIG_STORE.getSection(sectionName);

                for(ConfigProperties configState : ConfigProperties.values())
                {
                    if(!section.containsKey(configState.toString()))
                    {
                        ErrorLogging.logError("CONFIG LOAD ERROR!!! - Invalid config file.");
                        return output;
                    }
                    else
                    {
                        Double configValue = section.getDouble(configState.toString());
                        savedSection.put(configState,configValue);
                    }
                }
                if(configMap.containsKey(sectionName))
                {
                    configMap.put(sectionName,savedSection);
                }
            }
            output = true;
        }

        if(!output) ErrorLogging.logError("CONFIG LOAD ERROR!!! - Invalid path.");
        return output;
    }

    /**
     * Load config from the default file location.
     *
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig() { return loadConfig(configFileLocation); }
}
