# Java OCR

This is a personal/professional project, which makes use of JavaCV, OpenCV, Tesseract, Pi4J, JavaFX, and Apache Commons to perform automated optical character recognition.

## Currently Working Features

- CLI Interface (All instances have user input checking)
	- [x] Main Menu
	- Camera config menu
		- [x] Takes in inputs
		- [x] Sets config values
		- [x] Saves config values 
		- [x] Shows camera preview
	- [x] GPIO test interactions
	- Test suite
		- [x] OpenCV image capture
		- [x] OpenCV image processing
		- [x] Tesseract OCR processing 
		- [x] Data storage in defined XLSX file*
	- [x] modify number of iterations for test suite
- [x] JavaFX GUI (Designed, implemented)

## Known Bugs

- Closing the program throws a fatal error. This is due to the current implementation of monitoring of the Run switch, and as of now, to my knowledge, cannot be resolved.
	- As of CLI build 1.2.0, the external run switch bricks the system.
- Currently, all images are parsed on a per-iteration level. This means that we cannot guarantee the output columns are correct. As a temporary solution, image location, serial number, and parsed reading are placed in groups. Intention is to ultimately have them be column-wise written.
- As of CLI build 1.3.0, the first reading row must be discarded, due to one side of the fixture producing a consistent failed reading. This has yet to be debugged, or fully understood.

## Dependencies
To install this project, and use it fully, you must have the following:
- a Raspberry Pi 4 or 400 (other Pis may work properly, but has not been tested)
	- OpenJDK11
- A separate development computer (preferrably x86-64 based)
	- A Java-compatible IDE
	- Maven
	- OpenJDK11

There are several required dependencies for this project. Maven handles these dependencies for you. Note that your first build of the project will take some time, as all dependencies will need to be downloaded before compiling.

## Installation

The project is then built from source, and the output final binary (located in `target/discoTesting.jar`) is copied to the Raspberry Pi for use.

## Building from source

Clone the repository onto your computer, then run the following to compile the project into a runnable JAR file:

```
mvn clean package
```

## Documentation

This project was built with Javadoc in mind, as it is a good way to explore a project in an interactive manner. To generate Javadocs, run the following:

```
mvn site
```

The documentation site can then be found in `target/site/index.html`.
