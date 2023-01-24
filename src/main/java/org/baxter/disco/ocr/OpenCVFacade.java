package org.baxter.disco.ocr;

import java.util.Map;
import java.util.Set;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Facade for the OpenCV package.
 * Performs image capture, as well as image manipulation.
 *
 * @author Blizzard Finnegan
 * @version 0.0.2, 24 Jan. 2023
 */
public class OpenCVFacade
{
    //Local variable instantiation
    /**
     * Storage of all cameras as Map.
     * To get available camera names, getKeys.
     */
    private static final Map<String,FrameGrabber> cameraMap = new HashMap<>();

    /**
     * Object used to convert between Mats and Frames
     */
    private static final OpenCVFrameConverter.ToMat MAT_CONVERTER = new OpenCVFrameConverter.ToMat();

    /**
     * Width of the image created by the camera.
     * !!See camera documentation before modifying!!
     */
    private static final int IMG_WIDTH = 800;
    /**
     * Height of the image created by the camera.
     * !!See camera documentation before modifying!!
     */
    private static final int IMG_HEIGHT = 600;
    /**
     * FourCC code of the image created by the camera.
     * !!See camera documentation before modifying!!
     */
    private static final String CAMERA_CODEC = "mjpg";

    //Initial Camera creation
    static
    {
        //Pis should already be configured to create this symlink.
        newCamera("left", "/dev/video-cam1");
        newCamera("right","/dev/video-cam2");
    }

    /**
     * Default camera creator function.
     * Creates a camera, and adds it to cameraMap.
     * Uses values in constants, listed previous.
     *
     * @param name      Name of the new camera
     * @param location  Location of the new camera
     *
     * @return false if camera already exists, or if camera does not exist.
     *         Otherwise, returns true.
     */
    private static boolean newCamera(String name, String location)
    {
        return newCamera(name, location, IMG_WIDTH, IMG_HEIGHT);
    }

    /**
     * Camera creator function, with custom width and height.
     * Creates a camera, and adds it to cameraMap.
     * Defaults to {@link #CAMERA_CODEC} definition.
     *
     * @param name      Name of the new camera
     * @param location  Location of the new camera
     * @param width     Width of the camera's image, in pixels.
     * @param height    height of the camera's image, in pixels.
     *
     * @return false if camera already exists, or if camera does not exist.
     *         Otherwise, returns true.
     */
    private static boolean newCamera(String name, String location, int width, int height)
    {
        return newCamera(name, location, width, height, CAMERA_CODEC);
    }

    /**
     * Camera creator function, with custom width, height, and codec.
     * Creates a camera, and adds it to cameraMap.
     *
     * @param name      Name of the new camera
     * @param location  Location of the new camera
     * @param width     Width of the camera's image, in pixels.
     * @param height    height of the camera's image, in pixels.
     * @param codec     Codec to use for the new camera.
     *
     * @return false if camera already exists, or if camera does not exist.
     *         Otherwise, returns true.
     */
    private static boolean newCamera(String name, String location, int width, int height, String codec)
    {
        boolean output = true;
        FrameGrabber camera = new OpenCVFrameGrabber(location);
        try{ camera.start(); }
        catch(Exception e) { ErrorLogging.logError(e); }
        camera.setFormat(codec);
        camera.setImageWidth(width);
        camera.setImageHeight(height);
        FrameGrabber oldCamera = cameraMap.putIfAbsent(name, camera);

        //debug removal of below statement
        if(false) 
        {
            ErrorLogging.logError("Camera Initialisation Error!!! - Illegal camera location.");
            output = false;
        }
        return output;
    }

    /**
     * Getter for all camera names.
     *
     * @return List of available Webcam names.
     */
    public static Set<String> getCameraNames()
    {
        return cameraMap.keySet();
    }

    /**
     * Modify all cameras Gamma value, based on config data.
     */
    private static void gammaCalibrate()
    {
        for(String cameraName : getCameraNames())
        {
            gammaCalibrate(cameraName);
        }
    }

    /**
     * Modify a single camera's Gamma value, based on config data.
     *
     * @param cameraName    The name of the camera you would like to modify
     *
     * @return false if illegal camera name, otherwise true
     */
    public static boolean gammaCalibrate(String cameraName)
    {
        double gamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
        return gammaCalibrate(cameraName,gamma);
    }

    /**
     * Modify a single camera's Gamma value, based on a given value.
     *
     * @param cameraName    The name of the camera you would like to modify
     * @param gamma         The new gamma value for the camera
     *
     * @return false if illegal camera name, otherwise true
     */
    public static boolean gammaCalibrate(String cameraName, double gamma)
    {
        if(getCameraNames().contains(cameraName))
        {
            cameraMap.get(cameraName).setGamma(gamma);
            return true;
        }
        else
        {
            ErrorLogging.logError("CAMERA ERROR!!! - Given camera name not initialised.");
            return false;
        }
    }

