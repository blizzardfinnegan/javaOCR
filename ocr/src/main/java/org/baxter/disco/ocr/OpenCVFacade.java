package org.baxter.disco.ocr;

import java.util.Map;
import java.util.Set;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_core.*;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import java.awt.image.BufferedImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Facade for the OpenCV package.
 * Performs image capture, as well as image manipulation.
 * Final product should only have one function publicly available;
 * takeImages().
 *
 * @author Blizzard Finnegan
 * @version 19 Jan. 2023
 */
public class OpenCVFacade
{
    /**
     * Storage of all cameras as Map.
     * To get available camera names, getKeys.
     */
    private static final Map<String,FrameGrabber> cameraMap = new HashMap<>();

    /**
     *
     */
    private static final Java2DFrameConverter BUFF_CONVERTER = new Java2DFrameConverter();
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

    static
    {
        //Pis should already be configured to create this symlink.
        newCamera("left", "/dev/video-cam1");
        newCamera("right","/dev/video-cam2");
        gammaCalibrate();
    }

    /**
     * Camera creator function.
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
     * Camera creator function.
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
     * Camera creator function.
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
        camera.setFormat(codec);
        camera.setImageWidth(width);
        camera.setImageHeight(height);
        FrameGrabber oldCamera = cameraMap.putIfAbsent(name, camera);
        if(!oldCamera.equals(null)) output = false;
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
     * Modify a camera's Gamma value, based on config data.
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
     * Modify a camera's Gamma value, based on a given value.
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
            ErrorLogging.logError("CAMERA ERROR!!! - Illegal camera name.");
            return false;
        }
    }

    /**
     * Modify the gamma of a single image.
     * !!This should only be done to assist in setting config values.!!
     *
     * @param image     Image to have its gamma adjusted
     * @param gamma     Gamma value to set 
     *
     * @return Frame of the modified image
     */
    public static Frame gammaAdjust(Frame image, double gamma)
    {
        Java2DFrameConverter.applyGamma(image,gamma);
        return image;
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
    public static Frame takePicture(String cameraName)
    {
        Frame output = null;
        Frame temp = null;

        if(getCameraNames().contains(cameraName))
        {
            try{ temp = cameraMap.get(cameraName).grab(); }
            catch(Exception e) { ErrorLogging.logError(e); }
        }

        //Convert to grayscale
        Mat in = MAT_CONVERTER.convertToMat(temp);
        Mat out = MAT_CONVERTER.convertToMat(temp);
        cvtColor(in,out,CV_BGR2GRAY);
        output = MAT_CONVERTER.convert(out);

        return output;
    }

    /**
     * 
     * @param cameraName    Name of the camera to take a picture with.
     * @param frameCount    The number of images to take.
     *
     * @return List of Frames taken from the camera. List is in order
     */
    public static List<Frame> takeBurst(String cameraName, int frameCount)
    {
        List<Frame> output = null;
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

    /** TODO: Implement Overloads
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
    public static Frame crop(Frame image, int x, int y, int width, int height)
    {
        Frame output = null;
        BufferedImage bufImage = BUFF_CONVERTER.getBufferedImage(image);
        bufImage.getSubimage(x,y,width,height);
        output = BUFF_CONVERTER.getFrame(bufImage);
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
    public static Frame thresholdImage(Frame image)
    {
        Frame output = null;
        Mat in = MAT_CONVERTER.convertToMat(image);
        Mat out = MAT_CONVERTER.convertToMat(image);
        threshold(in,out,127,255,THRESH_BINARY);
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
    public static File saveImage(Frame image, String fileLocation)
    {
        File output = null;
        IplImage temp = MAT_CONVERTER.convertToIplImage(image);
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
     * @param gammaAdjust   Whether to adjust the image's gamma
     * @param gamma         Gamma value to be set on the camera
     * @param crop          Whether to crop the image
     * @param x             X-coordinate of the top-left of the cropped portion of the image.
     * @param y             y-coordinate of the top-left of the cropped portion of the image.
     * @param width         width of the the cropped portion of the image.
     * @param height        height of the the cropped portion of the image.
     *
     * @return A single image, found by boolean AND-ing together all parsed images.
     */
    public static Frame compose(List<Frame> images, boolean threshold, 
                                boolean gammaAdjust, double gamma, boolean crop, 
                                int x, int y, int width, int height)
    {
        Frame output = null;
        for(Frame image : images)
        {
            if(gammaAdjust) gammaAdjust(image,gamma);
            if(crop) crop(image,x,y,width,height);
            if(threshold) thresholdImage(image);
        }

        //Composite images
        if(images.size() > 1)
        {
            Mat matOut = MAT_CONVERTER.convertToMat(images.get(0));
            for(Frame image : images)
            {
                Mat temp = MAT_CONVERTER.convertToMat(image);
                bitwise_and(matOut,temp,matOut);
            }
            output = MAT_CONVERTER.convert(matOut);
        }
        else
        {
            output = images.get(0);
        }
        return output;
    }

    /**
     * Processes image from defined camera, using the config defaults.
     *
     * @param cameraName        Name of the camera to take a picture from.
     * @param crop              Whether to crop the image
     * @param threshold         Whether to threshold the image
     * @param gammaAdjust       Whether to adjust the gamma of the image
     * @param compositeFrames   Number of frames to composite together
     * @param saveLocation      Name of the outgoing file
     *
     * @return null if any error occurs; otherwise File of output image
     */
    public static File completeProcess(String cameraName, boolean crop, 
                                       boolean threshold, boolean gammaAdjust, 
                                       int compositeFrames, String saveLocation)
    {
        File output = null;
        if(!getCameraNames().contains(cameraName))
        {
            ErrorLogging.logError("OPENCV ERROR!!! - Invalid camera name.");
            return output;
        }
        List<Frame> imageList = takeBurst(cameraName, compositeFrames);
        double gamma = ConfigFacade.getValue(cameraName,ConfigProperties.GAMMA);
        int x = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X);
        int y = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_Y);
        int width = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_W);
        int height = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_H);
        Frame finalImage = compose(imageList, threshold, gammaAdjust, gamma, 
                                   crop, x, y, width, height);
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
        return completeProcess(cameraName,true,true,false,compositeFrames,saveLocation);
    }

    /**
     * Collect images from all cameras and save them, using the config defaults.
     *
     * @return List of Files, as defined by {@code #completeProcess(String, String)}
     */
    public static List<File> iteration()
    {
        List<File> output = new ArrayList<>();
        for(String cameraName : getCameraNames())
        {
            output.add(completeProcess(cameraName, ConfigFacade.getImgSaveLocation()));
        }
        return output;
    }
}
