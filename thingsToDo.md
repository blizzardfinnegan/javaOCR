# Things to do

## High-priority fixes

### CLI

- [ ] complete implementation (see comments in file)

### DataSaving

- [ ] complete implementation:
	- [ ] need to store location of excel file
	- [ ] need to have a function to write values to an excel file, parsing along the way for anomalies
		- [ ] requires looking back at logging_tools.py

### ConfigFacade

- [ ] refactor static block to load defaults
- [ ] refactor `saveCurrentConfig(String filename)`

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
	- [ ] requires much documentation reading

### OpenCVFacade

- [ ] Overload takeBurst with a default framecount from config
- [ ] Overload crop with default values from config
- [ ] completeProcess should have more robust file output checking

## Low-priority improvements

### All

- [ ] change Javadoc to be parameter type, rather than parameter name

### ConfigProperties

- [ ] More complete documentation

### Gui

- [ ] rebuild menus with current feature set
- [ ] Complete implementation; waiting on:
	- [ ] read documentation
	- [ ] implement Cli successfully
