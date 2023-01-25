package org.baxter.disco.ocr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.*;
import org.apache.commons.configuration2.builder.fluent.*;

import java.util.List;
import java.io.File;
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
 * @version 24 Jan. 2023
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
     */
    private static String imageSaveLocation;

    /**
     * Location to save output XLSX file to.
     */
    private static String outputSaveLocation;

    //For values that are ultimately ints, truncate.
    //For values that are ultimately booleans, anything that isn't 0 should be considered True.
    /**
     * Map of all config values relating to the camera.
     */
    private static final Map<String, Map<ConfigProperties, Double>> configMap = new HashMap<>();

    /**
     * Base class used for interacting with Configuration files.
     */
    private static final Configurations CONFIGURATIONS = new Configurations();

    /**
     * Builder for the main Configuration object.
     *
     * Also used for saving out to file.
     */
    private static FileBasedConfigurationBuilder<INIConfiguration> CONFIG_BUILDER;

    /**
     * Object used for reading and writing config values.
     *
     * This is used only for file-read and file-write operations.
     */
    private static INIConfiguration CONFIG_STORE;

    static
    {
        File configFile = new File(configFileLocation);
        boolean newConfig = true;
        try{ newConfig = configFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }
        CONFIGURATIONS.iniBuilder(configFileLocation);
        File imageLocation  = new File("images");
        imageLocation.mkdir();
        File outputFile = new File("output.xlsx");
        try{ outputFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }
        outputSaveLocation = "";
        if(newConfig) 
        {
            boolean saveSuccessful = saveDefaultConfig();
            if(!saveSuccessful) ErrorLogging.logError("Save config failed!!!");
        }
        loadConfig();
        //CONFIG_BUILDER.setAutoSave(true);
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
        List<String> activeCameras = new ArrayList<>(OpenCVFacade.getCameraNames());
        if(!activeCameras.contains(cameraName)) return output;
        Map<ConfigProperties,Double> cameraConfig = configMap.get(cameraName);
        if(cameraConfig == null) return output;
        output = cameraConfig.get(property);
        return output;
    }

    /**
     * Getter for the location of the output XLSX file.
     *
     * @return Absolute path of the image save location.
     */
    public static String getOutputSaveLocation()
    {
        return outputSaveLocation;
    }

    /**
     * Setter for the location of the output XLSX file.
     *
     * @return false if path does not exist; otherwise true
     */
    public static boolean setOutputSaveLocation(String path)
    {
        boolean output = false;
        if(Files.exists(Paths.get(path)))
        {
            outputSaveLocation = path;
            output = true;
        }
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
        List<String> activeCameras = new ArrayList<>(OpenCVFacade.getCameraNames());
        if(!activeCameras.contains(cameraName)) return output;
        Map<ConfigProperties,Double> cameraConfig = configMap.get(cameraName);
        if(cameraConfig.equals(null)) return output;
        Double oldValue = cameraConfig.get(property);
        return cameraConfig.replace(property,oldValue,propertyValue);
    }

    //**********************************************
    //SAVE AND LOAD SETTINGS
    //**********************************************
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
                ErrorLogging.logError("DEBUG: Attempting to save to config: ");
                ErrorLogging.logError("DEBUG: " + propertyName + ", " + propertyValue);
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

    /**
     * Save current config to a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveCurrentConfig(String filename)
    {
        boolean output = false;
        List<String> activeCameras = new ArrayList<>(OpenCVFacade.getCameraNames());
        for(String camera : activeCameras)
        {
            for(ConfigProperties property : ConfigProperties.values())
            {
                String propertyName = camera + "/" + property.toString();
                String propertyValue =configMap.get(camera).get(property).toString();
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
        File file = new File(filename);
        if(Files.isRegularFile(Path.of(file.toURI())))
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
