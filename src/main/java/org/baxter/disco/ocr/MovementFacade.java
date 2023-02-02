package org.baxter.disco.ocr;

import java.util.concurrent.locks.Lock;

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
 * @version 2.0.0, 01 Feb. 2023
 */
public class MovementFacade
{
    /**
     * Constructor for MovementFacade.
     *
     * @param LOCK  A Lock object, used for interactions with
     *              the physical lock switch on the fixture.
     */
    public MovementFacade(Lock LOCK)
    {
        //ErrorLogging.logError("DEBUG: Starting lock thread...");
        runSwitchThread = new Thread(() -> 
                {
                    boolean unlock = false;
                    while(true)
                    {
                        if(runSwitch.isOn())
                        {
                            //ErrorLogging.logError("Run switch turned off!");
                            while(!LOCK.tryLock())
                            { unlock = true; }
                        }
                        else
                        {
                            //ErrorLogging.logError("Run switch on!");
                            if(unlock) 
                            { LOCK.unlock(); unlock = false; }
                        }
                        //try{ Thread.sleep(100); } catch(Exception e) { ErrorLogging.logError(e); }
                    }
                }, "Run switch monitor.");
        runSwitchThread.start();
        //ErrorLogging.logError("DEBUG: Lock thread started!");
    }

    private static Thread runSwitchThread;

    //Externally Available Variables
    /**
     * PWM Frequency
     */
    private static int FREQUENCY = 60000;

    /**
     * PWM Duty Cycle
     */
    private static int DUTY_CYCLE = 50;

    /**
     * Number of seconds to wait before timing out a fixture movement.
     */
    private static int TIME_OUT = 10;

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
     * Setter for the fixture's PWM duty cycle.
     *
     * @param newDutyCycle  The new duty cycle to be set by the user.
     *
     * @return True if the value was set successfully; otherwise false.
     */
    public boolean setDutyCycle(int newDutyCycle)
    {
        boolean output = false;
        if(newDutyCycle < 0)
        {
            ErrorLogging.logError("Movement error!!! - Invalid DutyCycle input.");
        }
        else
        {
            DUTY_CYCLE = newDutyCycle;
            pwm.on(DUTY_CYCLE, FREQUENCY);
            output = true;
        }
        return output;
    }

    /**
     * Getter for the fixture's PWM duty cycle.
     *
     * @return The current DutyCycle.
     */
    public int getDutyCycle() { return DUTY_CYCLE; }

    /**
     * Setter for the fixture's time to give up on a movement.
     *
     * @param newTimeout  The new timeout (in seconds) to be set by the user.
     *
     * @return True if the value was set successfully; otherwise false.
     */
    public boolean setTimeout(int newTimeout)
    {
        boolean output = false;
        if(newTimeout < 0)
        {
            ErrorLogging.logError("Movement error!!! - Invalid timeout input.");
        }
        else
        {
            TIME_OUT = newTimeout;
            output = true;
        }
        return output;
    }

    /**
     * Getter for the fixture's time to give up on a movement.
     *
     * @return The current timeout.
     */
    public int getTimeout() { return TIME_OUT; }

    /**
     * Setter for the fixture's PWM frequency.
     *
     * @param newFrequency  The new frequency to be set by the user.
     *
     * @return True if the value was set successfully; otherwise false.
     */
    public boolean setFrequency(int newFrequency)
    {
        boolean output = false;
        if(newFrequency < 0)
        {
            ErrorLogging.logError("Movement error!!! - Invalid frequency input.");
        }
        else
        {
            FREQUENCY = newFrequency;
            pwm.on(DUTY_CYCLE, FREQUENCY);
            output = true;
        }
        return output;
    }

    /**
     * Getter for the fixture's PWM frequency.
     *
     * @return The current PWM frequency.
     */
    public int getFrequency() { return FREQUENCY; }

    /**
     * Internal function to send the fixture to one limit switch or another.
     *
     * @param moveUp    Whether to send the fixture up or down. (True = up, False = down)
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    private boolean gotoLimit(boolean moveUp, int timeout)
    {
        boolean output = false;
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

        motorEnable.on();
        for(int i = 0; i < (timeout * 20);i++)
        {
            try{ Thread.sleep(50); } catch(Exception e) {ErrorLogging.logError(e);};
            output = limitSense.isHigh();
            if(output) break;
        }
        if(output == false) 
            ErrorLogging.logError("FIXTURE MOVEMENT ERROR! - Motor movement timed out!");
        motorEnable.off();
        return output;
    }

    /**
     * Send the fixture to the lower limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public boolean goDown(int timeout) { return gotoLimit(false, timeout); }

    /**
     * Send the fixture to the upper limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public boolean goUp(int timeout) { return gotoLimit(true, timeout); }

    /**
     * Send the fixture to the lower limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public boolean goDown() { return goDown(TIME_OUT); }

    /**
     * Send the fixture to the upper limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public boolean goUp() { return goUp(TIME_OUT); }

    /**
     * Extends the piston for 1 second, pushing the button on the DUT.
     */
    public void pressButton()
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
    public void closeGPIO()
    {
        goUp();
        if(runSwitchThread.isAlive())
            runSwitchThread.interrupt();
        pi4j.shutdown();
    }

    /**
     * Tests all available motions of the fixture. 
     *
     * @return True if all movements worked properly; otherwise False
     */
    public boolean testMotions()
    {
        boolean output = goUp();
        if(!output) return output;
        pressButton();
        output = goDown();
        if(!output) return output;
        pressButton();
        output = goUp();
        return output;
    }

    public void iterationMovement(boolean prime)
    {
        goUp();
        if(prime) pressButton();
        goDown();
        pressButton();
    }

    public void main(String[] args)
    {
        testMotions();
        closeGPIO();
    }
}
