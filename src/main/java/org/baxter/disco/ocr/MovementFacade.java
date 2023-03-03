package org.baxter.disco.ocr;

//Pi4J imports
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfigBuilder;
import com.pi4j.io.pwm.PwmType;

/**
 * Facade for all movement of the fixture.
 *
 * Uses Pi4J to communicate with GPIO pins.
 * Currently missing Run switch compatibility.
 *
 * @author Blizzard Finnegan
 * @version 2.4.0, 01 Mar. 2023
 */
public class MovementFacade
{
    private static boolean exit = false;
    /**
     * Constructor for MovementFacade.
     *
     * @param LOCK  A Lock object, used for interactions with
     *              the physical lock switch on the fixture.
     */
    //public MovementFacade(Lock LOCK)
    //{
    //    //ErrorLogging.logError("DEBUG: Starting lock thread...");
    //    runSwitchThread = new Thread(() -> 
    //            {
    //                boolean unlock = false;
    //                while(!exit)
    //                {
    //                    if(runSwitch.isOn())
    //                    {
    //                        ErrorLogging.logError("Run switch turned off!");
    //                        while(!LOCK.tryLock())
    //                        {}
    //                        unlock = true;
    //                    }
    //                    else
    //                    {
    //                        //ErrorLogging.logError("Run switch on!");
    //                        if(unlock) 
    //                        { LOCK.unlock(); unlock = false; }
    //                    }
    //                    //try{ Thread.sleep(100); } catch(Exception e) { ErrorLogging.logError(e); }
    //                }
    //            }, "Run switch monitor.");
    //    runSwitchThread.start();
    //    //ErrorLogging.logError("DEBUG: Lock thread started!");
    //}

    private static Thread runSwitchThread;

    /**
     * Conversion factor from cm/s to PWM frequency. 
     *
     * PWM to linear speed conversion:
     * Motor controller should be set to 6400 pulses / revolution
     *      (See motor controller documentation)
     * Fixture corkscrew has a lead of 3 revolutions / cm
     *      (Lead = thread pitch * # of thread starts)
     *
     * Frequency (Hz) = Speed (cm/s) * (pulses/rev) * (lead)
     */
    private static final int PWM_FREQ_CONVERT = 19200;

    /**
     * Max allowed frequency by current fixture design.
     */
    //private static int MAX_FREQUENCY = 175000;

    /**
     * Max allowed speed by current fixture design.
     * Motor appears to start acting erratically over 192kHz.
     */
    private static final double MAX_SPEED = 10.0;

    /**
     * Amount of buffer between the found absolute speed, and used speed.
     */
    private static final double SPEED_BUFFER = 0.2;

    /**
     * Minimum allowed speed of the fixture arm; also used for reset travels.
     */
    private static final double MIN_SPEED = 0.5;

    /**
     * minimum speed, represented as frequency
     */
    private static final int MIN_FREQUENCY = (int)(MIN_SPEED * PWM_FREQ_CONVERT);

    /**
     * Distance in cm the fixture needs to travel.
     *
     * Distance between limit switches: ~80cm.
     * Thickness of fixture arm: ~50cm.
     */
    private static final int TRAVEL_DIST = 30;

    private static final double STEP_1 = 2/3;
    private static final double STEP_2 = 5/6;

    /**
     * Speed of the fixture arm, in cm/s
     */
    private static double SPEED = MIN_SPEED;

    private static int FREQUENCY = (int)(SPEED * PWM_FREQ_CONVERT);

    /**
     * PWM Duty Cycle.
     * Does not affect motor speed; necessary for PWM setup.
     */
    private static final int DUTY_CYCLE = 50;

    //PWM Addresses
    //All addresses are in BCM format.

    /**
     * Output pin address for motor power control.
     */
    private static final int MOTOR_ENABLE_ADDR = 22;

    /**
     * Output pin address for motor direction control.
     */
    private static final int MOTOR_DIRECTION_ADDR = 27;

