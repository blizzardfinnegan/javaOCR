package org.baxter.disco.ocr;

//Static imports for OpenCV
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.opencv.global.opencv_highgui.selectROI;
import static org.bytedeco.opencv.global.opencv_core.bitwise_and;

//JavaCV imports
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

//OpenCV imports
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;

//Standard imports
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Facade for the OpenCV package.
 * Performs image capture, as well as image manipulation.
 *  
 * @author Blizzard Finnegan
 * @version 3.0.0, 10 Mar. 2023
 */
public class OpenCVFacade
{
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

    /**
     * Name of custom-created symlink for cameras.
     * This configuration must be done manually on initial install. 
     */
    private static final String CAMERA_FILE_PREFIX = "video-cam-";

    //Initial Camera creation
    static
    {
        ErrorLogging.logError("Initialising cameras...");
        File devDirectory = new File("/dev");
        for(File cameraFile : devDirectory.listFiles(
                    (file) -> { return file.getName().contains(CAMERA_FILE_PREFIX); }))
        {
            String cameraName = cameraFile.getName().
                                substring(CAMERA_FILE_PREFIX.length());
            newCamera(cameraName, cameraFile.getAbsolutePath());
        }
    }

    /**
     * Default camera creator function.
     * Creates a camera, and adds it to cameraMap.
     * Uses values in constants, listed previous.
     *
     * @param name      Name of the new camera
     * @param location  Location of the new camera
     */
    private static void newCamera(String name, String location)
    {
        newCamera(name, location, IMG_WIDTH, IMG_HEIGHT);
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
     */
    private static void newCamera(String name, String location, int width, int height)
    {
        newCamera(name, location, width, height, CAMERA_CODEC);
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
     */
    private static void newCamera(String name, String location, int width, int height, String codec)
    {
        ErrorLogging.logError("Initialising camera : " + name + "...");
        File cameraLocation = new File(location);
        if (cameraLocation.exists())
        {
            FrameGrabber camera = new OpenCVFrameGrabber(location);
            try{ camera.start(); }
            catch(Exception e) 
            { 
                ErrorLogging.logError(e); 
                ErrorLogging.logError("CAMERA INIT ERROR!!! - Camera failed to initialise. Use of camera " + name + " will fail.");
                return;
            }
            camera.setFormat(codec);
            camera.setImageWidth(width);
            camera.setImageHeight(height);
            cameraMap.put(name, camera);
        }
        else
        {
            ErrorLogging.logError("CAMERA INIT ERROR!!! - Illegal camera location.");
        }
    }

    /**
     * Getter for all camera names.
     *
     * @return List of available Webcam names.
     */
    public static Set<String> getCameraNames()
    { return cameraMap.keySet(); }


    /** 
     * Wrapper function for native "take picture" function.
     * Image is immediately converted to greyscale to improve RAM footprint.
     *
     * @param cameraName    Name of the camera to take a picture with.
     *
     * @return              null if camera doesn't exist, or if capture fails;
     *                      otherwise, Frame of the taken image
     */
    private static Mat takePicture(String cameraName)
    {
        Mat output = null;
        Frame temp = null;

        if(getCameraNames().contains(cameraName))
        {
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
     * Show current processed image to the CLI user.
     *
     * @param cameraName    The name of the camera to be previewed
     *
     * @return File of the image being shown
     */
    public static File showImage(String cameraName)
    {
        File imageLocation = completeProcess(cameraName,ConfigFacade.getImgSaveLocation() + "/config");
        if(imageLocation == null) return null;
        Frame outputImage = MAT_CONVERTER.convert(imread(imageLocation.getAbsolutePath()));
        String canvasTitle = "Camera " + cameraName + " Preview";
        final CanvasFrame canvas = new CanvasFrame(canvasTitle);
        canvas.showImage(outputImage);
        return imageLocation;
    }

    ///**
    // * Show current processed image to the GUI user.
    // *
    // * @param cameraName    The name of the camera to be previewed
    // *
    // * @return The {@link CanvasFrame} that is being opened. This is returned so it can be closed by the program.
    // */
    //private static String showImage(String cameraName, Object object)
    //{
    //    File imageLocation = completeProcess(cameraName,ConfigFacade.getImgSaveLocation() + "/config");
    //    return imageLocation.getPath();
    //}

    /** 
     * Take multiple pictures in quick succession. 
     *
     * @param cameraName    Name of the camera to take a picture with.
     * @param frameCount    The number of images to take.
     *
     * @return List of Frames taken from the camera. List is in order
     */
    private static List<Mat> takeBurst(String cameraName, int frameCount)
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
     * Set crop size and location by GUI means.
     *
     * @param cameraName    The name of the camera being configured
     */
    public static void setCrop(String cameraName)
    {
        Mat uncroppedImage = takePicture(cameraName);
        Rect roi = selectROI("Pick Crop Location", uncroppedImage);
        if(roi.x() == 0 && roi.y() == 0 && roi.width() == 0 && roi.height() == 0)
        {
            ErrorLogging.logError("Crop error! - Invalid crop selection.");
            ErrorLogging.logError("If the crop region did not have a box indicating is location, please restart the program.");
            ConfigFacade.setValue(cameraName,ConfigProperties.CROP_X,ConfigProperties.CROP_X.getDefaultValue());
            ConfigFacade.setValue(cameraName,ConfigProperties.CROP_Y,ConfigProperties.CROP_Y.getDefaultValue());
            ConfigFacade.setValue(cameraName,ConfigProperties.CROP_W,ConfigProperties.CROP_W.getDefaultValue());
            ConfigFacade.setValue(cameraName,ConfigProperties.CROP_H,ConfigProperties.CROP_H.getDefaultValue());
            return;
        }
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP_X, roi.x());
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP_Y, roi.y());
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP_W, roi.width());
        ConfigFacade.setValue(cameraName,ConfigProperties.CROP_H, roi.height());
    }

    /**
     * Crop a given image, based on dimensions in the configuration.
     *
     * @param image         Frame taken from the camera
     * @param cameraName    Name of the camera the frame is from
     */
    private static Mat crop(Mat image, String cameraName)
    {
        int x = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_X);
        int y = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_Y);
        int width = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_W);
        int height = (int)ConfigFacade.getValue(cameraName,ConfigProperties.CROP_H);
        Rect roi = new Rect(x,y,width,height);
        return crop(image, roi,cameraName);
    }

    /** 
     * Crop the given image, based on dimensions defined in a {@link Rect}
     *
     * @param image         Frame taken from the camera
     * @param roi           The region of interest to crop the image to
     *
     * @return Frame of the cropped image
     */
    private static Mat crop(Mat image, Rect roi, String cameraName)
    {
        Mat output = image.apply(roi).clone();
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
    private static Mat thresholdImage(Mat image,String cameraName)
    {
        Mat output = image;
        Mat in = image;
        double thresholdValue = ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD_VALUE);
        threshold(in,output,thresholdValue,255,THRESH_BINARY);
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
    private static File saveImage(Mat image, String fileLocation, String cameraName)
    {
        File output = null;
        IplImage temp = MAT_CONVERTER.convertToIplImage(MAT_CONVERTER.convert(image));
        fileLocation = fileLocation + "/" + ErrorLogging.fileDatetime.format(LocalDateTime.now()) + "-" + cameraName + ".png";
        cvSaveImage(fileLocation,temp);
        output = new File(fileLocation);
        return output;
    }

    /**
     * Compose several images together.
     * This will also perform thresholding, and cropping,
     * based on boolean toggles. Crop information is collected 
     * from {@link ConfigFacade}.
     *
     * @param images        List of images to be composed
     * @param threshold     Whether to put the image through a binary threshold
     * @param crop          Whether to crop the image
     *
     * @return A single image, found by boolean AND-ing together all parsed images.
     */
    private static Mat compose(List<Mat> images, boolean threshold, 
                                boolean crop, String cameraName)
    {
        ErrorLogging.logError("DEBUG: Attempting to compose " + images.size() + " images...");
        Mat output = null;
        int iterationCount = 1;
        for(Mat image : images)
        { 
            Mat processedImage = image.clone();
            image.copyTo(processedImage);
            if(crop)        processedImage = crop(processedImage,cameraName);
            if(threshold)   processedImage = thresholdImage(processedImage,cameraName);

            if(iterationCount == 1) output = processedImage.clone();

            bitwise_and((iterationCount == 1 ? processedImage : output),processedImage, output);

            iterationCount++;
        }

        if(output != null)  ErrorLogging.logError("DEBUG: Compositing successful!");
        else                ErrorLogging.logError("ERROR: Final output image is null!");
        return output;
    }

    /**
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
        output = saveImage(finalImage, saveLocation,cameraName);
        return output;
    }

    /**
     * Processes image from defined camera, using the config defaults.
     * Assumes you want to crop and threshold.
     *
     * @param cameraName        Name of the camera to take a picture from.
     * @param saveLocation      Name of the outgoing file
     *
     * @return null if any error occurs; otherwise File of output image
     */
    public static File completeProcess(String cameraName, String saveLocation)
    {
        File output = null;

        if(!getCameraNames().contains(cameraName))
        {
            ErrorLogging.logError("OPENCV ERROR!!! - Invalid camera name.");
            return output;
        }

        int compositeFrames = (int)ConfigFacade.getValue(cameraName,ConfigProperties.COMPOSITE_FRAMES);

        boolean threshold = (ConfigFacade.getValue(cameraName,ConfigProperties.THRESHOLD)   != 0.0);
        boolean crop =      (ConfigFacade.getValue(cameraName,ConfigProperties.CROP)        != 0.0);

        output = completeProcess(cameraName,crop,threshold,compositeFrames,saveLocation);

        if(output == null) ErrorLogging.logError("OPENCV ERROR!!!: Final processed image is null!");
        return output;
    }

    /**
     * Process an image from a defined camera, using config defaults 
     * and saving to [defaultImageLocation]/config/
     *
     * @param cameraName        Name of the camera to take a picture from.
     *
     * @return null if any error occurs; otherwise File of output image
     */
    public static File completeProcess(String cameraName)
    { return completeProcess(cameraName,ConfigFacade.getImgSaveLocation()); }
}
