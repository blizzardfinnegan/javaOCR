# Things to do

## High-priority fixes

### CLI

- [ x ] complete implementation (see comments in file)
	- [ x ] primary functionality
	- [ ] connect to other classes
		- partially complete
	- [ x ] Javadoc documentation
- [ ] Debug so CLI is usable (HIGHEST PRIORITY)
- [ ] Find way to kill CanvasFrame protection Thread on exit

### DataSaving

- [ ] complete implementation
	- [ x ] need to store location of excel file
	- [ x ] need to have a function to write values to an excel file, parsing along the way for anomalies (Currently not backwards compatible)
		- requires looking back at `logging_tools.py`
	- [ x ] Javadoc documentation
	- [ ] Test if it works

### ConfigFacade

- [ x ] refactor static block to load defaults
- [ ] refactor `saveCurrentConfig(String filename)`
- [ x ] set default for `imageSaveLocation`
- [ x ] properly fill the list of active cameras
	- Is this still a necessary object? Appears to be similar to the `Set<String>` of `configMap`; consider replacing with a wrapper around `configMap.keySet()`.	This is not necessary, and has been removed.

### ErrorLogging

- [ x ] refactor static block create a new file at runtime

### MovementFacade

- [ ] Implement multithreading for physical Run switch
	- requires much documentation reading

### OpenCVFacade

- [ ] Overload takeBurst with a default framecount from config
- [ x ] Overload crop with default values from config
- [ ] completeProcess should have more robust file output checking

### Gui

- [ ] rebuild menus with current feature set
- [ ] Complete implementation; waiting on:
	- [ ] read documentation
	- [ ] implement Cli successfully

Notes: JavaFXML is the View. Model should be wrapper around all classes, similar to Cli. Controller should connect JavaFXML interface to Model.

## Low-priority improvements

### All

- [ ] denote overrided functions in first sentence of function
- [ ] reduce Javadoc linking to one link per reference per class
- [ ] Use generated Javadocs to improve Javadocs (perpetual)

### OpenCVFacade

- [ x ] convert all uses of Frame to Mat

### TesseractFacade

- [ ] parse text-based input?
	- requires further communication with Pete to determine if necessary.

### ConfigProperties

- [ x ] More complete documentation