    /**
     * Output pin address for piston control.
     */
    private static final int PISTON_ADDR = 25;

    /**
     * PWM pin address.
     */
    private static final int PWM_PIN_ADDR = 12;

    /**
     * Input pin address for the run switch. 
     */
    private static final int RUN_SWITCH_ADDR = 10;

    /**
     * Input pin address for the upper limit switch.
     */
    private static final int UPPER_LIMIT_ADDR = 23;

    /**
     * Input pin address for the lower limit switch.
     */
    private static final int LOWER_LIMIT_ADDR = 24;

    /**
     * How many milliseconds to wait before polling the GPIO
     */
    private static final int POLL_WAIT = 10;

    /**
     * Multiply the time-out value by this value to get the number of polls to make.
     */
    private static final int TIME_CONVERSION = 1000 / POLL_WAIT;

    //Pi GPIO pin objects
    
    /**
     * Upper limit switch object.
     * 
     * Status: High; Upper limit switch has been reached.
     * Status: Low; Upper limit switch has not been reached.
     */
    private static DigitalInput upperLimit;

    /**
     * Lower limit switch object.
     * 
     * Status: High; Lower limit switch has been reached.
     * Status: Low; Lower limit switch has not been reached.
     */
    private static DigitalInput lowerLimit;

    /**
     * Lower limit switch object.
     *
     * Status: High; Test may continue.
     * Status: Low; Test must stop immediately.
     */
    private static DigitalInput runSwitch;

    /**
     * Motor power object.
     *
     * Status: High; Motor starts moving, in the direction defined by {@link #motorDirection}.
     * Status: Low; Motor stops moving.
     */
    private static DigitalOutput motorEnable;

    /**
     * Defines the movement direction for the motor enabled by {@link #motorEnable}.
     *
     * Status: High; Motor will move upwards.
     * Status: Low; Motor will move downwards.
     */
    private static DigitalOutput motorDirection;

    /**
     * Piston control pin object.
     *
     * Status: High; Piston is extended.
     * Status: Low; Piston is retracted.
     */
    private static DigitalOutput pistonActivate;

    /**
     * PWM pin object.
     * Never used, but needs to be initialised to get GPIO to work properly.
     */
    private static Pwm pwm;

    /**
     * {@link Pi4J} API interaction object.
     */
    private static Context pi4j;

    static
    {
        //ErrorLogging.logError("DEBUG: Starting lock thread...");
        runSwitchThread = new Thread(() -> 
                {
                    boolean unlock = false;
                    while(!exit)
                    {
                        if(runSwitch.isOn())
                        {
                            ErrorLogging.logError("Run switch turned off!");
                            while(!Cli.LOCK.tryLock())
                            {}
                            unlock = true;
                        }
                        else
                        {
                            //ErrorLogging.logError("Run switch on!");
                            if(unlock) 
                            { Cli.LOCK.unlock(); unlock = false; }
                        }
                        //try{ Thread.sleep(100); } catch(Exception e) { ErrorLogging.logError(e); }
                    }
                }, "Run switch monitor.");
        runSwitchThread.start();
        //ErrorLogging.logError("DEBUG: Lock thread started!");

        //Initialise Pi4J
        pi4j = Pi4J.newAutoContext();

        //Initialise input pins
        upperLimit = inputBuilder("upperLimit", "Upper Limit Switch", UPPER_LIMIT_ADDR);
        lowerLimit = inputBuilder("lowerLimit", "Lower Limit Switch", LOWER_LIMIT_ADDR);
        runSwitch  = inputBuilder("runSwitch" , "Run Switch"        , RUN_SWITCH_ADDR);

        //Initialise output pins
        motorEnable    = outputBuilder("motorEnable"   , "Motor Enable"   , MOTOR_ENABLE_ADDR);
        motorDirection = outputBuilder("motorDirection", "Motor Direction", MOTOR_DIRECTION_ADDR);
        pistonActivate = outputBuilder("piston"        , "Piston Activate", PISTON_ADDR);

        //Initialise PWM object. This object is never used, 
        //as the PWM signal is simply a clock for the motor.
        pwm = pwmBuilder("pwm","PWM Pin",PWM_PIN_ADDR);
        //pwm.on(DUTY_CYCLE, FREQUENCY);

    }