    /** 
     * Wrapper function for native "take picture" function.
     * Image is immediately converted to greyscale to improve RAM footprint.
     *
     * @param cameraName    Name of the camera to take a picture with.
     *
     * @return              null if camera doesn't exist, or if capture fails;
     *                      otherwise, Frame of the taken image
     */
    public static Mat takePicture(String cameraName)
    {
        Mat output = null;
        Frame temp = null;

        if(getCameraNames().contains(cameraName))
        {
            double configGamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
            if(configGamma != cameraMap.get(cameraName).getGamma())
            {
                gammaCalibrate(cameraName, configGamma);
            }
            try{ temp = cameraMap.get(cameraName).grab(); }
            catch(Exception e) { ErrorLogging.logError(e); }
        }

        //Convert to grayscale
        Mat in = MAT_CONVERTER.convertToMat(temp);
        output = MAT_CONVERTER.convertToMat(temp);
        cvtColor(in,output,CV_BGR2GRAY);

        return output;
    }

    /**
     * Show current processed image to user.
     *
     * @param cameraName    The name of the camera to be previewed
     *
     * @return The {@link CanvasFrame} that is being opened. This is returned so it can be closed by the program.
     */
    public static CanvasFrame showImage(String cameraName)
    {
        Mat image = completeProcess(cameraName);
        Frame outputImage = MAT_CONVERTER.convert(image);
        String canvasTitle = "Camera " + cameraName + " Preview";
        CanvasFrame canvas = new CanvasFrame(canvasTitle);
        canvas.showImage(outputImage);
        return canvas;
    }

    /** 
     * Take multiple pictures in quick succession. 
     *
     * @param cameraName    Name of the camera to take a picture with.
     * @param frameCount    The number of images to take.
     *
     * @return List of Frames taken from the camera. List is in order
     */
    public static List<Mat> takeBurst(String cameraName, int frameCount)
    {
        List<Mat> output = null;
        if(getCameraNames().contains(cameraName))
        {
            output = new LinkedList<>();
            for(int i = 0; i < frameCount; i++)
            {
                output.add(takePicture(cameraName));
            }
        }
        return output;
    }

