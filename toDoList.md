# Things to do

## High-priority fixes

## Cli(?)

- [ ] enable camera toggling
	- may require more classes to be modified

### MovementFacade

- [x] Implement multithreading for physical Run switch\*
	- Currently questionably implemented, consider looking into more

### OpenCVFacade

- [x] Overload takeBurst with a default framecount from config
- [x] Overload crop with default values from config
- [ ] completeProcess should have more robust file output checking

### Gui

- [x] rebuild menus with current feature set
- [x] Complete implementation
	- currently questionably implemented; debugging...

## Low-priority improvements

### All

- [ ] denote overrided functions in first sentence of function
- [ ] reduce Javadoc linking to one link per reference per class
- [ ] Use generated Javadocs to improve Javadocs (perpetual)

### Cli

- [ ] Find way to kill CanvasFrame protection Thread on exit

### TesseractFacade

- [ ] parse text-based input?
	- requires further communication with Pete to determine if necessary.
	- requires retraining Tesseract