    /**
     * Builder function for PWM pins.
     *
     * @param id        ID of the new PWM pin.
     * @param name      Name of the new PWM pin.
     * @param address   BCM address of the PWM pin.
     * 
     * @return newly created PWM pin object.
     */
    private static Pwm pwmBuilder(String id, String name, int address)
    {
        PwmConfigBuilder configBuilder;
        switch (address)
        {
            //The following pins allow for hardware PWM support.
            case 12:
            case 13:
            case 18:
            case 19:
            case 40:
            case 41:
            case 45:
            case 52:
            case 53:
                configBuilder = Pwm.newConfigBuilder(pi4j)
                                   .id(id)
                                   .name(name)
                                   .address(address)
                                   .pwmType(PwmType.HARDWARE)
                                   .provider("pigpio-pwm")
                                   .initial(1)
                                   //On program close, turn off PWM.
                                   .shutdown(0);
                break;
            //Any pin not listed above must be software PWM controlled.
            default:
                configBuilder = Pwm.newConfigBuilder(pi4j)
                                   .id(id)
                                   .name(name)
                                   .address(address)
                                   .pwmType(PwmType.SOFTWARE)
                                   .provider("pigpio-pwm")
                                   .initial(1)
                                   //On program close, turn off PWM.
                                   .shutdown(0);
        }
        return pi4j.create(configBuilder);
    }

    /**
     * Builder function for DigitalInput pins. 
     *
     * @param id        ID of the new {@link DigitalInput} pin.
     * @param name      Name of the new {@link DigitalInput} pin.
     * @param address   BCM address of the {@link DigitalInput} pin.
     *
     * @return newly created {@link DigitalInput} object.
     */
    private static DigitalInput inputBuilder(String id, String name, int address)
    { 
        DigitalInputConfigBuilder configBuilder = DigitalInput.newConfigBuilder(pi4j)
                                                              .id(id)
                                                              //.name(name)
                                                              .address(address)
                                                              .pull(PullResistance.PULL_DOWN)
                                                              .debounce(3000L)
                                                              .provider("pigpio-digital-input");
        return pi4j.create(configBuilder);
    }

    /**
     * Builder function for DigitalOutput pins. 
     *
     * @param id        ID of the new {@link DigitalOutput} pin.
     * @param name      Name of the new {@link DigitalOutput} pin.
     * @param address   BCM address of the {@link DigitalOutput} pin.
     *
     * @return newly created {@link DigitalOutput} object
     */
    private static DigitalOutput outputBuilder(String id, String name, int address)
    {
        DigitalOutputConfigBuilder configBuilder = DigitalOutput.newConfigBuilder(pi4j)
                                                                .id(id)
                                                                .name(name)
                                                                .address(address)
                                                                .shutdown(DigitalState.LOW)
                                                                .initial(DigitalState.LOW)
                                                                .provider("pigpio-digital-output");
        return pi4j.create(configBuilder);

    }

