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
 * @version 3.0.0, 06 Mar. 2023
 */
public class MovementFacade
{
    /**
     * Boolean used to communicate with runSwitchThread to gracefully exit.
     */
    private static boolean exit = false;

    /**
     * Thread that watches the physical Run switch on the device so that 
     * fixture movement can be stopped.
     */
    private static Thread runSwitchThread;

    /**
     * Max allowed speed by current fixture design.
     * Motor appears to start acting erratically over 192kHz.
     */
    private static final int MAX_FREQUENCY = 192000;

    /**
     * Amount of buffer between the found absolute speed, and used speed.
     */
    private static final int SPEED_BUFFER = 5000;

    /**
     * Minimum allowed speed of the fixture arm; also used for reset travels.
     */
    private static final int MIN_FREQUENCY = 10000;

    /**
     * Fraction of the total travel time at speed to start slowing down.
     */
    private static final double SLOW_POLL_FACTOR = 3.0 / 4.0;

    /**
     * Amount to slow down the speed by.
     */
    private static final int SPEED_DOWN_FACTOR = 2;

    /**
     * Amount of distance to travel.
     * Measured in... seemingly arbitrary units? Not sure on the math here.
     * Set in {@link #findDistance()}
     */
    private static double TRAVEL_DIST;

    /**
     * Frequency fed to the PWM pin, which the motor controller converts into movement speed.
     */
    private static int FREQUENCY = MIN_FREQUENCY;

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
                        try{ Thread.sleep(100); } catch(Exception e) { ErrorLogging.logError(e); }
                    }
                }, "Run switch monitor.");
        runSwitchThread.start();

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

        //Initialise PWM object. 
        pwm = pwmBuilder("pwm","PWM Pin",PWM_PIN_ADDR);
        pwm.on(DUTY_CYCLE, FREQUENCY);

        //Find Distance and max speeds
        calibrate();
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
                                   .frequency(FREQUENCY)
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
                                   .frequency(FREQUENCY)
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
        ErrorLogging.logError("DEBUG: --------------------------------------");
        int counter;
        ErrorLogging.logError("DEBUG: Setting minimum frequency of PWM...");
        pwm.on(DUTY_CYCLE, MIN_FREQUENCY);
        if(upperLimit.isHigh())
        {
            ErrorLogging.logError("DEBUG: Motor at highest point! Lowering to reset.");
            motorDirection.low();
            ErrorLogging.logError("DEBUG: Motor offset on.");
            motorEnable.on();
            try{ Thread.sleep(500); }
            catch (Exception e){ ErrorLogging.logError(e); }
            motorEnable.off();
            ErrorLogging.logError("DEBUG: Motor offset off.");
        }
        ErrorLogging.logError("DEBUG: Moving motor to highest point.");
        motorDirection.high();

        ErrorLogging.logError("DEBUG: Motor return on.");
        motorEnable.on();

        ErrorLogging.logError("DEBUG: Is the upper limit switch reached? " + upperLimit.isHigh());
        for(counter = 0; counter < Integer.MAX_VALUE; counter++)
        { 
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(upperLimit.isOn()) 
            {
                try{ Thread.sleep(1); } catch(Exception e){ErrorLogging.logError(e); }
                if(upperLimit.isOn()) break;
            }
        }
        motorEnable.off();
        ErrorLogging.logError("DEBUG: Motor returned after " + counter + " polls.");
        ErrorLogging.logError("DEBUG: --------------------------------------");
        return counter;
    }

    /**
     * Used to programmatically determine the motor's max speed.
     */
    private static void calibrate()
    {
        ErrorLogging.logError("Determining distance to limit switches...");
        findDistance();
        ErrorLogging.logError("Resetting arm to set speed.");
        resetArm();
        ErrorLogging.logError("Calibrating...");
        FREQUENCY = calib(MIN_FREQUENCY, MAX_FREQUENCY, 10000);
        //ErrorLogging.logError("Fine calibrating...");
        //FREQUENCY = calib(FREQUENCY,(FREQUENCY+10000),1000);
        ErrorLogging.logError("Calibration complete!");
        ErrorLogging.logError("DEBUG: Speed set to " + (FREQUENCY - SPEED_BUFFER));
        setFrequency(FREQUENCY - SPEED_BUFFER);
    }

    /**
     * Used to programmatically find the distance between the upper and lower limit switches.
     */
    private static void findDistance()
    {
        resetArm();
        int downTravelCounter = 0;
        int upTravelCounter = 0;
        pwm.on(DUTY_CYCLE, MIN_FREQUENCY);
        motorDirection.low();
        motorEnable.on();
        for(downTravelCounter = 0; downTravelCounter < Integer.MAX_VALUE; downTravelCounter++)
        {
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(lowerLimit.isOn()) 
            {
                try{ Thread.sleep(1); } catch(Exception e){ErrorLogging.logError(e); }
                if(lowerLimit.isOn()) break;
            }
        }
        motorEnable.off();
        if(lowerLimit.isOff()) ErrorLogging.logError("DEBUG: False positive on findDistance down!");
        
        int downTravelDist = downTravelCounter * MIN_FREQUENCY;
        ErrorLogging.logError("DEBUG: Down travel distance found to be: " + downTravelDist);
        ErrorLogging.logError("DEBUG: Down travel count: " + downTravelCounter);

        motorDirection.high();
        motorEnable.on();
        for(upTravelCounter = 0; upTravelCounter < Integer.MAX_VALUE; upTravelCounter++)
        { 
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(upperLimit.isOn()) 
            {
                try{ Thread.sleep(1); } catch(Exception e){ErrorLogging.logError(e); }
                if(upperLimit.isOn()) break;
            }
        }
        motorEnable.off();
        if(upperLimit.isOff()) ErrorLogging.logError("DEBUG: False positive on findDistance up!");

        int upTravelDist = upTravelCounter * MIN_FREQUENCY;
        ErrorLogging.logError("DEBUG: Up travel distance found to be: " + upTravelDist);
        ErrorLogging.logError("DEBUG: Up travel count: " + downTravelCounter);

        if(Math.abs(upTravelCounter - downTravelCounter) > 3)
        {
            ErrorLogging.logError("DEBUG: Values differ too far to be error. Setting to lower value.");
        }
        int travelCounter = Math.min(upTravelCounter, downTravelCounter);
        TRAVEL_DIST = travelCounter * MIN_FREQUENCY;
    }


    /**
     * Find the max frequency to feed to the motor.
     *
     * @param start     Lowest frequency to check 
     * @param max       Highest frequency to check
     * @param iterate   How much to iterate by
     *
     * @return The largest safe value between start and max. 
     */
    private static int calib(int start, int max, int iterate)
    {
        //start -= iterate;
        for(int i = start; i < max; i+=iterate)
        {
            if(!setFrequency(i))
            {
                ErrorLogging.logError("DEBUG: Speed set unsuccessfully! returning " + MIN_FREQUENCY + "...");
                return MIN_FREQUENCY;
            }
            ErrorLogging.logError("DEBUG: Motor travelling down.");
            motorDirection.low();
            ErrorLogging.logError("DEBUG: Motor Frequency: " + FREQUENCY);
            ErrorLogging.logError("DEBUG: Motor calibrate on.");
            motorEnable.on();
            int TWO_SECONDS = 2 * TIME_CONVERSION;
            for(int j = 0; j < TWO_SECONDS; j++)
            {
                try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
                if(lowerLimit.isHigh()) 
                {
                    ErrorLogging.logError("DEBUG: Breaking loop early!");
                    break;
                }
            }
            motorEnable.off();
            ErrorLogging.logError("DEBUG: Motor calibrate off.");
            if(upperLimit.isHigh())
            {
                ErrorLogging.logError("DEBUG: Motor failed to move! Returning " + (i - iterate));
                return i-iterate;
            }
            else
            {

                ErrorLogging.logError("DEBUG: Motor moved at speed " + i + ". Checking for errors.");
                if(resetArm() < 10)
                {
                    ErrorLogging.logError("DEBUG: Motor failed to move! Returning " + (i - iterate));
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
    private static boolean setFrequency(int newFrequency)
    {
        boolean output;
        if(newFrequency < MIN_FREQUENCY || newFrequency > MAX_FREQUENCY) 
        {
            ErrorLogging.logError("DEBUG: Invalid MovementFacade.setFrequency() value, setting to minfrequency!");
            FREQUENCY = MIN_FREQUENCY;
            output = false;
        }
        else
        {
            FREQUENCY = newFrequency;
            output = true;
        }
        ErrorLogging.logError("DEBUG: Setting frequency to " + FREQUENCY);
        pwm.on(DUTY_CYCLE, FREQUENCY);
        return output;
    }

    /**
     * Internal function to send the fixture to a given limit switch.
     *
     * Detects if the limit switch is active before activating motor.
     *
     * @param moveUp    Whether to send the fixture up or down. (True = up, False = down)
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

        pwm.on(DUTY_CYCLE, FREQUENCY);
        int totalPollCount = (int)(TRAVEL_DIST / FREQUENCY);
        int highSpeedPolls = (int)(totalPollCount * SLOW_POLL_FACTOR);
        int notHighSpeedPolls = totalPollCount - highSpeedPolls;
        int medSpeedPolls = (int)(notHighSpeedPolls * SLOW_POLL_FACTOR);
        int lowSpeedPolls = notHighSpeedPolls - medSpeedPolls;

        ErrorLogging.logError("DEBUG: =============================");
        ErrorLogging.logError("DEBUG: Travel time: " + totalPollCount);
        ErrorLogging.logError("DEBUG: High speed poll count: " + highSpeedPolls);
        ErrorLogging.logError("DEBUG: Medium speed poll count: " + medSpeedPolls);
        ErrorLogging.logError("DEBUG: Low speed poll count: " + lowSpeedPolls);
        ErrorLogging.logError("DEBUG: =============================");
        motorEnable.on();
        for(int i = 0; i < highSpeedPolls; i++)
        {
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(limitSense.isOn())
            {
                motorEnable.off();
                ErrorLogging.logError("DEBUG: Motor moved too fast! Stopping motor early.");
                ErrorLogging.logError("DEBUG: Breaking high-speed loop and turning off motor!");
                ErrorLogging.logError("DEBUG: Iteration count: " + i);
                output = FinalState.FAILED;
                break;
            }
        }

        if(motorEnable.isOn())
        {
            output = FinalState.UNSAFE;
            pwm.on(DUTY_CYCLE, (FREQUENCY / SPEED_DOWN_FACTOR));
            for(int i = 0; i < medSpeedPolls; i++)
            {
                try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
                if(limitSense.isOn())
                {
                    motorEnable.off();
                    ErrorLogging.logError("DEBUG: Motor only partially slowed down! Stopping motor early.");
                    ErrorLogging.logError("DEBUG: Breaking medium-speed loop and turning off motor!");
                    ErrorLogging.logError("DEBUG: Iteration count: " + i);
                    break;
                }
            }
        }

        if(motorEnable.isOn())
        {
            output = FinalState.SAFE;
            pwm.on(DUTY_CYCLE, (FREQUENCY / SPEED_DOWN_FACTOR));
            for(int i = 0; i < lowSpeedPolls; i++)
            {
                try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
                if(limitSense.isOn())
                {
                    motorEnable.off();
                    ErrorLogging.logError("DEBUG: Motor slowed down completely, but hit limit switch early.");
                    ErrorLogging.logError("DEBUG: Breaking low-speed loop and turning off motor!");
                    ErrorLogging.logError("DEBUG: Iteration count: " + i);
                    break;
                }
            }
        }

        motorEnable.off();
        
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
