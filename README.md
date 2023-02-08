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
		- [x] Data storage in defined XLSX file
	- [x] modify number of iterations for test suite
- [ ] JavaFX GUI (see `gui` branch)

## Dependencies

To install this project, you must have the following:
- a Raspberry Pi 4 or 400 (other Pis may work properly, but has not been tested), with the following installed:
	- OpenJDK11

To further develop this software, or to compile it from source, the following is also recommended:
- A separate development computer (preferrably x86-64 based), with the following installed:
	- A Java-compatible IDE
	- Maven
	- OpenJDK11

OpenJDK11 is explicitly required, as it is the only currently available Java development platform compatible with [Pi4J](https://pi4j.com/getting-started/). According to the [documentation for Pi4J](https://pi4j.com/getting-started/developing-on-a-remote-pc/), development on a Raspberry Pi is possible, but given this project's build time (as of 4.0.0-rc3, 2-5 minutes on a Baxter-distributed device, before documentation generation), it is recommended to build on x86-64, and copy to the compiled JAR to the Pi. As such, this repository has been designed with this development model in mind. If you are intending on compiling on a Pi, please see the above-linked documentation to see what should be modified in your local `pom.xml` file.

There are several secondary dependencies that are required for this project. However, Maven handles these dependencies for you. Note that because of this, your first build of the project will take some time, and will require internet access, as all dependencies will need to be downloaded before compiling.

## First-Time Setup

If you are working with a newly-installed Raspberry Pi, there are several steps you will need to also take before running this program. This will require some use of the terminal.

1. You will need to kill and disable the `pigpio` daemon. This is done by running the following commands:
```
sudo killall pigpiod
sudo systemctl disable pigpiod
```
The first command stops all currently running `pigpio` daemon processes. The second command disables the daemon, so that it will not start again if you reboot.

2. You will need to create a new `udev` rule. This creates a symlink for a given camera, plugged into a specific USB port, and allows the Java code to consistently communicate with the camera. An example `udev` rule is given in this repo (`83-webcam.rules`), but will need to be modified to your specific device. 
	1. Copy the example `udev` rule file to your Raspberry Pi, and put it in `/etc/udev/rules.d/` (if this directory does not exist, create it). 
	2. Open the copied file in the text editor of your choice, and open a terminal window as well. 
	3. Run the following command in your terminal window. 

	```
	sudo udevadm monitor -p | grep ID_PATH=
	```  

	This will show all `udev` activity as it happens.
	
	4. Unplug *ONE* camera, and plug it back in to the same port. This will generate several lines of text in your terminal window. 
	5. Copy one of the lines, starting with `platform`, and, *crucially*, ending `.0`.
	6. Paste this into your `udev` rule file, replacing the `fillerText` portion, but leaving the quotes. The first line of the file distributed in this repo contains a commented-out example line, with the correct syntax.
	7. Repeat steps 4-6 for all cameras in the fixture. If there are no new lines available, copy and paste the line into a new line to create a new rule, *ensuring to increment the number at the end of the line in the `SYMLINK` section*.
	8. Reboot the Raspberry Pi to load the new `udev` rule. 
	9. Open a terminal, and check that the new symlinks were created successfully. This can be done by running the below command. If the symlinks have not been created successfully, restart from step 3 until all symlinks are created properly.
```
ls /dev/video-*
```

## Installation

The project is then built from source, and the output final binary (located in `target/discoTesting.jar`) is copied to the Raspberry Pi for use.

This project requires use of `udev` rules to ensure that cameras are in the proper location, by default. This can be modified in this project (camera initialisation is done in the initialisation of `OpenCVFacade`).

## Usage

To use this program, it *must* be run on a Raspberry Pi, with available GPIO. 

1. Copy both the generated JAR file (the largest file in the `target` directory), the `tessdata` folder (Provided by Baxter, currently ommited from this repository due to licensing conflicts), and the `runScript.sh` to a flash drive. 
2. Eject the flash drive, then plug it into your Raspberry Pi (which should be connected to the fixture). 
3. Copy the files from step 1 to the desktop of the Pi, then either:
	- Easy: Double-click the `runScript.sh` file. This should show a warning, asking if you would like to Execute, Execute in Terminal, Open, or Cancel. Click "Execute in Terminal". 
	- Open a terminal, `cd` onto the desktop, then run the following command: (The `sudo` is necessary to access GPIO, it should not prompt for password.)

```
sudo java -jar [name of JAR file, including extension]
```
4. What will happen next depends on your current version:
	- Versions `4.0.0-rc1`,`4.0.0-rc2`, and `4.0.0` will create a terminal window. From there, use the numbers shown in the menu to control the fixture, and run tests as necessary.
	- An upcoming version will create a terminal window, which load things for a moment before also creating a GUI. This GUI can be used to control the fixture, and run tests as necessary. 
		- GUI development is currently limited to the `gui` branch.


### Potential Errors

If the terminal almost-immediately exits, or something seems wrong, check the log file (which is named with the current date and time; ex. `2023-02-07_09.15.22-log.txt`). This will show potential errors. 
- If the file contains the phrase `PI_INIT_FAILED`, the default GPIO daemon is currently active, and needs to be deactivated. To do so, run the following line in a terminal, then try to run the program again:
```
sudo killall pigpiod
```
- If the file contains a `CAMERA INIT ERROR`, this means that the associated camera was not picked up properly. This can be due to several reasons. Below are some debugging steps to take. Note that this will cause cascading errors, if attempting to import a camera's config from a pre-existing config file, so config-related errors can be ignored.
    1. Ensure that both cameras are plugged in properly.
    2. Unplug, and then plug back in, the erroring camera.
    3. Ensure that the `/dev/video-cam1` and `/dev/video-cam2` files are both created. If they are not, then you will need to update your `udev` rules, as in the Installation section.
    4. Reboot the Raspberry Pi. (This can be done by opening the terminal, and typing `reboot`, then hitting enter.) Camera drivers occasionally fail to load on boot, and will unload after a long time with no use. Rebooting generally solves this issue (although it may take multiple reboots.)

## Building from source

Before building this project, decide whether you want a TUI (Terminal User Interface), or a GUI (Graphical User Interface). GUI development has been moved to its own separate branch, for ease of project management.
- If you wish to build the TUI, ensure the `uitype` field in your `pom.xml` is `Cli`. 
- If you wish to build the GUI, ensure you are in the `gui` branch.

For your first time compiling the project, clone the repository onto your computer, then run the following in a terminal with Maven to compile the project into a runnable JAR file:

```
mvn clean package
```

Maven can also be interacted with in a GUI environment in Visual Studio Code, but at time of writing, this process is unknown to me.

This will create a new `target` folder, download all dependencies, and compile the code into a final JAR file. Subsequent project builds can be alled using either the above command (which will delete the previous `target` folder before recreating it, copying the required libraries, and compiling the code), or to save time, the following can also be run instead:

```
mvn package
```

As the next section describes, you can also build documentation at the same time, by running the following in your terminal:

```
mvn package; mvn site
```

or

```
mvn clean package; mvn site
```

## Documentation

This project was built with Javadoc in mind, as it is a good way to explore a project in an interactive manner. To generate Javadocs, run the following:

```
mvn site
```

The documentation site can then be found in `target/site/index.html`.

Note that because this documentation is generated in the same folder as the final project file, running `mvn clean package` will delete the documentation in its current state. As such, it is recommended to run a new documentation generation call on every clean build, like so:

```
mvn clean package; mvn site
```