    /**
     * Function used to locate the fixture's motor.
     */
    public static int resetArm()
    {
        int counter = 0;
        //ErrorLogging.logError("Setting minimum frequency of PWM...");
        //ErrorLogging.logError("Minimum frequency: " + MIN_FREQUENCY);
        pwm.on(DUTY_CYCLE, MIN_FREQUENCY);
        if(upperLimit.isHigh())
        {
            //ErrorLogging.logError("Motor at highest point! Lowering to reset.");
            motorDirection.low();
            //ErrorLogging.logError("Motor on!");
            motorEnable.on();
            try{ Thread.sleep(500); }
            catch (Exception e){ ErrorLogging.logError(e); }
            motorEnable.off();
            //ErrorLogging.logError("Motor off!");
        }
        //ErrorLogging.logError("Moving motor to highest point.");
        motorDirection.high();

        //ErrorLogging.logError("Motor on!");
        motorEnable.on();

        //ErrorLogging.logError("Is the upper limit switch reached? " + upperLimit.isHigh());
        while(!upperLimit.isHigh()) 
        { 
            try{ Thread.sleep(100); }
            catch (Exception e) { ErrorLogging.logError(e); }
            counter++;
        }
        motorEnable.off();
        //ErrorLogging.logError("Motor returned after " + counter + " polls.");
        return counter;
    }

    /**
     * Used to set the motor's max speed.
     */
    public static void calibrate()
    {
        ErrorLogging.logError("Initial Calibration reset.");
        resetArm();
        ErrorLogging.logError("Coarse calibrating...");
        SPEED = calib(1, MAX_SPEED, 1);
        ErrorLogging.logError("Coarse calibrating...");
        SPEED = calib(SPEED,(SPEED+1),0.1);
        ErrorLogging.logError("Speed set to " + (SPEED - SPEED_BUFFER));
        setSpeed(SPEED - SPEED_BUFFER);
    }

    /**
     * Find the max speed of the fixure between two points.
     *
     * @param start     Lowest speed to check 
     * @param max       Highest speed to check
     * @param iterate   How much to iterate by
     *
     * @return The largest safe value between start and max. 
     */
    private static double calib(double start, double max, double iterate)
    {
        ErrorLogging.logError("Calibrating. Iterating from " + start + " to " + max + " in " + iterate + " steps.");
        for(double i = start; i < max; i+=iterate)
        {
            //ErrorLogging.logError("Testing speed " + i + "...");
            if(!setSpeed(i))
            {
                //ErrorLogging.logError("Speed set unsuccessfully! returning " + MIN_SPEED + "...");
                return MIN_SPEED;
            }
            //ErrorLogging.logError("Motor travelling down.");
            motorDirection.low();
            //ErrorLogging.logError("Motor on!");
            motorEnable.on();
            for(int j = 0; j < 5; j++)
            {
                try{ Thread.sleep(100); }
                catch (Exception e){ ErrorLogging.logError(e); }
                if(lowerLimit.isHigh()) break;
            }
            motorEnable.off();
            //ErrorLogging.logError("Motor off!");
            if(upperLimit.isHigh())
            {
                ErrorLogging.logError("Motor faild to move! Returning " + (i - iterate));
                return i-iterate;
            }
            else
            {

                //ErrorLogging.logError("Motor moved at speed " + i + ". Checking for errors.");
                if(resetArm() < 10 && i > 1.0)
                {
                    ErrorLogging.logError("Motor failed to move! Returning " + (i - iterate));
                    return i - iterate;
                }
            }
        }
        return max-iterate;
    }

    /**
     * Safely set the speed of the motor and fixture.
     *
     * @return true if set successfully, else false
     */
    private static boolean setSpeed(double newSpeed)
    {
        boolean output;
        if(newSpeed < MIN_SPEED || newSpeed > MAX_SPEED) 
        {
            SPEED = MIN_SPEED;
            output = false;
        }
        else
        {
            SPEED = newSpeed;
            output = true;
        }
        SPEED = newSpeed;
        FREQUENCY = (int)(SPEED * PWM_FREQ_CONVERT);
        pwm.on(DUTY_CYCLE, FREQUENCY);
        return output;
    }