    /** 
     * Crop the given image to the dimensions in the configuration.
     *
     * @param image         Frame taken from the camera.
     * @param cameraName    Name of the camera taking the picture
     *
     * @return Frame of the cropped image
     */
    public static Mat crop(Mat image, String cameraName)
    {
        Mat output = null;
        int x = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X);
        int y = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_Y);
        int width = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_W);
        int height = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_H);
        var cropRectangle = new Rect(x,y,width,height);
        output = new Mat(image,cropRectangle);
        return output;
    }

    /**
     * Crop the given image to the given dimensions.
     *
     * @param image     Frame taken from the camera.
     * @param x         X-coordinate of the top-left of the cropped portion of the image.
     * @param y         y-coordinate of the top-left of the cropped portion of the image.
     * @param width     width of the the cropped portion of the image.
     * @param height    height of the the cropped portion of the image.
     *
     * @return Frame of the cropped image
     */
    public static Mat crop(Mat image, int x, int y, int width, int height)
    {
        Mat output = null;
        var cropRectangle = new Rect(x,y,width,height);
        output = new Mat(image,cropRectangle);
        return output;
    }

    /**
     * Put the given image through a binary threshold.
     * This reduces the image from greyscale to only pure white and black pixels.
     *
     * @param image     Frame taken from the camera.
     *
     * @return Frame of the thresholded image
     */
    public static Mat thresholdImage(Mat image)
    {
        Mat output = image;
        Mat in = image;
        threshold(in,output,127,255,THRESH_BINARY);
        return output;
    }

    /**
     * Save input Frame at the location given.
     *
     * @param image         Image to be saved.
     * @param fileLocation  Where to save the image.
     *
     * @return File if save was successful, otherwise null
     */
    public static File saveImage(Mat image, String fileLocation)
    {
        File output = null;
        IplImage temp = MAT_CONVERTER.convertToIplImage(MAT_CONVERTER.convert(image));
        cvSaveImage(fileLocation,temp);
        output = new File(fileLocation);
        return output;
    }

    /**
     * Compose several images together.
     * This will also perform thresholding, gamma adjustment, and cropping,
     * based on boolean toggles.
     *
     * @param images        List of images to be composed
     * @param threshold     Whether to put the image through a binary threshold
     * @param crop          Whether to crop the image
     * @param x             X-coordinate of the top-left of the cropped portion of the image.
     * @param y             y-coordinate of the top-left of the cropped portion of the image.
     * @param width         width of the the cropped portion of the image.
     * @param height        height of the the cropped portion of the image.
     *
     * @return A single image, found by boolean AND-ing together all parsed images.
     */
    public static Mat compose(List<Mat> images, boolean threshold, 
                                boolean crop, String cameraName)
    {
        Mat output = null;
        for(Mat image : images)
        {
            if(crop) crop(image,cameraName);
            if(threshold) thresholdImage(image);
        }

        //Composite images
        if(images.size() > 1)
        {
            output = images.get(0);
            for(Mat image : images)
            {
                bitwise_and(output,image,output);
            }
        }
        else
        {
            output = images.get(0);
        }
        return output;
    }

    /**TODO: More robust file output checking;
     * Processes image from defined camera, using the config defaults.
     *
     * 
     * @param cameraName        Name of the camera to take a picture from.
     * @param crop              Whether to crop the image
     * @param threshold         Whether to threshold the image
     * @param compositeFrames   Number of frames to composite together
     * @param saveLocation      Name of the outgoing file
     *
     * @return null if any error occurs; otherwise File of output image
     */
    public static File completeProcess(String cameraName, boolean crop, 
                                       boolean threshold, int compositeFrames, 
                                       String saveLocation)
    {
        File output = null;
        if(!getCameraNames().contains(cameraName))
        {
            ErrorLogging.logError("OPENCV ERROR!!! - Invalid camera name.");
            return output;
        }
        List<Mat> imageList = takeBurst(cameraName, compositeFrames);
        Mat finalImage = compose(imageList, threshold, crop, cameraName);
        output = saveImage(finalImage, saveLocation);
        return output;
    }

    /**
     * Processes image from defined camera, using the config defaults.
     * Assumes you want to crop and threshold, but not gamma adjust the image.
     * (Gamma should be set on the camera by modifying the config.)
     *
     * @param cameraName        Name of the camera to take a picture from.
     * @param saveLocation      Name of the outgoing file
     *
     * @return null if any error occurs; otherwise File of output image
     */
    public static File completeProcess(String cameraName, String saveLocation)
    {
        int compositeFrames = (int)ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES);
        return completeProcess(cameraName,true,true,compositeFrames,saveLocation);
    }

    /**
     * Internal function to process image without saving to file.
     * Runs the same function as {@link #completeProcess(String, String)},
     * without file I/O, and ensuring to set the gamma for the camera according
     * to the config.
     *
     * @param cameraName        Name of the camera to take a picture from.
     *
     * @return null if any error occurs; otherwise File of output image
     */
    private static Mat completeProcess(String cameraName)
    {
        double configGamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
        double cameraGamma = cameraMap.get(cameraName).getGamma();
        if(configGamma != cameraGamma)
            gammaCalibrate(cameraName,configGamma);

        Mat output = null;
        int compositeFrames = (int)ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES);
        if(!getCameraNames().contains(cameraName))
        {
            ErrorLogging.logError("OPENCV ERROR!!! - Invalid camera name.");
            return output;
        }
        List<Mat> imageList = takeBurst(cameraName, compositeFrames);
        output = compose(imageList, true, true, cameraName);
        return output;
    }

    /**
     * Collect images from all cameras and save them, using the config defaults.
     * Configures the camera's gamma before running process.
     *
     * @return List of Files, as defined by {@code #completeProcess(String, String)}
     */
    public static List<File> singleIteration()
    {
        List<File> output = new ArrayList<>();
        for(String cameraName : getCameraNames())
        {
            double configGamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
            double cameraGamma = cameraMap.get(cameraName).getGamma();
            if(configGamma != cameraGamma)
                gammaCalibrate(cameraName,configGamma);

            output.add(completeProcess(cameraName, ConfigFacade.getImgSaveLocation()));
        }
        return output;
    }

    /**
     * Collect images from all cameras and save them, using the config defaults.
     * Does NOT configure camera's gamma before running process.
     *
     * @param filler    Not used for anything, just exists so we can use the same function name 
     *                  without setting the camera's gamma level on every iteration.
     *
     * @return List of Files, as defined by {@code #completeProcess(String, String)}
     */
    private static List<File> singleIteration(Object filler)
    {
        List<File> output = new ArrayList<>();
        for(String cameraName : getCameraNames())
        {
            double configGamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
            double cameraGamma = cameraMap.get(cameraName).getGamma();
            if(configGamma != cameraGamma)
                gammaCalibrate(cameraName,configGamma);

            output.add(completeProcess(cameraName, ConfigFacade.getImgSaveLocation()));
        }
        return output;
    }

    /**
     *
     */
    public static List<List<File>> multipleIterations(int iterationCount)
    {
        List<List<File>> output = new ArrayList<>();
        for(String cameraName : getCameraNames())
        {
            double configGamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
            double cameraGamma = cameraMap.get(cameraName).getGamma();
            if(configGamma != cameraGamma)
                gammaCalibrate(cameraName,configGamma);
        }

        for(int i = 0; i < iterationCount; i++)
        {
            output.add(singleIteration(new Object()));
        }

        return output;
    }
}
