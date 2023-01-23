# Things to do

## High-priority fixes

### CLI

- [ ] complete implementation (see comments in file)
	- [ x ] primary functionality
	- [ ] connect to other classes
		- partially complete
	- [ x ] Javadoc documentation

### DataSaving

- [ ] complete implementation
	- [ x ] need to store location of excel file
	- [ x ] need to have a function to write values to an excel file, parsing along the way for anomalies (Currently not backwards compatible)
		- requires looking back at `logging_tools.py`
	- [ x ] Javadoc documentation

### ConfigFacade

- [ ] refactor static block to load defaults
- [ ] refactor `saveCurrentConfig(String filename)`
- [ x ] set default for `imageSaveLocation`
- [ ] properly fill the list of active cameras
	- Is this still a necessary object? Appears to be similar to the `Set<String>` of `configMap`; consider replacing with a wrapper around `configMap.keySet()`

### ErrorLogging

- [ ] refactor static block create a new file at runtime

### MovementFacade

- [ x ] refactor with frequency as private
	- [ x ] getter
	- [ x ] setter, with bounds
		- [ x ] no negatives
		- [ ] ~~some upper bound (probably the RPi's upper bound of HW PWM)~~ According to Ed, this is not necessary.
- [ x ] actually set PWM duty cycle in PWM creator
- [ ] Implement multithreading for physical Run switch
	- requires much documentation reading

### OpenCVFacade

- [ ] Overload takeBurst with a default framecount from config
- [ x ] Overload crop with default values from config
- [ ] completeProcess should have more robust file output checking

### Gui
- [ ] Complete implementation; waiting on:
	- [ ] read documentation
	- [ x ] implement Cli successfully

## Low-priority improvements

### All

- [ x ] denote overrided functions in first sentence of function
- [ ] reduce linking to one link per reference per class
- [ ] Use generated Javadocs to improve Javadocs (perpetual)

### OpenCVFacade

- [ x ] convert all uses of Frame to Mat

### TesseractFacade

- [ ] parse text-based input?
	- requires further communication with Pete to determine if necessary.

### ConfigProperties

- [ x ] More complete documentation

### Gui

- [ ] rebuild menus with current feature set