    /**
     * Internal function to send the fixture to a given limit switch.
     *
     * Detects if the limit switch is active before activating motor.
     *
     * Motor slows down after timeout is halfway through, to protect hardware.
     *
     * @param moveUp    Whether to send the fixture up or down. (True = up, False = down)
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    private static FinalState gotoLimit(boolean moveUp)
    {
        FinalState output = FinalState.FAILED;
        DigitalInput limitSense;
        if(moveUp)  
        {
            motorDirection.high();
            limitSense = upperLimit;
            ErrorLogging.logError("Sending fixture up...");
        }
        else        
        {
            motorDirection.low();
            limitSense = lowerLimit;
            ErrorLogging.logError("Sending fixture down...");
        }

        if(limitSense.isHigh()) return FinalState.SAFE;

        FREQUENCY = (int)(SPEED * TIME_CONVERSION);
        pwm.on(DUTY_CYCLE, FREQUENCY);
        int TRAVEL_TIME = (int)(TRAVEL_DIST / SPEED);
        int POLL_COUNT = TRAVEL_TIME * TIME_CONVERSION;
        int VEL_STEP_1 = (int)(STEP_1 * POLL_COUNT);
        int VEL_STEP_2 = (int)(STEP_2 * POLL_COUNT);
        //int mostlyThere = (int) (POLL_COUNT * slowFraction);
        //int slowerSpeed = (int) (FREQUENCY / speedReduceFactor);

        ErrorLogging.logError("Total Poll count: " + POLL_COUNT);
        ErrorLogging.logError("Transition 1: " + VEL_STEP_1);
        ErrorLogging.logError("Transition 2: " + VEL_STEP_2);
        ErrorLogging.logError("Travel time: " + TRAVEL_TIME);
        ErrorLogging.logError("Travel speed: " + SPEED);
        motorEnable.on();
        for(int i = 0; i < (POLL_COUNT);i++)
        {
            ErrorLogging.logError("Iteration " + i);
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(i >= VEL_STEP_1 && i < VEL_STEP_2)
            {
                output = FinalState.UNSAFE;
                ErrorLogging.logError("Slow down!");
                pwm.on(DUTY_CYCLE, FREQUENCY / 2);
            }
            else if(i >= VEL_STEP_2)
            {
                ErrorLogging.logError("Slow down more!");
                pwm.on(DUTY_CYCLE, FREQUENCY / 4);
                output = FinalState.SAFE;
            }
        }
        motorEnable.off();
        
        if(output == FinalState.FAILED) 
            ErrorLogging.logError("FIXTURE MOVEMENT ERROR! - Motor movement timed out!");
        pwm.on(DUTY_CYCLE, FREQUENCY);
        return output;
    }

    /**
     * Send the fixture to the lower limit switch.
     *
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goDown() { return gotoLimit(false); }

    /**
     * Send the fixture to the upper limit switch.
     *
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goUp() { return gotoLimit(true); }

    /**
     * Extends the piston for 1 second, pushing the button on the DUT.
     */
    public static void pressButton()
    {
        ErrorLogging.logError("DEBUG: Pressing button...");
        pistonActivate.on();
        try{ Thread.sleep(1000); } catch(Exception e) {ErrorLogging.logError(e);};
        ErrorLogging.logError("DEBUG: Releasing button...");
        pistonActivate.off();
    }

    /**
     * Closes connections to all GPIO pins.
     */
    public static void closeGPIO()
    {
        resetArm();
        if(runSwitchThread.isAlive())
        {
                exit = true;
                try{ Thread.sleep(500); } catch(Exception e){}
        }
        pi4j.shutdown();
    }

    /**
     * Function to move the fixture once for an iteration.
     *
     * @param prime     Whether or not to wake up the DUT
     */
    public static void iterationMovement(boolean prime)
    {
        goUp();
        if(prime) pressButton();
        goDown();
        try{ Thread.sleep(100); } catch(Exception e){ ErrorLogging.logError(e); }
        pressButton();
    }

    public enum FinalState
    { UNSAFE, SAFE, FAILED; }
}
