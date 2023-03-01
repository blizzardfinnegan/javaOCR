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
     * Internal PWM Frequency
     */
    private static int FREQUENCY;

    /**
     * Conversion factor for freq to FREQUENCY
     */
    private static final int FREQUENCY_UNITS = 1000;

    /**
     * Max allowable frequency by current fixture design.
     */
    private static final int MAX_FREQUENCY = 175000;

    /**
     * Minimum allowed frequency; also used for reset travels.
     */
    private static final int MIN_FREQUENCY = 25000;

    //Externally Available Variables
    /**
     * Human-readable frequency
     */
    private static double freq;

    /**
     * PWM Duty Cycle
     */
    private static final int DUTY_CYCLE = 50;

    /**
     * Number of seconds to wait before timing out a fixture movement.
     */
    private static double TIME_OUT;

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
     * How many times to poll the GPIO during a movement call.
     * The 1000/POLL_WAIT in this definition is converting poll-times to polls-per-second.
     */
    private static double POLL_COUNT = TIME_OUT * (1000 / POLL_WAIT);

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
        FREQUENCY = (int)ConfigFacade.getFixtureValue(ConfigFacade.FixtureValues.FREQUENCY);
        freq = FREQUENCY / FREQUENCY_UNITS;
        TIME_OUT = (int)ConfigFacade.getFixtureValue(ConfigFacade.FixtureValues.TIMEOUT);

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
        pwm.on(DUTY_CYCLE, FREQUENCY);

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
                                   .frequency(FREQUENCY)
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
                                   .frequency(FREQUENCY)
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
     * Getter for the fixture's PWM duty cycle.
     *
     * @return The current DutyCycle.
     */
    public static int getDutyCycle() { return DUTY_CYCLE; }

    /**
     * Setter for the fixture's time to give up on a movement.
     *
     * @param newTimeout  The new timeout (in seconds) to be set by the user.
     *
     * @return True if the value was set successfully; otherwise false.
     */
    public static boolean setTimeout(double newTimeout)
    {
        boolean output = false;
        if(newTimeout < 0)
        {
            ErrorLogging.logError("Movement error!!! - Invalid timeout input.");
        }
        else
        {
            TIME_OUT = newTimeout;
            ConfigFacade.setFixtureValue(ConfigFacade.FixtureValues.TIMEOUT, newTimeout);
            POLL_COUNT = TIME_OUT * ( 1000 / POLL_WAIT );
            output = true;
        }
        return output;
    }

    /**
     * Getter for the fixture's time to give up on a movement.
     *
     * @return The current timeout.
     */
    public static double getTimeout() { return TIME_OUT; }

    /**
     * Setter for the fixture's PWM frequency.
     *
     * @param newFrequency  The new frequency to be set by the user.
     *
     * @return True if the value was set successfully; otherwise false.
     */
    public static boolean setFrequency(double newFrequency)
    {
        boolean output = false;
        if(newFrequency < 0)
        {
            ErrorLogging.logError("Movement error! - Invalid frequency input.");
        }
        else if(newFrequency > MAX_FREQUENCY)
        {
            ErrorLogging.logError("Movement warning!!! - Value above maximum allowed.");
        }
        else
        {
            freq = newFrequency;
            ConfigFacade.setFixtureValue(ConfigFacade.FixtureValues.FREQUENCY, newFrequency);
            FREQUENCY = (int)(freq * FREQUENCY_UNITS);
            pwm.on(DUTY_CYCLE, FREQUENCY);
            output = true;
        }
        return output;
    }

    /**
     * Getter for the fixture's PWM frequency, in hertz.
     *
     * @return The current PWM frequency.
     */
    public static int getFrequency() { return FREQUENCY; }

    /**
     * Getter for the fixture's PWM frequency, in KHz.
     *
     * @return The current PWM frequency.
     */
    public static double getUserFrequency() { return freq; }

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
    private static FinalState gotoLimit(boolean moveUp, double timeout)
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

        double mostlyThere = (POLL_COUNT * 1) / 2;
        int slowerSpeed = FREQUENCY / 4;

        motorEnable.on();
        for(int i = 0; i < (POLL_COUNT);i++)
        {
            try{ Thread.sleep(POLL_WAIT); } 
            catch(Exception e) {ErrorLogging.logError(e);};

            if(limitSense.isHigh()) 
            {
                output = ( (i >= mostlyThere) ? FinalState.SAFE : FinalState.UNSAFE);
                break;
            }
            else if(i >= mostlyThere)
            { 
                pwm.on(DUTY_CYCLE, slowerSpeed); 
                continue; 
            }
        }
        
        if(output == FinalState.FAILED) 
            ErrorLogging.logError("FIXTURE MOVEMENT ERROR! - Motor movement timed out!");
        motorEnable.off();
        pwm.on(DUTY_CYCLE, FREQUENCY);
        return output;
    }

    public static void reset()
    {
        pwm.on(DUTY_CYCLE, MIN_FREQUENCY);
        goUp(Double.POSITIVE_INFINITY);
        pwm.on(DUTY_CYCLE, FREQUENCY);
    }

    /**
     * Send the fixture to the lower limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goDown(double timeout) { return gotoLimit(false, timeout); }

    /**
     * Send the fixture to the upper limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goUp(double timeout) { return gotoLimit(true, timeout); }

    /**
     * Send the fixture to the lower limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goDown() { return goDown(TIME_OUT); }

    /**
     * Send the fixture to the upper limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public static FinalState goUp() { return goUp(TIME_OUT); }

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
        reset();
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
