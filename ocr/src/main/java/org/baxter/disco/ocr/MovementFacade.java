package org.baxter.disco.ocr;

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
 * @author Blizzard Finnegan
 * @version 19 Jan. 2023
 */
public class MovementFacade
{
    //Externally Available Variables
    /**
     * Number of times to test the Device Under Test
     */
    public static int CYCLES = 10000;

    /**
     * PWM Frequency
     * TODO: Setter with bounds
     */
    private static int FREQUENCY = 60000;

    /**
     * PWM Duty Cycle
     */
    public static final int DUTY_CYCLE = 50;

    /**
     * Number of seconds to wait before timing out a fixture movement.
     */
    public static final int TIME_OUT = 20;

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
     * Output pin addres for piston control.
     */
    private static final int PISTON_ADDR = 15;

    /**
     * PWM pin address
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
     * Piston control pin.
     *
     * Status: High; Piston is extended.
     * Status: Low; Piston is retracted.
     */
    private static DigitalOutput pistonActivate;

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
        pwmBuilder("pwm","PWM Pin",PWM_PIN_ADDR);
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
            //TODO: Set PWM duty cycle
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
                                   //Start PWM signal on initialisation
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
                                   //Start PWM signal on initialisation
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
                                                              .name(name)
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
     * Internal function to send the fixture to one limit switch or another.
     *
     * @param moveUp    Whether to send the fixture up or down. (True = up, False = down)
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    private static boolean gotoLimit(boolean moveUp, int timeout)
    {
        boolean output = false;
        DigitalInput limitSense;
        if(moveUp)  
        {
            motorDirection.high();
            limitSense = upperLimit;
        }
        else        
        {
            motorDirection.low();
            limitSense = lowerLimit;
        }
        for(int i = 0; i < (timeout * 20);i++)
        {
            try{ Thread.sleep(50); } catch(Exception e) {ErrorLogging.logError(e);};
            output = limitSense.isHigh();
            if(output) return output;
        }
        return output;
    }

    /**
     * Send the fixture to the lower limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public static boolean goDown(int timeout) { return gotoLimit(false, timeout); }

    /**
     * Send the fixture to the upper limit switch.
     *
     * @param timeout   How long (in seconds) to wait before timing out.
     * @return true if movement was successful; otherwise false
     */
    public static boolean goUp(int timeout) { return gotoLimit(true, timeout); }

    /**
     * Send the fixture to the lower limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public static boolean goDown() { return goDown(TIME_OUT); }

    /**
     * Send the fixture to the upper limit switch.
     * Timeout defaults to {@link #TIME_OUT}.
     *
     * @return true if movement was successful; otherwise false
     */
    public static boolean goUp() { return goUp(TIME_OUT); }

    /**
     * Extends the piston for 1 second, pushing the button on the DUT.
     */
    public static void pressButton()
    {
        pistonActivate.on();
        try{ Thread.sleep(1000); } catch(Exception e) {ErrorLogging.logError(e);};
        pistonActivate.off();
    }

    /**
     * Closes connections to all GPIO pins.
     * Also closes logs.
     */
    public static void closeGPIO()
    {
        pi4j.shutdown();
        ErrorLogging.closeLogs();
    }

    /**
     * Tests all available motions of the fixture. 
     *
     * @return True if all movements worked properly; otherwise False
     */
    public static boolean testMotions()
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

    //TODO: Multithreading, to allow for RunSwitch Interrupts.
//    protected class RunSwitchInterrupt implements Runnable
//    {
//        public void run()
//        {
//        }
//    }

    /**
     * Main function.
     *
     * Run this class by itself to test the GPIO functionality.
     */
    public static void main(String[] args)
    {
        testMotions();
        closeGPIO();
    }

}
