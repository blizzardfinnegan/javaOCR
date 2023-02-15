package org.baxter.disco.ocr;

//Standard imports
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;

//Apache Commons Configuration imports
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;

/**
 * Facade for working with config files, using the Apache Commons 
 * Configuration library.
 * Stores current config setup in a HashMap for easy and quick access.
 * Can write to file when requested, reads from file on initial start.
 *
 * @author Blizzard Finnegan
 * @version 1.3.1, 09 Feb. 2023
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
    private static String imageSaveLocation = "images-" + 
        (LocalDateTime.now().format(ErrorLogging.fileDatetime));

    /**
     * Location to save output XLSX file to.
     */
    public static String outputSaveLocation = "outputData/" + 
        (LocalDateTime.now().format(ErrorLogging.fileDatetime)) + ".xlsx";

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

    //This block will ALWAYS run first.
    static 
    {
        ErrorLogging.logError("Starting configuration setup...");
        //Give CONFIG_STORE an intentionally bad value
        CONFIG_STORE = null;
        //See if a config file already exists
        File configFile = new File(configFileLocation);
        boolean newConfig = true;
        try{ newConfig = configFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }

        ErrorLogging.logError("Creating config file interface...");
        CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class)
            .configure(new Parameters().fileBased().setFile(configFile));

        //Try to import the config
        ErrorLogging.logError("Attempting to import config..."); 
        if(!newConfig)
        {
            try { CONFIG_STORE = CONFIG_BUILDER.getConfiguration(); }
            catch(Exception e) { ErrorLogging.logError(e); }
            finally 
            { 
                if(CONFIG_STORE == null) 
                    ErrorLogging.logError("CONFIG INIT ERROR!!! - Unsuccessful "+
                            "config initialisation. Please delete the current config "+
                            "file, then restart this program!"); 
                else
                {
                    ErrorLogging.logError("Config successfully imported!");
                    loadConfig(); 
                    ErrorLogging.logError("Configuration settings loaded!");
                }
            }
        }

        //If there isn't a config file yet (or rather, if we just made a new one)
        //save the default values to it
        else
        {
            ErrorLogging.logError("Unable to import config. Loading defaults...");
            boolean saveSuccessful = saveDefaultConfig();
            if(!saveSuccessful) ErrorLogging.logError("Save config failed!!!");
            else ErrorLogging.logError("Configuration settings set up!");
        }

        //Make necessary directories, if not already available
        ErrorLogging.logError("Creating image storage directories...");
        File imageLocation  = new File(imageSaveLocation);
        imageLocation.mkdir();
        File debugImageLocation  = new File(imageSaveLocation + "/debug");
        debugImageLocation.mkdir();
        File configImageLocation  = new File(imageSaveLocation + "/config");
        configImageLocation.mkdir();
        File outputFileDirectory = new File("outputData");
        outputFileDirectory.mkdir();

        //Create a new output file
        ErrorLogging.logError("Creating output file....");
        File outputFile = new File(outputSaveLocation);
        try{ outputFile.createNewFile(); }
        catch(Exception e){ ErrorLogging.logError(e); }

        //Autosave the config
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
            //Log failure information
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
        //ErrorLogging.logError("DEBUG: getValue - return value: " + cameraName 
        //                      + "/" + property.getConfig() + " = " + output);
        return output;
    }

    /**
     * Called to force early calling of the static block
     */
    public static void init() {}

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
        //Get set of camera names
        boolean output = false;
        Set<String> cameraNames = OpenCVFacade.getCameraNames();

        //Set the config builder to a file-based, INI configuration, 
        //with the given filename
        CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class)
                             .configure(new Parameters().fileBased()
                             .setFile(new File(filename)));

        //Set CONFIG_STORE to the Configuration created by the Builder 
        try { CONFIG_STORE = CONFIG_BUILDER.getConfiguration(); }
        catch(Exception e) { ErrorLogging.logError(e); }

        //If the save default fails, warn the user that something is wrong
        finally 
        { 
            if(CONFIG_STORE == null) 
            {
                ErrorLogging.logError("CONFIG INIT ERROR!!! - Unsuccessful config initialisation. Camera commands will fail!"); 
                ErrorLogging.logError("CONFIG INIT ERROR!!! - Attempted file save point: "+ filename);
            }
        }

        //Iterate over every camera
        //  Create a map of the default values
        //  Save the default values to the CONFIG_STORE
        //  save the map to the main config map
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
                CONFIG_STORE.setProperty(propertyName,propertyValue);
            }
            configMap.put(camera,cameraConfig);
        }

        //Save out to the file
        try
        { 
            CONFIG_BUILDER.save(); 
            output = true;
        }
        catch(Exception e){ ErrorLogging.logError(e); }

        return output;
    }

    /**
     * Save current config to the default file location.
     *
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveDefaultConfig() 
    { return saveDefaultConfig(configFileLocation); }

    /**
     * Save current config to a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveCurrentConfig(String filename)
    {
        boolean output = false;

        //Get a list of all cameras
        List<String> activeCameras = new ArrayList<>(OpenCVFacade.getCameraNames());

        //For every available camera
        //  get every current property value, save it to the CONFIG_STORE
        for(String camera : activeCameras)
        {
            for(ConfigProperties property : ConfigProperties.values())
            {
                String propertyName = camera + "." + property.getConfig();
                String propertyValue = configMap.get(camera).get(property).toString();
                CONFIG_STORE.setProperty(propertyName,propertyValue);
            }
        }

        //Save to the file
        try
        { 
            CONFIG_BUILDER.save(); 
            output = true;
        }
        catch(Exception e) { ErrorLogging.logError(e); }

        return output;
    }

    /**
     * Save current config to the default file location.
     *
     * @return true if saved successfully, otherwise false
     */
    public static boolean saveCurrentConfig() 
    { return saveCurrentConfig(configFileLocation); }

    /** 
     * Load config from a user-defined file location.
     *
     * @param filename  Name and location of the config file (typically, config.properties)
     * @return true if loaded successfully, otherwise false
     */
    public static boolean loadConfig(String filename)
    {
        //Check if the current configMap is empty
        boolean emptyMap = configMap.keySet().size() == 0;
        boolean output = false;

        //If the config file we're trying to load from doesn't exist, failover to saving
        //the default values to a new file with that name
        File file = new File(filename);
        try{ if(file.createNewFile()) return saveDefaultConfig(); }
        catch(Exception e)
        {
            ErrorLogging.logError(e);
            return saveDefaultConfig(filename);
        }

        //At this point, the file should exist
        //Get a list of camera names
        List<String> cameraNames = new ArrayList<>(OpenCVFacade.getCameraNames());
        if(Files.isRegularFile(Path.of(file.toURI())))
        {
            //Import the config file into a Java object
            try
            { 
                CONFIG_BUILDER = new FileBasedConfigurationBuilder<>(INIConfiguration.class)
                    .configure(new Parameters().fileBased().setFile(file));
                CONFIG_STORE = CONFIG_BUILDER.getConfiguration();
            }
            catch(Exception e){ ErrorLogging.logError(e); }

            //Iterate over the imported object, saving the file's config values to the map
            Set<String> configSections = CONFIG_STORE.getSections();
            for(String sectionName : configSections)
            {
                Map<ConfigProperties,Double> savedSection = new HashMap<>();
                String subSectionPrefix = "";
                for(String cameraName : cameraNames)
                {
                    if(sectionName.equals(cameraName))
                    {
                        subSectionPrefix = cameraName;
                        break;
                    }
                }

                //If an imported section fails, fallback to saving the default values to 
                //the given location
                if(subSectionPrefix.equals("")) 
                {
                    ErrorLogging.logError("CONFIG LOAD ERROR!!! - Failed import from file. Setting default config.");
                    return saveDefaultConfig(filename);
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

        //If something broke, complain
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
