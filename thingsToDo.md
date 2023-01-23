# Things to do

## High-priority fixes

### CLI

- [ x ] complete implementation (see comments in file)

### DataSaving

- [ ] complete implementation
	- [ ] need to store location of excel file
	- [ ] need to have a function to write values to an excel file, parsing along the way for anomalies
		- requires looking back at `logging_tools.py`

### ConfigFacade

- [ ] refactor static block to load defaults
- [ ] refactor `saveCurrentConfig(String filename)`
- [ ] set default for `imageSaveLocation`
- [ ] properly fill the list of active cameras
	- Is this still a necessary object? Appears to be similar to the `Set<String>` of `configMap`; consider replacing with a wrapper around `configMap.keySet()`

### ErrorLogging

- [ ] refactor static block create a new file at runtime

### MovementFacade

- [ ] refactor with frequency as private
	- [ ] getter
	- [ ] setter, with bounds
		- [ ] no negatives
		- [ ] some upper bound (probably the RPi's upper bound of HW PWM)
- [ ] actually set PWM duty cycle in PWM creator
- [ ] Implement multithreading for physical Run switch
	- requires much documentation reading

### OpenCVFacade

- [ ] Overload takeBurst with a default framecount from config
- [ ] Overload crop with default values from config
- [ ] completeProcess should have more robust file output checking

### Gui
- [ ] Complete implementation; waiting on:
	- [ ] read documentation
	- [ ] implement Cli successfully

## Low-priority improvements

### All

- [ ] change Javadoc to be parameter type, rather than parameter name
- [ ] denote overrided functions in first sentence of function
- [ ] reduce linking to one link per reference per class
- [ ] Use generated Javadocs to improve Javadocs

### OpenCVFacade

- [ ] convert all uses of Frame to Mat
	- not sure if it does much; will have to finish building to find out.

### TesseractFacade

- [ ] parse text-based input?
	- requires further communication with Pete to determine if necessary.

### ConfigProperties

- [ ] More complete documentation

### Gui

- [ ] rebuild menus with current feature set

