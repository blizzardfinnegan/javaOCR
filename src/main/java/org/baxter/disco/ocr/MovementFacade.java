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

/**
 * Facade for all movement of the fixture.
 *
 * Uses Pi4J to communicate with GPIO pins.
 * Currently missing Run switch compatibility.
 *
 * @author Blizzard Finnegan
 * @version 3.0.1, 10 Mar. 2023
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
     * Fraction of the total travel time, so the arm won't push through the limit switch.
     */
    private static final double SLOW_POLL_FACTOR = 0.95;

    /**
     * Amount of distance to travel.
     * Measured in... seemingly arbitrary units? Not sure on the math here.
     * Set in {@link #findDistance()}
     */
    private static double TRAVEL_DIST;

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
     * {@link Pi4J} API interaction object.
     */
    private static Context pi4j;

    static
    {
        ErrorLogging.logError("DEBUG: Starting lock thread...");
        runSwitchThread = new Thread(() -> 
                {
                    boolean unlock = false;
                    while(!exit)
                    {
                        if(runSwitch.isOn())
                        {
                            ErrorLogging.logError("DEBUG: Run switch turned off!");
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

        pi4j = Pi4J.newAutoContext();

        upperLimit = inputBuilder("upperLimit", "Upper Limit Switch", UPPER_LIMIT_ADDR);
        lowerLimit = inputBuilder("lowerLimit", "Lower Limit Switch", LOWER_LIMIT_ADDR);
        runSwitch  = inputBuilder("runSwitch" , "Run Switch"        , RUN_SWITCH_ADDR);

        motorEnable    = outputBuilder("motorEnable"   , "Motor Enable"   , MOTOR_ENABLE_ADDR);
        motorDirection = outputBuilder("motorDirection", "Motor Direction", MOTOR_DIRECTION_ADDR);
        pistonActivate = outputBuilder("piston"        , "Piston Activate", PISTON_ADDR);

        findDistance();
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
     * Used to programmatically find the distance between the upper and lower limit switches.
     */
    private static void findDistance()
    {
        resetArm();
        int downTravelCounter = 0;
        int upTravelCounter = 0;
        //pwm.on(DUTY_CYCLE, MIN_FREQUENCY);
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

        ErrorLogging.logError("DEBUG: Up travel count: " + downTravelCounter);

        int travelCounter = Math.min(upTravelCounter, downTravelCounter);
        TRAVEL_DIST = travelCounter;
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
            ErrorLogging.logError("DEBUG: Sending fixture up...");
        }
        else        
        {
            motorDirection.low();
            limitSense = lowerLimit;
            ErrorLogging.logError("DEBUG: Sending fixture down...");
        }

        if(limitSense.isHigh()) return FinalState.SAFE;

        int totalPollCount = (int)(TRAVEL_DIST);
        int highSpeedPolls = (int)(totalPollCount * SLOW_POLL_FACTOR);
        ErrorLogging.logError("DEBUG: =============================");
        ErrorLogging.logError("DEBUG: Travel time: " + totalPollCount);
        ErrorLogging.logError("DEBUG: High speed poll count: " + highSpeedPolls);
        ErrorLogging.logError("DEBUG: =============================");
        motorEnable.on();
        for(int i = 0; i < highSpeedPolls; i++)
        {
            try{ Thread.sleep(POLL_WAIT); } catch(Exception e){ ErrorLogging.logError(e); }
            if(limitSense.isOn())
            {
                motorEnable.off();
                break;
            }
        }
        motorEnable.off();

        output = (limitSense.isOn() ? FinalState.UNSAFE : FinalState.SAFE);

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
