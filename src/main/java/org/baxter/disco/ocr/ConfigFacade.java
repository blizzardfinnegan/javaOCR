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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Facade for working with config files, using the Apache Commons 
 * Configuration library.
 * Stores current config setup in a HashMap for easy and quick access.
 * Can write to file when requested, reads from file on initial start.
 *
 * @author Blizzard Finnegan
 * @version 1.3.0, 08 Feb. 2023
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
    private static String imageSaveLocation = "images";

    /**
     * Location to save output XLSX file to.
     */
    public static String outputSaveLocation = "outputData-" + 
        (LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) + ".xlsx";

    /**
     * Map of all config values relating to the camera.
     * For values that are ultimately ints, truncate.
     * For values that are ultimately booleans, anything that isn't 0 should be considered True.    
     */
    private static final Map<String, Map<ConfigProperties, Double>> configMap = new HashMap<>();

    /**
     * Temporary storage for the DUT's serial number.
     */
    private static final Map<String, String> DUT_SERIALS = new HashMap<>();

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
        ErrorLogging.logError("Starting configuration setup...");
        CONFIG_STORE = null;
        File configFile = new File(configFileLocation);
        boolean newConfig = true;
        try{ newConfig = configFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }

        ErrorLogging.logError("Creating config file interface...");
        CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class)
            .configure(new Parameters().fileBased().setFile(configFile));

        ErrorLogging.logError("Attempting to import config..."); 
        if(!newConfig)
        {
            try { CONFIG_STORE = CONFIG_BUILDER.getConfiguration(); }
            catch(Exception e) { ErrorLogging.logError(e); }
            finally 
            { 
                if(CONFIG_STORE == null) 
                    ErrorLogging.logError("CONFIG INIT ERROR!!! - Unsuccessful config initialisation. Camera commands will fail!"); 
                else
                    ErrorLogging.logError("Config successfully imported!");
            }
        }
        else
        {
            ErrorLogging.logError("Unable to import config. Loading defaults...");
            saveDefaultConfig();
        }

        ErrorLogging.logError("Creating image storage directories...");
        File imageLocation  = new File(imageSaveLocation);
        imageLocation.mkdir();
        File debugImageLocation  = new File(imageSaveLocation + "/debug");
        debugImageLocation.mkdir();
        File configImageLocation  = new File(imageSaveLocation + "/config");
        configImageLocation.mkdir();

        ErrorLogging.logError("Creating output file....");
        File outputFile = new File(outputSaveLocation);
        try{ outputFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }
        if(newConfig) 
        {
            boolean saveSuccessful = saveDefaultConfig();
            if(!saveSuccessful) ErrorLogging.logError("Save config failed!!!");
            else ErrorLogging.logError("Configuration settings set up!");
        }
        else 
        { 
            loadConfig(); 
            ErrorLogging.logError("Configuration settings loaded!");
        }
        CONFIG_BUILDER.setAutoSave(true);
    }
    /**
     * Get a given config value. 
     * All values are stored as doubles.
     * Ints should be truncated.
     * Any boolean that should be false should be stored as 0.
     *
     * @param cameraName    Name of the camera (defined in {@link OpenCVFacade})
     * @param property      name of the property ({@link ConfigProperties})
     *
     * @return double of config value. Returns 0 if invalid input.
     * (Under normal circumstances, I would be returning -1, but because this 
     *  function is fed into a function expecting all values to be positive, 
     *  that practice has been set aside.)
     */
    public static double getValue(String cameraName, ConfigProperties property)
    {
        double output = 0.0;
        if(!configMap.keySet().contains(cameraName)) 
        {
            ErrorLogging.logError("CONFIG ERROR!!! - Invalid camera name: " + cameraName);
            ErrorLogging.logError("\tKey set: " + configMap.keySet().toString());
            ErrorLogging.logError("\tProperty: " + property.getConfig());
            ErrorLogging.logError("\tconfigMap keys: " + configMap.keySet().toString());
            return output;
        }
        Map<ConfigProperties,Double> cameraConfig = configMap.get(cameraName);
        output = cameraConfig.get(property);
        //Debug logger. 
        //NOTE THAT THIS BREAKS TUI MENUS, AS OF ErrorLogging 1.1.0
        //ErrorLogging.logError("DEBUG: getValue - return value: " + cameraName + "/" + property.getConfig() + " = " + output);
        return output;
    }

    /**
     * Initialises local list of available cameras
     */
    public static void init()
    {
        //ErrorLogging.logError("Starting import...");
    }

    /**
     * Getter for the location of the output XLSX file.
     *
     * @return Absolute path of the image save location.
     */
    public static String getOutputSaveLocation()
    { return outputSaveLocation; }

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
    { return imageSaveLocation; }

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
        if(cameraConfig == null) return output;
        Double oldValue = cameraConfig.get(property);
        output = cameraConfig.replace(property,oldValue,propertyValue);
        saveCurrentConfig();
        return output;
    }

    /**
     * Setter for a Device Under Test's serial number.
     *
     * @param cameraName    The camera observing the given serial number 
     * @param serial        The serial of the DUT 
     */
    public static void setSerial(String cameraName, String serial)
    { DUT_SERIALS.put(cameraName,serial); }

    /**
     * Getter for a Device Under Test's serial number.
     *
     * @param cameraName    The camera observing the given serial number
     *
     * @return The DUT's serial
     */
    public static String getSerial(String cameraName)
    { 
        if(!DUT_SERIALS.keySet().contains(cameraName)) return null;
        return DUT_SERIALS.get(cameraName); 
    }

    public static Map<String,String> getSerials()
    { return DUT_SERIALS; }

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
        CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class).configure(new Parameters().fileBased().setFile(new File(filename)));
        try
        { 
            CONFIG_STORE = CONFIG_BUILDER.getConfiguration();
        }
        catch(Exception e) { ErrorLogging.logError(e); }
        finally { if(CONFIG_STORE == null) ErrorLogging.logError("CONFIG INIT ERROR!!! - Unsuccessful config initialisation. Camera commands will fail!"); }
        for(String camera : cameraNames)
        {
            Map<ConfigProperties,Double> cameraConfig = new HashMap<>();
            for(ConfigProperties property : ConfigProperties.values())
            {
                String propertyName = camera + "." + property.getConfig();
                double propertyValue = property.getDefaultValue();
                cameraConfig.put(property,propertyValue);
                //ErrorLogging.logError("DEBUG: Attempting to save to config: ");
                //ErrorLogging.logError("DEBUG: " + propertyName + ", " + propertyValue);
                CONFIG_STORE.addProperty(propertyName,propertyValue);
            }
            configMap.put(camera,cameraConfig);
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
                String propertyName = camera + "." + property.toString();
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
        boolean emptyMap = configMap.keySet().size() == 0;
        boolean output = false;
        File file = new File(filename);
        try
        {
        if(file.createNewFile())
            return saveDefaultConfig();
        }
        catch(Exception e)
        {
            ErrorLogging.logError(e);
            return saveDefaultConfig();
        }
        List<String> cameraNames = new ArrayList<>(OpenCVFacade.getCameraNames());
        if(Files.isRegularFile(Path.of(file.toURI())))
        {
            try
            { 
                CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class).configure(new Parameters().fileBased().setFile(file));
                CONFIG_STORE = CONFIG_BUILDER.getConfiguration();
            }
            catch(Exception e){ ErrorLogging.logError(e); }

            Set<String> configSections = CONFIG_STORE.getSections();
            //ErrorLogging.logError("DEBUG: imported sections - " + configSections.toString());
            //ErrorLogging.logError("DEBUG: empty map? : " + (configMap.keySet().size() == 0));
            //ErrorLogging.logError("DEBUG: imported section size - " + configSections.size());
            for(String sectionName : configSections)
            {
                Map<ConfigProperties,Double> savedSection = new HashMap<>();
                String subSectionPrefix = "";
                for(String cameraName : cameraNames)
                {
                    if(cameraName == null)
                    {
                        ErrorLogging.logError("CONFIG LOAD ERROR!!! - Empty camera name.");
                        continue;
                    }
                    else if(sectionName == null)
                    {
                        ErrorLogging.logError("CONFIG LOAD ERROR!!! - Invalid imported section name.");
                        continue;
                    }
                    else if(sectionName.equals(cameraName))
                    {
                        subSectionPrefix = cameraName;
                        break;
                    }
                }

                if(subSectionPrefix.equals("")) 
                {
                    ErrorLogging.logError("CONFIG LOAD ERROR!!! - Failed import from file. Setting default config.");
                    return saveDefaultConfig();
                }

                for(ConfigProperties configState : ConfigProperties.values())
                {
                    Double configValue = CONFIG_STORE.getDouble(sectionName + "." + configState.getConfig());
                    //ErrorLogging.logError("DEBUG: Imported config value: " + Double.toString(configValue));
                    savedSection.put(configState,configValue);
                }
                if(emptyMap) configMap.put(sectionName,savedSection);
                else
                {
                    for(String key : configMap.keySet())
                    {
                        //ErrorLogging.logError("DEBUG: configMap Key: " + key + " ?= " + sectionName);
                        if( key.equals(sectionName))
                        { configMap.put(key,savedSection); }
                    }
                }
            }
            output = true;
        }

        if(!output) ErrorLogging.logError("CONFIG LOAD ERROR!!! - Invalid path.");
        else Cli.configImported();
        return output;
    }

    /**
     * Load config from the default file location.
     *
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig() { return loadConfig(configFileLocation); }
}
